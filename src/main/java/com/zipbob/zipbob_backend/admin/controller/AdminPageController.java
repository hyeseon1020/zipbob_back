package com.zipbob.zipbob_backend.admin.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminPageController {

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard() {
        return "admin/dashboard";
    }

    @GetMapping("/menus")
    public String menus() {
        return "admin/menus/list";
    }

    @GetMapping("/menus/form")
    public String menuForm() {
        return "admin/menus/form";
    }

    @GetMapping("/codes")
    public String codes() {
        return "admin/codes/list";
    }

    @GetMapping("/codes/form")
    public String codeForm() {
        return "admin/codes/form";
    }

    @GetMapping("/users")
    public String users() {
        return "admin/users/list";
    }
}
