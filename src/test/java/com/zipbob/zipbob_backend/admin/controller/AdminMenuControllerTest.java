package com.zipbob.zipbob_backend.admin.controller;

import com.zipbob.zipbob_backend.admin.dto.MenuCreateRequestDto;
import com.zipbob.zipbob_backend.admin.service.AdminMenuService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.http.HttpMethod;
import java.util.NoSuchElementException;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminMenuController.class)
@ContextConfiguration(classes = AdminMenuController.class)
@AutoConfigureMockMvc(addFilters = false)
// 실제 관리자 메뉴 컨트롤러의 HTTP 테스트
class AdminMenuControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AdminMenuService service;

    private MockMultipartHttpServletRequestBuilder request(HttpMethod method) {
        var request = multipart(method, method == HttpMethod.POST
                ? "/api/admin/menus" : "/api/admin/menus/7");
        request.file("image", new byte[]{1, 2, 3});
        request.param("menuName", "계란밥").param("category", "rice_bowl")
                .param("tagIds", "simple", "quick").param("servings", "1")
                .param("cookTime", "0").param("cookMinute", "10")
                .param("ingredients", "밥, 달걀").param("steps", "달걀을 익힌다.")
                .param("difficultyLvl", "쉬움").principal(() -> "admin-test");
        return request;
    }

    @Test
    void create_returns201AndBindsMultipartAndAdmin() throws Exception {
        when(service.create(any(), eq("admin-test"))).thenReturn(7);
        mvc.perform(request(HttpMethod.POST)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.menuId").value(7))
                .andExpect(jsonPath("$.message").value("신규 메뉴가 등록되었습니다."));
        var captor = org.mockito.ArgumentCaptor.forClass(MenuCreateRequestDto.class);
        verify(service).create(captor.capture(), eq("admin-test"));
        assertThat(captor.getValue().getMenuName()).isEqualTo("계란밥");
        assertThat(captor.getValue().getTagIds()).containsExactly("simple", "quick");
        assertThat(captor.getValue().getImage().getBytes()).containsExactly((byte)1, (byte)2, (byte)3);
    }

    @Test
    void update_returns200AndPassesMenuAndAdmin() throws Exception {
        mvc.perform(request(HttpMethod.PUT)).andExpect(status().isOk())
                .andExpect(jsonPath("$.menuId").value(7));
        verify(service).update(eq(7), any(), eq("admin-test"));
    }

    @ParameterizedTest
    @CsvSource({"POST,menuName,' '", "PUT,menuName,' '", "POST,servings,0",
            "PUT,servings,101", "POST,cookMinute,60", "PUT,cookMinute,-1",
            "POST,servings,abc"})
    void invalidInput_returns400WithoutCallingService(String method, String field, String value) throws Exception {
        var req = request(HttpMethod.valueOf(method));
        req.with(r -> { r.setParameter(field, value); return r; });
        mvc.perform(req).andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isNotEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void longName_returns400() throws Exception {
        var req = request(HttpMethod.POST);
        req.with(r -> { r.setParameter("menuName", "가".repeat(51)); return r; });
        mvc.perform(req).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("메뉴명은 50자 이하로 입력해주세요."));
        verifyNoInteractions(service);
    }

    @Test
    void missingDetail_returns404() throws Exception {
        when(service.getDetail(7)).thenThrow(new NoSuchElementException("메뉴가 없습니다."));
        mvc.perform(get("/api/admin/menus/7")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("메뉴가 없습니다."));
    }

    @Test
    void missingUpdate_returns404() throws Exception {
        doThrow(new NoSuchElementException("메뉴가 없습니다.")).when(service).update(eq(7), any(), anyString());
        mvc.perform(request(HttpMethod.PUT)).andExpect(status().isNotFound());
    }

    @Test
    void invalidImage_returns400() throws Exception {
        when(service.create(any(), anyString())).thenThrow(new IllegalArgumentException("잘못된 이미지입니다."));
        mvc.perform(request(HttpMethod.POST)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("잘못된 이미지입니다."));
    }

    @Test
    void storageFailure_returns500WithoutInternalDetails() throws Exception {
        when(service.create(any(), anyString())).thenThrow(new IllegalStateException("private filesystem path"));
        mvc.perform(request(HttpMethod.POST)).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("메뉴 저장에 실패했습니다. 잠시 후 다시 시도해주세요."));
    }
}
