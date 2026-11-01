package com.zipbob.zipbob_backend.admin.service;

import com.zipbob.zipbob_backend.admin.dto.MenuCreateRequestDto;
import com.zipbob.zipbob_backend.entity.Code;
import com.zipbob.zipbob_backend.entity.Menu;
import com.zipbob.zipbob_backend.repository.CodeRepository;
import com.zipbob.zipbob_backend.repository.MenuRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.imageio.ImageIO;
import javax.sql.DataSource;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// 이미지 교체 테스트
class AdminMenuImageReplaceIntegrationTest {

    // 테스트 전체에 트랜잭션을 걸지 않고 서비스 커밋 후 DB를 조회한다.
    private static final Path IMAGE_DIR = createImageDirectory();

    @DynamicPropertySource
    static void imageDirectory(DynamicPropertyRegistry registry) {
        registry.add("app.menu-image-dir", IMAGE_DIR::toString);
    }

    @Autowired
    private AdminMenuService adminMenuService;

    @Autowired
    private MenuRepository menuRepository;

    @Autowired
    private CodeRepository codeRepository;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

    private JdbcTemplate jdbc;
    private String adminId;
    private String categoryId;
    private String tagId;
    private String difficultyId;

    @BeforeEach
    void setUp() {
        jdbc = new JdbcTemplate(dataSource);
        String suffix = UUID.randomUUID().toString().replace("-", "");
        adminId = "menu_test_" + suffix;
        categoryId = "cat_" + suffix;
        tagId = "tag_" + suffix;
        difficultyId = "diff_" + suffix;

        saveCode(categoryId, "테스트 카테고리", "category_code", "Y");
        saveCode(tagId, "간편식", "tag_code", "Y");

        // 현재 난이도 선택 목록은 menu_mng의 기존 난이도에서 조회한다.
        menuRepository.saveAndFlush(Menu.create(
                "난이도 기준 메뉴", categoryId, "기준 데이터",
                1, 0, 10, "밥", "준비한다", difficultyId, adminId
        ));
    }

    @AfterEach
    void cleanUp() throws IOException {
        try {
            // 공유 데이터는 삭제하지 않고 이번 테스트가 생성한 메뉴만 정리한다.
            jdbc.update("DELETE FROM zipbob_app.tag_mng WHERE menu_id IN "
                    + "(SELECT menu_id FROM zipbob_app.menu_mng WHERE created_id = ?)", adminId);
            jdbc.update("DELETE FROM zipbob_app.file_mng WHERE menu_id IN "
                    + "(SELECT menu_id FROM zipbob_app.menu_mng WHERE created_id = ?)", adminId);
            jdbc.update("DELETE FROM zipbob_app.menu_mng WHERE created_id = ?", adminId);
            jdbc.update("DELETE FROM zipbob_app.code_mng WHERE created_id = ?", adminId);
        } finally {
            try (var paths = Files.list(IMAGE_DIR)) {
                for (Path path : paths.toList()) {
                    Files.deleteIfExists(path);
                }
            }
        }
    }

    @org.junit.jupiter.api.AfterAll
    static void removeImageDirectory() throws IOException {
        Files.deleteIfExists(IMAGE_DIR);
    }

    @Test
    @DisplayName("PNG 교체 시 새 이미지가 활성화되고 기존 파일은 삭제된다")
    void replaceWithPng() throws IOException {
        verifyReplacement("png", false);
    }

    @Test
    @DisplayName("JPG 교체 시 PNG로 저장되고 기존 파일은 삭제된다")
    void replaceWithJpeg() throws IOException {
        verifyReplacement("jpg", false);
    }

    @Test
    @DisplayName("기존 이미지 파일은 트랜잭션 커밋 전까지 유지되고 커밋 후 삭제된다")
    void deleteOldImageOnlyAfterCommit() throws IOException {
        verifyReplacement("png", true);
    }

    private void verifyReplacement(String format, boolean inspectBeforeCommit) throws IOException {
        Integer menuId = adminMenuService.create(request("png"), adminId);
        Menu before = menuRepository.findByIdWithFiles(menuId).orElseThrow();
        assertThat(before.getFiles()).hasSize(1);
        var original = before.getFiles().getFirst();
        Path oldPath = IMAGE_DIR.resolve(original.getFileSaveName());
        byte[] oldBytes = Files.readAllBytes(oldPath);
        String editorId = "editor_" + UUID.randomUUID().toString().replace("-", "");

        MenuCreateRequestDto update = request(format);
        update.setImage(replacementImage(format));
        update.setMenuName("이미지를 교체한 메뉴");

        if (inspectBeforeCommit) {
            // 서비스가 이 트랜잭션에 참여하도록 하여 커밋 직전 상태도 확인한다.
            var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);
            transaction.executeWithoutResult(status -> {
                adminMenuService.update(menuId, update, editorId);
                assertThat(oldPath).isRegularFile();
                try {
                    assertThat(Files.readAllBytes(oldPath)).isEqualTo(oldBytes);
                } catch (IOException e) {
                    throw new java.io.UncheckedIOException(e);
                }
                assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM zipbob_app.file_mng WHERE menu_id = ?",
                        Integer.class, menuId)).isEqualTo(2);
                assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM zipbob_app.file_mng WHERE menu_id = ? AND use_yn = 'Y'",
                        Integer.class, menuId)).isEqualTo(1);
            });
        } else {
            adminMenuService.update(menuId, update, editorId);
        }

        // 실제 커밋 이후 별도 조회로 파일 정보와 디스크 상태를 확인한다.
        Menu after = menuRepository.findByIdWithFiles(menuId).orElseThrow();
        assertThat(after.getMenuName()).isEqualTo(update.getMenuName());
        assertThat(after.getCreatedId()).isEqualTo(adminId);
        assertThat(after.getUpdatedId()).isEqualTo(editorId);
        assertThat(after.getFiles()).hasSize(2);
        var previous = after.getFiles().stream()
                .filter(file -> file.getFileId().equals(original.getFileId()))
                .findFirst().orElseThrow();
        assertThat(previous.getUseYn()).isEqualTo("N");
        assertThat(previous.getFilePath()).isEqualTo(original.getFilePath());
        assertThat(previous.getCreatedId()).isEqualTo(adminId);
        assertThat(previous.getUpdatedId()).isEqualTo(editorId);
        assertThat(previous.getUpdatedDt()).isNotNull();

        var active = after.getFiles().stream()
                .filter(file -> "Y".equals(file.getUseYn())).toList();
        assertThat(active).hasSize(1);
        var current = active.getFirst();
        assertThat(current.getFileId()).isNotEqualTo(original.getFileId());
        assertThat(current.getFileSaveName()).isNotEqualTo(original.getFileSaveName());
        assertThat(current.getFileSaveName()).startsWith("menus_" + menuId + "_").endsWith(".png");
        assertThat(current.getFilePath()).isEqualTo("/images/menus/" + current.getFileSaveName());
        assertThat(current.getFileName()).isEqualTo("replacement." + format);
        assertThat(current.getCreatedId()).isEqualTo(editorId);

        Path newPath = IMAGE_DIR.resolve(current.getFileSaveName());
        assertThat(oldPath).doesNotExist();
        assertThat(newPath).isRegularFile();
        byte[] bytes = Files.readAllBytes(newPath);
        assertThat(current.getFileSize().longValue()).isEqualTo(bytes.length);
        assertThat(bytes).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e,
                (byte) 0x47, (byte) 0x0d, (byte) 0x0a, (byte) 0x1a, (byte) 0x0a);
        BufferedImage decoded = ImageIO.read(newPath.toFile());
        assertThat(decoded).isNotNull();
        try {
            // 기존 이미지와 다른 크기로 새 이미지가 저장되었는지 확인한다.
            assertThat(decoded.getWidth()).isEqualTo(24);
            assertThat(decoded.getHeight()).isEqualTo(12);
        } finally {
            decoded.flush();
        }
        try (var paths = Files.list(IMAGE_DIR)) {
            assertThat(paths.toList()).containsExactly(newPath);
        }
        assertThat(adminMenuService.getDetail(menuId).imageUrl()).isEqualTo(current.getFilePath());
    }

    private MockMultipartFile replacementImage(String format) throws IOException {
        BufferedImage image = new BufferedImage(24, 12, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertThat(ImageIO.write(image, format, output)).isTrue();
            return new MockMultipartFile("image", "replacement." + format,
                    "jpg".equals(format) ? "image/jpeg" : "image/png", output.toByteArray());
        } finally {
            image.flush();
        }
    }

    private MenuCreateRequestDto request(String format) throws IOException {
        MenuCreateRequestDto request = new MenuCreateRequestDto();
        request.setMenuName("테스트 간장계란밥");
        request.setCategory(categoryId);
        request.setTagIds(List.of(tagId));
        request.setServings(1);
        request.setCookTime(0);
        request.setCookMinute(10);
        request.setIngredients("밥 200g, 달걀 1개, 간장 1작은술");
        request.setSteps("달걀을 굽는다.\n밥 위에 올린다.");
        request.setDifficultyLvl(difficultyId);

        BufferedImage image = new BufferedImage(16, 9, BufferedImage.TYPE_INT_RGB);
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            assertThat(ImageIO.write(image, format, output)).isTrue();
            request.setImage(new MockMultipartFile("image", "recipe." + format,
                    "jpg".equals(format) ? "image/jpeg" : "image/png", output.toByteArray()));
        } finally {
            image.flush();
        }
        return request;
    }

    private void saveCode(String id, String name, String parent, String useYn) {
        Code code = new Code();
        code.setCodeId(id);
        code.setCodeName(name);
        code.setCodeParent(parent);
        code.setCodeDepth(2);
        code.setUseYn(useYn);
        code.setCreatedId(adminId);
        codeRepository.saveAndFlush(code);
    }

    private static Path createImageDirectory() {
        try {
            return Files.createTempDirectory("zipbob-menu-replace-");
        } catch (IOException e) {
            throw new IllegalStateException("테스트 이미지 폴더 생성 실패", e);
        }
    }
}
