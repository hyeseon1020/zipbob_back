package com.zipbob.zipbob_backend.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;

import java.security.Principal;

@Controller
@RequestMapping("/admin")
// 관리자 URL에 맞는 HTML 화면
public class AdminPageController {

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
    // 메뉴 목록
    @GetMapping("/menus")
    public String menus() {
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
    public String users() {
        return "admin/users/list";
    }
}
