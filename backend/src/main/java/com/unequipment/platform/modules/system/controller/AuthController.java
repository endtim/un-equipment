package com.unequipment.platform.modules.system.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.modules.system.dto.LoginRequest;
import com.unequipment.platform.modules.system.dto.RegisterRequest;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.modules.system.service.AuthService;
import com.unequipment.platform.security.CurrentUser;
import java.util.Map;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 登录入口：参数校验后交由 AuthService 完成鉴权与令牌生成。
     */
    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    /**
     * 注册入口：创建账号并返回登录态相关信息。
     */
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success(authService.register(request));
    }

    /**
     * 返回当前登录用户信息，用于前端刷新用户上下文（角色/部门等）。
     */
    @GetMapping("/info")
    public ApiResponse<AuthService.UserView> info(@CurrentUser SysUser user) {
        return ApiResponse.success(authService.info(user));
    }
}
