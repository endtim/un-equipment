package com.unequipment.platform.modules.stat.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.modules.stat.service.StatService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatController {

    private final StatService statService;
    private static final DateTimeFormatter ISO_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter SPACE_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 前台统计总览：
     * 支持可选时间区间，若时间为空则返回全局口径总览。
     */
    @GetMapping("/overview")
    public ApiResponse<?> overview(
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime) {
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);
        if (start != null && end != null && start.isAfter(end)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "开始时间不能晚于结束时间");
        }
        return ApiResponse.success(statService.overview(start, end));
    }

    /**
     * 前台平台成员列表（部门 + 仪器数）。
     */
    @GetMapping("/platform-members")
    public ApiResponse<?> platformMembers() {
        return ApiResponse.success(statService.platformMembers());
    }

    /**
     * 时间解析兼容两种输入格式：
     * 1) yyyy-MM-dd'T'HH:mm:ss
     * 2) yyyy-MM-dd HH:mm:ss
     */
    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        String text = value.trim();
        try {
            return LocalDateTime.parse(text, ISO_SECOND);
        } catch (DateTimeParseException ex) {
            try {
                return LocalDateTime.parse(text, SPACE_SECOND);
            } catch (DateTimeParseException ex2) {
                throw new BizException(ErrorCodes.INVALID_REQUEST, "时间格式错误，示例：2026-03-12 08:30:00");
            }
        }
    }
}
