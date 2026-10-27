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
import java.util.List;
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
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Service
public class AdminMenuService {

    private static final long MAX_UPLOAD_SIZE =
            10L * 1024 * 1024;

    private static final long MAX_PIXELS =
            12_000_000L;

    private static final int MAX_DESCRIPTION_LENGTH = 100;

    private final MenuRepository menuRepository;
    private final MenuFileRepository menuFileRepository;
    private final CodeRepository codeRepository;
    private final AdminMenuTagService adminMenuTagService;
    private final Path imageDirectory;

    public AdminMenuService(
            MenuRepository menuRepository,
            MenuFileRepository menuFileRepository,
            CodeRepository codeRepository,
            AdminMenuTagService adminMenuTagService,
            @Value("${app.menu-image-dir:./images/menus}")
            String imageDirectory
    ) {
        this.menuRepository = menuRepository;
        this.menuFileRepository = menuFileRepository;
        this.codeRepository = codeRepository;
        this.adminMenuTagService = adminMenuTagService;

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

    // 등록·수정 팝업에서 사용할 선택 목록
    public record FormOptions(
            List<Option> categories,
            List<Option> difficulties,
            List<Option> tags
    ) {
    }

    // 수정 팝업에 표시할 기존 메뉴 정보
    public record MenuEditDetail(
            Integer menuId,
            String menuName,
            String category,
            String description,
            Integer servings,
            Integer cookTime,
            Integer cookMinute,
            String ingredients,
            String steps,
            String difficultyLvl,
            String imageUrl,
            List<String> tagIds
    ) {
    }

    // 카테고리·난이도·음식 특징 조회
    @Transactional(readOnly = true)
    public FormOptions getFormOptions() {
        List<Option> categories = codeRepository
                .findByCodeParent("category_code")
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
                .findByCodeParent("tag_code")
                .stream()
                .map(code -> new Option(
                        code.getCodeId(),
                        code.getCodeName()
                ))
                .toList();

        return new FormOptions(
                categories,
                difficulties,
                tags
        );
    }

    // 수정 팝업에 표시할 메뉴 상세 조회
    @Transactional(readOnly = true)
    public MenuEditDetail getDetail(Integer menuId) {
        Menu menu = menuRepository.findByIdWithFiles(menuId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "메뉴를 찾을 수 없습니다."
                        )
                );

        // 현재 사용 중인 이미지 중 첫 번째 이미지 선택
        String imageUrl = menu.getFiles()
                .stream()
                .filter(file -> "Y".equals(file.getUseYn()))
                .map(MenuFile::getFilePath)
                .findFirst()
                .orElse(null);

        // 해당 메뉴에 연결된 사용 중인 태그 코드 조회
        List<String> tagIds = adminMenuTagService.getTagIds(menuId);

        return new MenuEditDetail(
                menu.getMenuId(),
                menu.getMenuName(),
                menu.getCategory(),
                menu.getDescription(),
                menu.getServings(),
                menu.getCookTime(),
                menu.getCookMinute(),
                menu.getIngredients(),
                menu.getSteps(),
                menu.getDifficultyLvl(),
                imageUrl,
                tagIds
        );
    }

    // 기존 메뉴 수정
    @Transactional
    public void update(
            Integer menuId,
            MenuCreateRequestDto request,
            String adminId
    ) {
        // 같은 메뉴의 수정 요청이 겹치면 순서대로 처리
        Menu menu = menuRepository.findByIdForUpdate(menuId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "메뉴를 찾을 수 없습니다."
                        )
                );

        // 카테고리·난이도 검사
        validateCodes(request);

        // 선택한 음식 특징 코드로 설명 문자열 생성
        String description = createDescription(
                request.getTagIds()
        );

        if (request.getCookTime() == 0
                && request.getCookMinute() == 0) {
            throw new IllegalArgumentException(
                    "조리 시간은 1분 이상 입력해주세요."
            );
        }

        MultipartFile image = request.getImage();

        // 새 이미지를 첨부했을 때만 이미지 교체
        if (image != null && !image.isEmpty()) {
            byte[] pngBytes = convertToPng(image);

            // 기존 파일을 덮어쓰지 않도록 별도 이름 사용
            String saveName = "menus_"
                    + menuId
                    + "_"
                    + UUID.randomUUID()
                    + ".png";

            Path target = imageDirectory.resolve(saveName);

            // 새 이미지 파일 저장
            // DB 롤백 시 이 파일을 정리하는 작업도 등록됨
            saveImage(target, pngBytes);

            // 현재 사용 중인 기존 이미지 정보 조회
            List<MenuFile> previous = menu.getFiles()
                    .stream()
                    .filter(file ->
                            "Y".equals(file.getUseYn())
                    )
                    .toList();

            // 기존 이미지 정보 비활성화
            previous.forEach(file ->
                    file.deactivate(adminId)
            );

            // 새 이미지 정보 등록
            MenuFile newImage = MenuFile.create(
                    menu,
                    "/images/menus/" + saveName,
                    pngBytes.length,
                    saveName,
                    originalFileName(image),
                    adminId
            );

            menuFileRepository.saveAndFlush(newImage);

            // DB 커밋이 완료된 뒤 기존 업로드 파일 정리
            TransactionSynchronizationManager
                    .registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    for (MenuFile file : previous) {
                                        String name =
                                                file.getFileSaveName();

                                        // 해당 메뉴의 관리 대상 파일만 정리
                                        if (name == null
                                                || !name.matches(
                                                "menus_"
                                                        + menuId
                                                        + "(?:_[0-9a-f-]{36})?\\.png"
                                        )
                                                || !(
                                                "/images/menus/"
                                                        + name
                                        ).equals(
                                                file.getFilePath()
                                        )) {
                                            continue;
                                        }

                                        try {
                                            Files.deleteIfExists(
                                                    imageDirectory.resolve(
                                                            name
                                                    )
                                            );
                                        } catch (IOException e) {
                                            log.warn(
                                                    "이전 메뉴 이미지 정리 실패: {}",
                                                    name,
                                                    e
                                            );
                                        }
                                    }
                                }
                            }
                    );
        }

        // 메뉴 텍스트 정보와 수정 이력 변경
        menu.update(
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

        // 기존 태그 연결 삭제 후 현재 선택한 태그로 교체
        adminMenuTagService.replace(
                menu,
                request.getTagIds(),
                adminId
        );

        // 변경 SQL을 DB에 반영
        // 최종 커밋은 트랜잭션 완료 시 처리
        menuRepository.flush();
    }

    // 신규 메뉴 등록
    @Transactional
    public Integer create(
            MenuCreateRequestDto request,
            String adminId
    ) {
        validateCodes(request);

        String description = createDescription(
                request.getTagIds()
        );

        if (request.getCookTime() == 0
                && request.getCookMinute() == 0) {
            throw new IllegalArgumentException(
                    "조리 시간은 1분 이상 입력해주세요."
            );
        }

        // 신규 등록에서는 이미지 필수
        byte[] pngBytes = convertToPng(
                request.getImage()
        );

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

        // 메뉴 INSERT 후 발급된 menu_id 확보
        menuRepository.saveAndFlush(menu);

        // 선택한 음식 특징을 tag_mng에 등록
        adminMenuTagService.create(
                menu,
                request.getTagIds(),
                adminId
        );

        String saveName =
                "menus_" + menu.getMenuId() + ".png";

        String filePath =
                "/images/menus/" + saveName;

        Path target = imageDirectory.resolve(saveName);

        saveImage(target, pngBytes);

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
    private void validateCodes(
            MenuCreateRequestDto request
    ) {
        FormOptions options = getFormOptions();

        boolean validCategory = options.categories()
                .stream()
                .anyMatch(option ->
                        option.codeId().equals(
                                request.getCategory()
                        )
                );

        if (!validCategory) {
            throw new IllegalArgumentException(
                    "사용 가능한 카테고리를 선택해주세요."
            );
        }

        boolean validDifficulty = options.difficulties()
                .stream()
                .anyMatch(option ->
                        option.codeId().equals(
                                request.getDifficultyLvl()
                        )
                );

        if (!validDifficulty) {
            throw new IllegalArgumentException(
                    "사용 가능한 난이도를 선택해주세요."
            );
        }
    }

    // 음식 특징 코드 → 음식 특징 이름 문자열
    private String createDescription(
            List<String> requestedTagIds
    ) {
        if (requestedTagIds == null
                || requestedTagIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "음식 특징을 최소 1개 선택해주세요."
            );
        }

        Map<String, String> availableTags = codeRepository
                .findByCodeParent("tag_code")
                .stream()
                .collect(Collectors.toMap(
                        code -> code.getCodeId(),
                        code -> code.getCodeName()
                ));

        // 중복 제거 및 전달받은 순서 유지
        Set<String> selectedTagIds =
                new LinkedHashSet<>();

        for (String tagId : requestedTagIds) {
            if (tagId == null
                    || !availableTags.containsKey(tagId)) {
                throw new IllegalArgumentException(
                        "사용할 수 없는 음식 특징이 포함되어 있습니다."
                );
            }

            selectedTagIds.add(tagId);
        }

        String description = selectedTagIds.stream()
                .map(availableTags::get)
                .collect(Collectors.joining(", "));

        int descriptionLength =
                description.codePointCount(
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

                BufferedImage bufferedImage =
                        reader.read(0);

                if (bufferedImage == null) {
                    throw new IllegalArgumentException(
                            "이미지 파일을 읽을 수 없습니다."
                    );
                }

                try (
                        ByteArrayOutputStream output =
                                new ByteArrayOutputStream()
                ) {
                    boolean converted = ImageIO.write(
                            bufferedImage,
                            "png",
                            output
                    );

                    if (!converted) {
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

    // 이미지 저장 및 DB 롤백 시 신규 파일 정리
    private void saveImage(
            Path target,
            byte[] bytes
    ) {
        AtomicBoolean created =
                new AtomicBoolean(false);

        TransactionSynchronizationManager
                .registerSynchronization(
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
                                            "메뉴 저장 롤백 후 이미지 삭제 실패: {}",
                                            target,
                                            e
                                    );
                                }
                            }
                        }
                );

        try {
            Files.createDirectories(imageDirectory);

            // 같은 이름의 기존 파일을 덮어쓰지 않음
            try (
                    OutputStream output =
                            Files.newOutputStream(
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
    private String originalFileName(
            MultipartFile image
    ) {
        String name = image.getOriginalFilename();

        if (name == null || name.isBlank()) {
            return "image";
        }

        // 경로를 제외한 파일명만 사용
        name = name.replace('\\', '/');
        name = name.substring(
                name.lastIndexOf('/') + 1
        );

        // 제어 문자 제거
        name = name.replaceAll("\\p{Cntrl}", "");

        if (name.isBlank()) {
            return "image";
        }

        return name.substring(
                0,
                Math.min(name.length(), 100)
        );
    }
}