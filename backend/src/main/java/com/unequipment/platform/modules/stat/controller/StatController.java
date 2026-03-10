package com.unequipment.platform.modules.stat.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.modules.stat.service.StatService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;

    @GetMapping("/overview")
    public ApiResponse<?> overview() {
        return ApiResponse.success(statService.overview());
    }
}
