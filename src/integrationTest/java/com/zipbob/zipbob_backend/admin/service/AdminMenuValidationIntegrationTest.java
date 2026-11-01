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
// 잘못된 입력 차단 테스트
class AdminMenuValidationIntegrationTest {

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
    @DisplayName("신규 등록 시 이미지가 없으면 거절한다")
    void rejectMissingImage() throws IOException {
        var request = request("png");
        request.setImage(null);
        assertCreateRejected(request, "메뉴 이미지를 첨부해주세요.");
    }

    @Test
    @DisplayName("신규 등록 시 빈 이미지 파일을 거절한다")
    void rejectEmptyImage() throws IOException {
        var request = request("png");
        request.setImage(new MockMultipartFile("image", "empty.png", "image/png", new byte[0]));
        assertCreateRejected(request, "메뉴 이미지를 첨부해주세요.");
    }

    @Test
    @DisplayName("PNG 확장자와 MIME 타입으로 위장한 텍스트 파일을 거절한다")
    void rejectFakeImage() throws IOException {
        var request = request("png");
        request.setImage(new MockMultipartFile("image", "fake.png", "image/png",
                "not an image".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertCreateRejected(request, "정상적인 JPG 또는 PNG 이미지를 첨부해주세요.");
    }

    @Test
    @DisplayName("10MiB를 1바이트 초과한 업로드를 거절한다")
    void rejectOversizedImage() throws IOException {
        var request = request("png");
        request.setImage(new MockMultipartFile("image", "large.png", "image/png",
                new byte[10 * 1024 * 1024 + 1]));
        assertCreateRejected(request, "이미지는 10MB 이하로 첨부해주세요.");
    }

    @Test
    @DisplayName("실제 GIF 이미지는 PNG로 이름을 바꿔도 거절한다")
    void rejectGifImage() throws IOException {
        var request = request("gif");
        request.setImage(new MockMultipartFile("image", "fake.png", "image/png",
                request.getImage().getBytes()));
        assertCreateRejected(request, "JPG 또는 PNG 이미지만 등록할 수 있습니다.");
    }

    @Test
    @DisplayName("존재하지 않는 카테고리 코드를 거절한다")
    void rejectUnknownCategory() throws IOException {
        var request = request("png");
        request.setCategory("missing_" + UUID.randomUUID());
        assertCreateRejected(request, "사용 가능한 카테고리를 선택해주세요.");
    }

    @Test
    @DisplayName("선택 목록에 없는 난이도 코드를 거절한다")
    void rejectUnknownDifficulty() throws IOException {
        var request = request("png");
        request.setDifficultyLvl("missing_" + UUID.randomUUID());
        assertCreateRejected(request, "사용 가능한 난이도를 선택해주세요.");
    }

    @Test
    @DisplayName("음식 특징을 선택하지 않으면 거절한다")
    void rejectEmptyTags() throws IOException {
        var request = request("png");
        request.setTagIds(List.of());
        assertCreateRejected(request, "음식 특징을 최소 1개 선택해주세요.");
    }

    @Test
    @DisplayName("존재하지 않는 음식 특징이 포함되면 거절한다")
    void rejectUnknownTag() throws IOException {
        var request = request("png");
        request.setTagIds(List.of(tagId, "missing_" + UUID.randomUUID()));
        assertCreateRejected(request, "사용할 수 없는 음식 특징이 포함되어 있습니다.");
    }

    @Test
    @DisplayName("조리 시간이 0시간 0분이면 거절한다")
    void rejectZeroCookingTime() throws IOException {
        var request = request("png");
        request.setCookTime(0);
        request.setCookMinute(0);
        assertCreateRejected(request, "조리 시간은 1분 이상 입력해주세요.");
    }

    @Test
    @DisplayName("잘못된 이미지로 수정하면 기존 메뉴·태그·이미지가 유지된다")
    void rejectInvalidReplacement() throws IOException {
        Integer menuId = adminMenuService.create(request("png"), adminId);
        var menuBefore = jdbc.queryForMap(
                "SELECT * FROM zipbob_app.menu_mng WHERE menu_id = ?", menuId);
        var tagsBefore = jdbc.queryForList(
                "SELECT * FROM zipbob_app.tag_mng WHERE menu_id = ? ORDER BY tag_seq", menuId);
        var filesBefore = jdbc.queryForList(
                "SELECT * FROM zipbob_app.file_mng WHERE menu_id = ? ORDER BY file_id", menuId);
        Path oldPath = IMAGE_DIR.resolve("menus_" + menuId + ".png");
        byte[] oldBytes = Files.readAllBytes(oldPath);
        var update = request("png");
        update.setMenuName("저장되면 안 되는 이름");
        update.setImage(new MockMultipartFile("image", "fake.png", "image/png", new byte[]{1, 2, 3}));

        assertThatThrownBy(() -> adminMenuService.update(menuId, update, adminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("정상적인 JPG 또는 PNG 이미지를 첨부해주세요.");

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
    }

    private void assertCreateRejected(MenuCreateRequestDto request, String message) throws IOException {
        var menusBefore = jdbc.queryForList(
                "SELECT * FROM zipbob_app.menu_mng WHERE created_id = ? ORDER BY menu_id", adminId);
        assertThatThrownBy(() -> adminMenuService.create(request, adminId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);

        // 난이도 기준 메뉴를 포함해 기존 데이터는 그대로이고 추가 행은 없어야 한다.
        assertThat(jdbc.queryForList(
                "SELECT * FROM zipbob_app.menu_mng WHERE created_id = ? ORDER BY menu_id", adminId))
                .isEqualTo(menusBefore);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM zipbob_app.tag_mng WHERE created_id = ?",
                Integer.class, adminId)).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM zipbob_app.file_mng WHERE created_id = ?",
                Integer.class, adminId)).isZero();
        try (var paths = Files.list(IMAGE_DIR)) {
            assertThat(paths.toList()).isEmpty();
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
            return Files.createTempDirectory("zipbob-menu-validation-");
        } catch (IOException e) {
            throw new IllegalStateException("테스트 이미지 폴더 생성 실패", e);
        }
    }
}
