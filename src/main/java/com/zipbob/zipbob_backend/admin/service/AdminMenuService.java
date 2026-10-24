package com.zipbob.zipbob_backend.admin.service;

import com.zipbob.zipbob_backend.admin.dto.MenuCreateRequestDto;
import com.zipbob.zipbob_backend.entity.Menu;
import com.zipbob.zipbob_backend.entity.MenuFile;
import com.zipbob.zipbob_backend.repository.CodeRepository;
import com.zipbob.zipbob_backend.repository.MenuFileRepository;
import com.zipbob.zipbob_backend.repository.MenuRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminMenuService {

    private static final long MAX_UPLOAD_SIZE = 10L * 1024 * 1024;
    private static final long MAX_PIXELS = 12_000_000L;
    private static final int MAX_DESCRIPTION_LENGTH = 100;

    private final MenuRepository menuRepository;
    private final MenuFileRepository menuFileRepository;
    private final CodeRepository codeRepository;
    private final Path imageDirectory;

    public AdminMenuService(
            MenuRepository menuRepository,
            MenuFileRepository menuFileRepository,
            CodeRepository codeRepository,
            @Value("${app.menu-image-dir:./images/menus}")
            String imageDirectory
    ) {
        this.menuRepository = menuRepository;
        this.menuFileRepository = menuFileRepository;
        this.codeRepository = codeRepository;

        this.imageDirectory = Path.of(imageDirectory)
                .toAbsolutePath()
                .normalize();
    }

    // 선택 항목: 코드 ID와 표시 이름
    public record Option(
            String codeId,
            String codeName
    ) {
    }

    // 등록 팝업에서 사용할 선택 목록
    public record FormOptions(
            List<Option> categories,
            List<Option> difficulties,
            List<Option> tags
    ) {
    }

    // 카테고리·난이도·음식 특징 조회
    @Transactional(readOnly = true)
    public FormOptions getFormOptions() {
        List<Option> categories = codeRepository
                .findByCodeParentAndUseYn("category_code", "Y")
                .stream()
                .map(code -> new Option(
                        code.getCodeId(),
                        code.getCodeName()
                ))
                .toList();

        List<Option> difficulties = menuRepository
                .findDifficultyOptions()
                .stream()
                .map(code -> new Option(
                        code.getCodeId(),
                        code.getCodeName()
                ))
                .toList();

        List<Option> tags = codeRepository
                .findByCodeParentAndUseYn("tag_code", "Y")
                .stream()
                .map(code -> new Option(
                        code.getCodeId(),
                        code.getCodeName()
                ))
                .toList();

        return new FormOptions(categories, difficulties, tags);
    }

    // 신규 메뉴 등록
    @Transactional
    public Integer create(
            MenuCreateRequestDto request,
            String adminId
    ) {
        // 카테고리·난이도 유효성 검사
        validateCodes(request);

        // 음식 특징 검사 및 description 생성
        String description = createDescription(request.getTagIds());

        if (request.getCookTime() == 0
                && request.getCookMinute() == 0) {
            throw new IllegalArgumentException(
                    "조리 시간은 1분 이상 입력해주세요."
            );
        }

        // 이미지 검사 및 실제 PNG 형식으로 변환
        byte[] pngBytes = convertToPng(request.getImage());

        // 음식 특징 문자열을 description에 저장
        Menu menu = Menu.create(
                request.getMenuName(),
                request.getCategory(),
                description,
                request.getServings(),
                request.getCookTime(),
                request.getCookMinute(),
                request.getIngredients(),
                request.getSteps(),
                request.getDifficultyLvl(),
                adminId
        );

        // 메뉴 INSERT → DB에서 발급한 menu_id 확보
        menuRepository.saveAndFlush(menu);

        String saveName = "menus_" + menu.getMenuId() + ".png";
        String filePath = "/images/menus/" + saveName;
        Path target = imageDirectory.resolve(saveName);

        // 실제 이미지 파일 저장
        saveImage(target, pngBytes);

        // file_mng에 이미지 정보 저장
        MenuFile menuFile = MenuFile.create(
                menu,
                filePath,
                pngBytes.length,
                saveName,
                originalFileName(request.getImage()),
                adminId
        );

        menuFileRepository.saveAndFlush(menuFile);

        return menu.getMenuId();
    }

    // 카테고리·난이도 검사
    private void validateCodes(MenuCreateRequestDto request) {
        FormOptions options = getFormOptions();

        boolean validCategory = options.categories().stream()
                .anyMatch(option ->
                        option.codeId().equals(request.getCategory())
                );

        if (!validCategory) {
            throw new IllegalArgumentException(
                    "사용 가능한 카테고리를 선택해주세요."
            );
        }

        boolean validDifficulty = options.difficulties().stream()
                .anyMatch(option ->
                        option.codeId().equals(request.getDifficultyLvl())
                );

        if (!validDifficulty) {
            throw new IllegalArgumentException(
                    "사용 가능한 난이도를 선택해주세요."
            );
        }
    }

    // 음식 특징 코드 → DB에 저장된 특징 이름으로 변환
    private String createDescription(List<String> requestedTagIds) {
        // 최소 1개 필수, 선택 개수 상한 없음
        if (requestedTagIds == null || requestedTagIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "음식 특징을 최소 1개 선택해주세요."
            );
        }

        // 사용 중인 음식 특징만 조회
        Map<String, String> availableTags = codeRepository
                .findByCodeParentAndUseYn("tag_code", "Y")
                .stream()
                .collect(Collectors.toMap(
                        code -> code.getCodeId(),
                        code -> code.getCodeName()
                ));

        // 중복 코드는 제거하고 전달받은 순서는 유지
        Set<String> selectedTagIds = new LinkedHashSet<>();

        for (String tagId : requestedTagIds) {
            if (tagId == null || !availableTags.containsKey(tagId)) {
                throw new IllegalArgumentException(
                        "사용할 수 없는 음식 특징이 포함되어 있습니다."
                );
            }

            selectedTagIds.add(tagId);
        }

        // 예: mild, egg_tofu → "순한맛, 달걀·두부"
        String description = selectedTagIds.stream()
                .map(availableTags::get)
                .collect(Collectors.joining(", "));

        // DB의 description VARCHAR(100)에 맞춰 문자 수 검사
        int descriptionLength = description.codePointCount(
                0,
                description.length()
        );

        if (descriptionLength > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "선택한 음식 특징의 전체 이름은 "
                            + "쉼표와 공백을 포함해 100자 이하여야 합니다."
            );
        }

        return description;
    }

    // 이미지 검사 및 PNG 변환
    private byte[] convertToPng(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException(
                    "메뉴 이미지를 첨부해주세요."
            );
        }

        if (image.getSize() > MAX_UPLOAD_SIZE) {
            throw new IllegalArgumentException(
                    "이미지는 10MB 이하로 첨부해주세요."
            );
        }

        try (
                var input = image.getInputStream();
                ImageInputStream imageInput =
                        ImageIO.createImageInputStream(input)
        ) {
            if (imageInput == null) {
                throw new IllegalArgumentException(
                        "이미지 파일을 읽을 수 없습니다."
                );
            }

            Iterator<ImageReader> readers =
                    ImageIO.getImageReaders(imageInput);

            if (!readers.hasNext()) {
                throw new IllegalArgumentException(
                        "정상적인 JPG 또는 PNG 이미지를 첨부해주세요."
                );
            }

            ImageReader reader = readers.next();

            try {
                reader.setInput(imageInput);

                String format = reader.getFormatName();

                if (!"PNG".equalsIgnoreCase(format)
                        && !"JPEG".equalsIgnoreCase(format)) {
                    throw new IllegalArgumentException(
                            "JPG 또는 PNG 이미지만 등록할 수 있습니다."
                    );
                }

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);

                if (width <= 0
                        || height <= 0
                        || (long) width * height > MAX_PIXELS) {
                    throw new IllegalArgumentException(
                            "이미지는 1,200만 화소 이하로 첨부해주세요."
                    );
                }

                BufferedImage bufferedImage = reader.read(0);

                if (bufferedImage == null) {
                    throw new IllegalArgumentException(
                            "이미지 파일을 읽을 수 없습니다."
                    );
                }

                try (
                        ByteArrayOutputStream output =
                                new ByteArrayOutputStream()
                ) {
                    if (!ImageIO.write(bufferedImage, "png", output)) {
                        throw new IllegalStateException(
                                "PNG 변환을 지원하지 않습니다."
                        );
                    }

                    return output.toByteArray();

                } finally {
                    bufferedImage.flush();
                }

            } finally {
                reader.dispose();
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "이미지를 읽을 수 없습니다. 파일을 확인해주세요.",
                    e
            );
        }
    }

    // 이미지 저장 및 DB 롤백 시 파일 정리
    private void saveImage(Path target, byte[] bytes) {
        AtomicBoolean created = new AtomicBoolean(false);

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_ROLLED_BACK
                                || !created.get()) {
                            return;
                        }

                        try {
                            Files.deleteIfExists(target);
                        } catch (IOException e) {
                            log.error(
                                    "메뉴 등록 롤백 후 이미지 삭제 실패: {}",
                                    target,
                                    e
                            );
                        }
                    }
                }
        );

        try {
            Files.createDirectories(imageDirectory);

            // 기존 파일을 덮어쓰지 않고 새 파일 생성
            try (
                    OutputStream output = Files.newOutputStream(
                            target,
                            StandardOpenOption.CREATE_NEW,
                            StandardOpenOption.WRITE
                    )
            ) {
                created.set(true);
                output.write(bytes);
            }

        } catch (IOException e) {
            throw new IllegalStateException(
                    "메뉴 이미지 저장에 실패했습니다.",
                    e
            );
        }
    }

    // 원본 파일명 정리
    private String originalFileName(MultipartFile image) {
        String name = image.getOriginalFilename();

        if (name == null || name.isBlank()) {
            return "image";
        }

        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        name = name.replaceAll("\\p{Cntrl}", "");

        if (name.isBlank()) {
            return "image";
        }

        return name.substring(0, Math.min(name.length(), 100));
    }
}