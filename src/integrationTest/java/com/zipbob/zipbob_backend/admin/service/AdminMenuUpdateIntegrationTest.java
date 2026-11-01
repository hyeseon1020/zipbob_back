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
// 메뉴 수정·기존 이미지 유지 테스트
class AdminMenuUpdateIntegrationTest {

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
    @DisplayName("이미지 미첨부 수정 시 메뉴 정보와 태그가 변경되고 기존 이미지가 유지된다")
    void updateWithoutImage() throws IOException {
        verifyUpdate(false, false);
    }

    @Test
    @DisplayName("빈 이미지 파트로 수정해도 기존 이미지 정보와 파일이 유지된다")
    void updateWithEmptyImage() throws IOException {
        verifyUpdate(true, false);
    }

    @Test
    @DisplayName("비활성 코드로 수정할 수 있고 중복 태그는 하나만 저장된다")
    void updateWithInactiveCodes() throws IOException {
        verifyUpdate(false, true);
    }

    private void verifyUpdate(boolean emptyImage, boolean inactive) throws IOException {
        Integer menuId = adminMenuService.create(request("png"), adminId);
        Menu before = menuRepository.findByIdWithFiles(menuId).orElseThrow();
        assertThat(before.getFiles()).hasSize(1);
        var originalFile = before.getFiles().getFirst();
        Path originalPath = IMAGE_DIR.resolve(originalFile.getFileSaveName());
        byte[] originalBytes = Files.readAllBytes(originalPath);

        String suffix = UUID.randomUUID().toString().replace("-", "");
        String newCategory = "cat_" + suffix;
        String newTag = "tag_" + suffix;
        String newDifficulty = "diff_" + suffix;
        String editorId = "editor_" + suffix;
        String useYn = inactive ? "N" : "Y";
        saveCode(newCategory, "수정 카테고리", "category_code", useYn);
        saveCode(newTag, "든든한 음식", "tag_code", useYn);
        menuRepository.saveAndFlush(Menu.create(
                "수정 난이도 기준", newCategory, "기준 데이터",
                1, 0, 10, "밥", "준비한다", newDifficulty, adminId
        ));

        MenuCreateRequestDto update = request("png");
        update.setMenuName("수정한 계란밥");
        update.setCategory(newCategory);
        update.setTagIds(List.of(newTag, newTag));
        update.setServings(2);
        update.setCookTime(1);
        update.setCookMinute(20);
        update.setIngredients("밥 400g, 달걀 2개");
        update.setSteps("달걀을 굽는다.\n밥과 섞는다.");
        update.setDifficultyLvl(newDifficulty);
        update.setImage(emptyImage
                ? new MockMultipartFile("image", "", "application/octet-stream", new byte[0])
                : null);

        adminMenuService.update(menuId, update, editorId);

        // 서비스 트랜잭션이 커밋된 후 새로 조회한다.
        Menu after = menuRepository.findByIdWithFiles(menuId).orElseThrow();
        assertThat(after.getMenuId()).isEqualTo(menuId);
        assertThat(after.getMenuName()).isEqualTo(update.getMenuName());
        assertThat(after.getCategory()).isEqualTo(newCategory);
        assertThat(after.getDescription()).isEqualTo("든든한 음식");
        assertThat(after.getServings()).isEqualTo(2);
        assertThat(after.getCookTime()).isEqualTo(1);
        assertThat(after.getCookMinute()).isEqualTo(20);
        assertThat(after.getIngredients()).isEqualTo(update.getIngredients());
        assertThat(after.getSteps()).isEqualTo(update.getSteps());
        assertThat(after.getDifficultyLvl()).isEqualTo(newDifficulty);
        assertThat(after.getCreatedId()).isEqualTo(adminId);
        assertThat(after.getCreatedDt()).isEqualTo(before.getCreatedDt());
        assertThat(after.getUpdatedId()).isEqualTo(editorId);
        assertThat(after.getUpdatedDt()).isNotNull();

        // 기존 태그는 제거되고 새 태그만 한 번 저장되어야 한다.
        var tags = jdbc.queryForList(
                "SELECT code_id, use_yn, created_id FROM zipbob_app.tag_mng WHERE menu_id = ?",
                menuId);
        assertThat(tags).hasSize(1);
        assertThat(tags.getFirst().get("code_id")).isEqualTo(newTag);
        assertThat(tags.getFirst().get("use_yn")).isEqualTo(useYn);
        assertThat(tags.getFirst().get("created_id")).isEqualTo(editorId);

        // 파일 행을 새로 만들거나 기존 파일을 변경하면 안 된다.
        assertThat(after.getFiles()).hasSize(1);
        var unchangedFile = after.getFiles().getFirst();
        assertThat(unchangedFile.getFileId()).isEqualTo(originalFile.getFileId());
        assertThat(unchangedFile.getFilePath()).isEqualTo(originalFile.getFilePath());
        assertThat(unchangedFile.getFileSaveName()).isEqualTo(originalFile.getFileSaveName());
        assertThat(unchangedFile.getFileName()).isEqualTo(originalFile.getFileName());
        assertThat(unchangedFile.getFileSize()).isEqualTo(originalFile.getFileSize());
        assertThat(unchangedFile.getUseYn()).isEqualTo("Y");
        assertThat(unchangedFile.getCreatedId()).isEqualTo(originalFile.getCreatedId());
        assertThat(unchangedFile.getUpdatedId()).isEqualTo(originalFile.getUpdatedId());
        assertThat(unchangedFile.getUpdatedDt()).isEqualTo(originalFile.getUpdatedDt());
        assertThat(originalPath).isRegularFile();
        assertThat(Files.readAllBytes(originalPath)).isEqualTo(originalBytes);
        try (var paths = Files.list(IMAGE_DIR)) {
            assertThat(paths.toList()).containsExactly(originalPath);
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
            return Files.createTempDirectory("zipbob-menu-update-");
        } catch (IOException e) {
            throw new IllegalStateException("테스트 이미지 폴더 생성 실패", e);
        }
    }
}
