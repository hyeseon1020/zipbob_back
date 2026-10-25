package com.zipbob.zipbob_backend.admin.controller;

import com.zipbob.zipbob_backend.admin.common.AdminPagination;
import com.zipbob.zipbob_backend.admin.dto.CodeSaveRequestDto;
import com.zipbob.zipbob_backend.admin.dto.UserResponseDto;
import com.zipbob.zipbob_backend.admin.service.AdminCodeService;
import com.zipbob.zipbob_backend.admin.service.AdminDashboardService;
import com.zipbob.zipbob_backend.dto.MenuListResponseDto;
import com.zipbob.zipbob_backend.service.CodeService;
import com.zipbob.zipbob_backend.service.MenuService;
import com.zipbob.zipbob_backend.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
/*
관리자 화면 요청 처리
 - 관리자 로그인 화면
 - 대시보드 통계 조회
 - 메뉴 목록·검색·페이지네이션
 - 공통코드 화면
 - 회원 목록·검색·페이지네이션
 */
public class AdminPageController {

    private final MenuService menuService;
    private final CodeService codeService;
    private final AdminCodeService adminCodeService;
    private final UserService userService;
    private final AdminDashboardService adminDashboardService;

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
    public String dashboard(Model model) {
        // model : 화면에서 사용할 데이터를 담는 객체
        model.addAttribute(
                "dashboard",
                adminDashboardService.getDashboard()
        );

        return "admin/dashboard";
    }

    // 메뉴 목록·검색·카테고리·페이지네이션
    @GetMapping("/menus")
    public String menus(
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category,
            @RequestParam(name = "page", defaultValue = "1") int page,
            HttpServletRequest request,
            Model model
    ) {
        String searchKeyword = keyword.trim();
        String searchCategory = category.trim();

        Page<MenuListResponseDto> menuPage =
                AdminPagination.loadPage(
                        page,
                        currentPage -> menuService.getMenusPage(
                                searchKeyword,
                                searchCategory,
                                currentPage
                        )
                );

        model.addAttribute("menus", menuPage.getContent());
        model.addAttribute("menuPage", menuPage);

        model.addAttribute("categories", codeService.getCategoryCodes());

        model.addAttribute("keyword", searchKeyword);
        model.addAttribute("category", searchCategory);

        model.addAttribute(
                "pagination",
                new AdminPagination(
                        menuPage,
                        request.getContextPath() + "/admin/menus",
                        Map.of(
                                "keyword", searchKeyword,
                                "category", searchCategory
                        )
                )
        );

        return "admin/menus/list";
    }
    // 메뉴 등록
    @GetMapping("/menus/form")
    public String menuForm() {
        return "admin/menus/form";
    }

    // 공통코드 목록 페이지
    @GetMapping("/codes")
    public String codes(Model model) {
        model.addAttribute("categoryCodes", adminCodeService.getCategoryCodes());
        model.addAttribute("tagCodes", adminCodeService.getTagCodes());
        return "admin/codes/list";
    }

    // 공통코드 등록/수정 API
    @PostMapping("/api/codes/save")
    @ResponseBody
    public ResponseEntity<String> saveCode(@RequestBody CodeSaveRequestDto requestDto) {
        try {
            adminCodeService.saveCode(requestDto);
            return ResponseEntity.ok("success");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("fail");
        }
    }

    // 공통코드 등록
    @GetMapping("/codes/form")
    public String codeForm() {
        return "admin/codes/form";
    }

    // 회원 목록
    @GetMapping("/users")
    public String users(
            @RequestParam(name = "searchType", defaultValue = "userId") String searchType,
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            HttpServletRequest request,
            Model model
    ) {
        String selectedSearchType = switch (searchType) {
            case "userName" -> "userName";
            case "email" -> "email";
            default -> "userId";
        };

        String searchKeyword = keyword.trim();

        //회원 리스트 가져오기
        Page<UserResponseDto> userPage =
                AdminPagination.loadPage(
                        page,
                        currentPage -> userService.getAllUsers(
                                selectedSearchType,
                                searchKeyword,
                                currentPage
                        )
                );

        // 모델에 담아서 타임리프 화면으로 전달
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("searchType", selectedSearchType);
        model.addAttribute("keyword", searchKeyword);

        model.addAttribute(
                "pagination",
                new AdminPagination(
                        userPage,
                        request.getContextPath() + "/admin/users",
                        Map.of(
                                "searchType", selectedSearchType,
                                "keyword", searchKeyword
                        )
                )
        );

        // templates/admin/users/list.html 반환
        return "admin/users/list";
    }
}