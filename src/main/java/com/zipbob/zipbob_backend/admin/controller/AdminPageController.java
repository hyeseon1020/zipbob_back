package com.zipbob.zipbob_backend.admin.controller;

import com.zipbob.zipbob_backend.admin.dto.UserResponseDto;
import com.zipbob.zipbob_backend.dto.MenuListResponseDto;
import com.zipbob.zipbob_backend.service.CodeService;
import com.zipbob.zipbob_backend.service.MenuService;
import com.zipbob.zipbob_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
// 관리자 URL에 맞는 HTML 화면
public class AdminPageController {

    private final MenuService menuService;
    private final CodeService codeService;
    private final UserService userService;

    // 관리자 화면에서 공통으로 사용할 로그인 아이디
    @ModelAttribute("adminId")
    public String adminId(Principal principal) {
        return principal == null ? "" : principal.getName();
    }
    // 로그인
    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }
    // 대시보드
    @GetMapping({"", "/dashboard"})
    public String dashboard() {
        return "admin/dashboard";
    }
    // 메뉴 목록·검색·카테고리·페이지네이션
    @GetMapping("/menus")
    public String menus(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "page", defaultValue = "1") int page,
            Model model
    ) {
        String searchKeyword = keyword.trim();
        String searchCategory = category.trim();
        int currentPage = Math.max(page, 1);

        Page<MenuListResponseDto> menuPage =
                menuService.getMenusPage(
                        searchKeyword,
                        searchCategory,
                        currentPage
                );

        // 마지막 페이지를 초과한 요청은 유효한 마지막 페이지로 이동
        int lastPage = Math.max(menuPage.getTotalPages(), 1);

        if (currentPage > lastPage) {
            currentPage = lastPage;

            menuPage = menuService.getMenusPage(
                    searchKeyword,
                    searchCategory,
                    currentPage
            );
        }

        // 페이지 번호는 5개씩 표시
        // 1~5, 6~10, 11~15 ...
        int startPage = ((currentPage - 1) / 5) * 5 + 1;
        int endPage = Math.min(startPage + 4, lastPage);

        model.addAttribute("menus", menuPage.getContent());
        model.addAttribute("menuPage", menuPage);

        model.addAttribute("categories", codeService.getCategoryCodes());

        model.addAttribute("keyword", searchKeyword);
        model.addAttribute("category", searchCategory);

        model.addAttribute("currentPage", currentPage);
        model.addAttribute("startPage", startPage);
        model.addAttribute("endPage", endPage);

        return "admin/menus/list";
    }
    // 메뉴 등록
    @GetMapping("/menus/form")
    public String menuForm() {
        return "admin/menus/form";
    }
    // 공통코드 목록
    @GetMapping("/codes")
    public String codes() {
        return "admin/codes/list";
    }
    // 공통코드 등록
    @GetMapping("/codes/form")
    public String codeForm() {
        return "admin/codes/form";
    }
    // 회원 목록
    @GetMapping("/users")
    public String users(Model model) {
        //회원 리스트 가져오기
        List<UserResponseDto> userList = userService.getAllUsers();

        // 모델에 담아서 타임리프 화면으로 전달
        model.addAttribute("users", userList);

        // templates/admin/users/list.html 반환
        return "admin/users/list";
    }
}
