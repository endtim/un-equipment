package com.unequipment.platform.modules.log.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.log.repository.OperationLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/logs")
@RequiredArgsConstructor
public class LogController {

    private final OperationLogRepository operationLogRepository;

    @GetMapping
    public ApiResponse<?> logs(
        @RequestParam(required = false) String moduleName,
        @RequestParam(required = false) String keyword,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        return ApiResponse.success(new PageResponse<>(
            operationLogRepository.findPage(moduleName, keyword, offset, pageSize),
            operationLogRepository.countPage(moduleName, keyword),
            pageNum,
            pageSize
        ));
    }
}
