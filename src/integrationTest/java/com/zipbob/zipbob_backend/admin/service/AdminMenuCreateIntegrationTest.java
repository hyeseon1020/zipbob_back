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
// 메뉴 등록 통합 테스트
class AdminMenuCreateIntegrationTest {

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
    @DisplayName("PNG 등록 시 메뉴·태그·파일 정보와 실제 이미지가 저장된다")
    void createWithPng() throws IOException {
        MenuCreateRequestDto request = request("png");

        Integer menuId = adminMenuService.create(request, adminId);

        assertSaved(menuId, request, "Y");
    }

    @Test
    @DisplayName("JPG 이미지는 실제 PNG 형식으로 변환되어 저장된다")
    void createWithJpeg() throws IOException {
        MenuCreateRequestDto request = request("jpg");

        Integer menuId = adminMenuService.create(request, adminId);

        assertSaved(menuId, request, "Y");
    }

    @Test
    @DisplayName("비활성 카테고리·태그도 관리자 등록에 사용하며 중복 태그는 한 번만 저장한다")
    void createWithInactiveCodesAndDuplicateTags() throws IOException {
        jdbc.update("UPDATE zipbob_app.code_mng SET use_yn = 'N' WHERE code_id IN (?, ?)",
                categoryId, tagId);
        MenuCreateRequestDto request = request("png");
        request.setTagIds(List.of(tagId, tagId));

        Integer menuId = adminMenuService.create(request, adminId);

        assertSaved(menuId, request, "N");
    }

    private void assertSaved(Integer menuId, MenuCreateRequestDto request,
                             String tagUseYn) throws IOException {
        Menu saved = menuRepository.findByIdWithFiles(menuId).orElseThrow();
        assertThat(saved.getMenuName()).isEqualTo(request.getMenuName());
        assertThat(saved.getCategory()).isEqualTo(categoryId);
        assertThat(saved.getDescription()).isEqualTo("간편식");
        assertThat(saved.getServings()).isEqualTo(1);
        assertThat(saved.getCookTime()).isZero();
        assertThat(saved.getCookMinute()).isEqualTo(10);
        assertThat(saved.getIngredients()).isEqualTo(request.getIngredients());
        assertThat(saved.getSteps()).isEqualTo(request.getSteps());
        assertThat(saved.getDifficultyLvl()).isEqualTo(difficultyId);
        assertThat(saved.getCreatedId()).isEqualTo(adminId);

        var tags = jdbc.queryForList(
                "SELECT code_id, use_yn, created_id FROM zipbob_app.tag_mng WHERE menu_id = ?",
                menuId);
        assertThat(tags).hasSize(1);
        assertThat(tags.getFirst().get("code_id")).isEqualTo(tagId);
        assertThat(tags.getFirst().get("use_yn")).isEqualTo(tagUseYn);
        assertThat(tags.getFirst().get("created_id")).isEqualTo(adminId);

        assertThat(saved.getFiles()).hasSize(1);
        var file = saved.getFiles().getFirst();
        String saveName = "menus_" + menuId + ".png";
        assertThat(file.getFileSaveName()).isEqualTo(saveName);
        assertThat(file.getFilePath()).isEqualTo("/images/menus/" + saveName);
        assertThat(file.getFileName()).isEqualTo(request.getImage().getOriginalFilename());
        assertThat(file.getUseYn()).isEqualTo("Y");
        assertThat(file.getCreatedId()).isEqualTo(adminId);

        Path imagePath = IMAGE_DIR.resolve(saveName);
        assertThat(imagePath).isRegularFile();
        byte[] bytes = Files.readAllBytes(imagePath);
        assertThat(file.getFileSize().longValue()).isEqualTo(bytes.length);
        assertThat(bytes).startsWith((byte) 0x89, (byte) 0x50, (byte) 0x4e,
                (byte) 0x47, (byte) 0x0d, (byte) 0x0a, (byte) 0x1a, (byte) 0x0a);
        BufferedImage image = ImageIO.read(imagePath.toFile());
        assertThat(image).isNotNull();
        try {
            assertThat(image.getWidth()).isEqualTo(16);
            assertThat(image.getHeight()).isEqualTo(9);
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
            return Files.createTempDirectory("zipbob-menu-create-");
        } catch (IOException e) {
            throw new IllegalStateException("테스트 이미지 폴더 생성 실패", e);
        }
    }
}
