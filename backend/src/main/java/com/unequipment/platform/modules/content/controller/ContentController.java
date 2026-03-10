package com.unequipment.platform.modules.content.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.modules.content.service.ContentService;
import com.unequipment.platform.modules.content.service.MessageService;
import com.unequipment.platform.modules.system.entity.SysUser;
import com.unequipment.platform.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContentController {

    private final ContentService contentService;
    private final MessageService messageService;

    @GetMapping("/notices")
    public ApiResponse<?> notices() {
        return ApiResponse.success(contentService.notices());
    }

    @GetMapping("/notices/{id}")
    public ApiResponse<?> notice(@PathVariable Long id) {
        return ApiResponse.success(contentService.notice(id));
    }

    @GetMapping("/help-docs")
    public ApiResponse<?> helpDocs() {
        return ApiResponse.success(contentService.helpDocs());
    }

    @GetMapping("/help-docs/{id}")
    public ApiResponse<?> helpDoc(@PathVariable Long id) {
        return ApiResponse.success(contentService.helpDoc(id));
    }

    @GetMapping("/messages")
    public ApiResponse<?> messages(@CurrentUser SysUser user) {
        return ApiResponse.success(messageService.findByUser(user));
    }

    @PostMapping("/messages/{id}/read")
    public ApiResponse<?> read(@PathVariable Long id, @CurrentUser SysUser user) {
        messageService.markRead(user, id);
        return ApiResponse.success("ok");
    }

    @PostMapping("/messages/read-all")
    public ApiResponse<?> readAll(@CurrentUser SysUser user) {
        messageService.markAllRead(user);
        return ApiResponse.success("ok");
    }
}
