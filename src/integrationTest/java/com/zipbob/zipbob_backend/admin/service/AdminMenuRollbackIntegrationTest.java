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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("integration")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
// 저장 실패·롤백 테스트
class AdminMenuRollbackIntegrationTest {

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
    @DisplayName("등록 후 트랜잭션이 롤백되면 메뉴·태그·파일 정보와 새 이미지가 제거된다")
    void rollbackCreate() throws IOException {
        var request = request("png");
        var before = ownedMenus();
        var createdId = new java.util.concurrent.atomic.AtomicReference<Integer>();
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            Integer menuId = adminMenuService.create(request, adminId);
            createdId.set(menuId);
            assertThat(IMAGE_DIR.resolve("menus_" + menuId + ".png")).isRegularFile();
            assertThat(jdbc.queryForObject(
                    "SELECT COUNT(*) FROM zipbob_app.file_mng WHERE menu_id = ?",
                    Integer.class, menuId)).isEqualTo(1);
            throw new ForcedRollbackException();
        })).isInstanceOf(ForcedRollbackException.class);

        assertThat(createdId.get()).isNotNull();
        assertThat(menuRepository.findById(createdId.get())).isEmpty();
        assertThat(ownedMenus()).isEqualTo(before);
        assertNoOwnedChildren();
        assertNoImages();
    }

    @Test
    @DisplayName("이미지 교체 후 롤백되면 기존 데이터·파일이 유지되고 새 파일만 삭제된다")
    void rollbackReplacement() throws IOException {
        Integer menuId = adminMenuService.create(request("png"), adminId);
        var menuBefore = jdbc.queryForMap(
                "SELECT * FROM zipbob_app.menu_mng WHERE menu_id = ?", menuId);
        var tagsBefore = jdbc.queryForList(
                "SELECT * FROM zipbob_app.tag_mng WHERE menu_id = ? ORDER BY tag_seq", menuId);
        var filesBefore = jdbc.queryForList(
                "SELECT * FROM zipbob_app.file_mng WHERE menu_id = ? ORDER BY file_id", menuId);
        Path oldPath = IMAGE_DIR.resolve("menus_" + menuId + ".png");
        byte[] oldBytes = Files.readAllBytes(oldPath);
        String newTag = "tag_" + UUID.randomUUID().toString().replace("-", "");
        saveCode(newTag, "변경 음식 특징", "tag_code", "Y");
        var update = request("jpg");
        update.setMenuName("롤백될 메뉴 이름");
        update.setTagIds(List.of(newTag));
        var newPath = new java.util.concurrent.atomic.AtomicReference<Path>();
        var transaction = new org.springframework.transaction.support.TransactionTemplate(transactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            adminMenuService.update(menuId, update, adminId);
            String saveName = jdbc.queryForObject(
                    "SELECT file_save_name FROM zipbob_app.file_mng WHERE menu_id = ? AND use_yn = 'Y'",
                    String.class, menuId);
            newPath.set(IMAGE_DIR.resolve(saveName));
            assertThat(newPath.get()).isNotEqualTo(oldPath).isRegularFile();
            assertThat(oldPath).isRegularFile();
            assertThat(jdbc.queryForObject(
                    "SELECT menu_name FROM zipbob_app.menu_mng WHERE menu_id = ?",
                    String.class, menuId)).isEqualTo(update.getMenuName());
            assertThat(jdbc.queryForObject(
                    "SELECT code_id FROM zipbob_app.tag_mng WHERE menu_id = ?",
                    String.class, menuId)).isEqualTo(newTag);
            throw new ForcedRollbackException();
        })).isInstanceOf(ForcedRollbackException.class);

        assertThat(newPath.get()).isNotNull();
        assertThat(newPath.get()).doesNotExist();
        assertThat(jdbc.queryForMap("SELECT * FROM zipbob_app.menu_mng WHERE menu_id = ?", menuId))
                .isEqualTo(menuBefore);
        assertThat(jdbc.queryForList(
                "SELECT * FROM zipbob_app.tag_mng WHERE menu_id = ? ORDER BY tag_seq", menuId))
                .isEqualTo(tagsBefore);
        assertThat(jdbc.queryForList(
                "SELECT * FROM zipbob_app.file_mng WHERE menu_id = ? ORDER BY file_id", menuId))
                .isEqualTo(filesBefore);
        assertThat(Files.readAllBytes(oldPath)).isEqualTo(oldBytes);
        try (var paths = Files.list(IMAGE_DIR)) {
            assertThat(paths.toList()).containsExactly(oldPath);
        }
        assertThat(adminMenuService.getDetail(menuId).imageUrl())
                .isEqualTo("/images/menus/" + oldPath.getFileName());
    }

    @Test
    @DisplayName("이미지 저장 경로가 파일이면 등록에 실패하고 DB 변경이 롤백된다")
    void rollbackWhenImageCannotBeSaved() throws IOException {
        var request = request("png");
        var before = ownedMenus();
        // 이 클래스 전용 임시 폴더를 파일로 바꾸어 실제 파일 시스템 오류를 만든다.
        Files.delete(IMAGE_DIR);
        try {
            Files.writeString(IMAGE_DIR, "test-only blocker");
            assertThatThrownBy(() -> adminMenuService.create(request, adminId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("메뉴 이미지 저장에 실패했습니다.")
                    .hasCauseInstanceOf(IOException.class);
            assertThat(ownedMenus()).isEqualTo(before);
            assertNoOwnedChildren();
            assertThat(Files.readString(IMAGE_DIR)).isEqualTo("test-only blocker");
        } finally {
            Files.deleteIfExists(IMAGE_DIR);
            Files.createDirectories(IMAGE_DIR);
        }
        assertNoImages();
    }

    private java.util.List<java.util.Map<String, Object>> ownedMenus() {
        return jdbc.queryForList(
                "SELECT * FROM zipbob_app.menu_mng WHERE created_id = ? ORDER BY menu_id", adminId);
    }

    private void assertNoOwnedChildren() {
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM zipbob_app.tag_mng WHERE created_id = ?",
                Integer.class, adminId)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM zipbob_app.file_mng WHERE created_id = ?",
                Integer.class, adminId)).isZero();
    }

    private void assertNoImages() throws IOException {
        try (var paths = Files.list(IMAGE_DIR)) {
            assertThat(paths.toList()).isEmpty();
        }
    }

    // 서비스 처리가 완료된 뒤 바깥 트랜잭션의 실패를 재현한다.
    private static class ForcedRollbackException extends RuntimeException {
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
            return Files.createTempDirectory("zipbob-menu-rollback-");
        } catch (IOException e) {
            throw new IllegalStateException("테스트 이미지 폴더 생성 실패", e);
        }
    }
}
