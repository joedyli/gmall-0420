package com.atguigu.gmall.auth.controller;

import com.atguigu.gmall.auth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @GetMapping("toLogin.html")
    public String toLogin(@RequestParam(value = "returnUrl", required = false)String returnUrl, Model model){
        model.addAttribute("returnUrl", returnUrl);
        return "login";
    }

    @PostMapping("login")
    public String login(@RequestParam("loginName")String loginName, @RequestParam("password")String password,
                        @RequestParam(value = "returnUrl", required = false)String returnUrl,
                        HttpServletRequest request, HttpServletResponse response){
        this.authService.login(loginName, password, request, response);

        return "redirect:" + returnUrl;
    }
}
