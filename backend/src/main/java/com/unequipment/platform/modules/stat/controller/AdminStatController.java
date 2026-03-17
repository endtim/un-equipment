package com.unequipment.platform.modules.stat.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.modules.stat.service.StatService;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stats")
@RequiredArgsConstructor
public class AdminStatController {

    private final StatService statService;
    private static final DateTimeFormatter ISO_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter SPACE_SECOND = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 管理端统计总览：
     * 支持按当前用户角色收敛数据范围（如部门管理员仅本部门）。
     */
    @GetMapping("/overview")
    public ApiResponse<?> overview(
        @CurrentUser SysUser user,
        @RequestParam(required = false) String startTime,
        @RequestParam(required = false) String endTime) {
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);
        if (start != null && end != null && start.isAfter(end)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "开始时间不能晚于结束时间");
        }
        return ApiResponse.success(statService.overview(start, end, user));
    }

    /**
     * 管理端平台成员列表：
     * ADMIN 可看全量，DEPT_MANAGER 仅看本部门。
     */
    @GetMapping("/platform-members")
    public ApiResponse<?> platformMembers(@CurrentUser SysUser user) {
        return ApiResponse.success(statService.platformMembers(user));
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
