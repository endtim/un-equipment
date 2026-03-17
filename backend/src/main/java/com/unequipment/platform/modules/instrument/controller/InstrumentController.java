package com.unequipment.platform.modules.instrument.controller;

import com.unequipment.platform.common.api.ApiResponse;
import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.modules.instrument.service.InstrumentService;
import com.unequipment.platform.modules.order.service.OrderService;
import com.unequipment.platform.modules.order.vo.ReservedSlotVO;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instruments")
@RequiredArgsConstructor
public class InstrumentController {

    private final InstrumentService instrumentService;
    private final OrderService orderService;

    /**
     * 前台仪器分页列表。
     */
    @GetMapping
    public ApiResponse<PageResponse<Map<String, Object>>> page(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "1") int pageNum,
        @RequestParam(defaultValue = "10") int pageSize) {
        return ApiResponse.success(instrumentService.page(keyword, categoryId, status, pageNum, pageSize));
    }

    /**
     * 前台仪器详情（包含开放规则与运行态等聚合信息）。
     */
    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable Long id) {
        return ApiResponse.success(instrumentService.detail(id));
    }

    /**
     * 指定日期已占用时段查询（用于预约表单避让）。
     */
    @GetMapping("/{id}/reserved-slots")
    public ApiResponse<List<ReservedSlotVO>> reservedSlots(@PathVariable Long id,
                                                           @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate date) {
        return ApiResponse.success(orderService.listMachineReservedSlots(id, date));
    }

    /**
     * 仪器分类列表（前台筛选项）。
     */
    @GetMapping("/categories")
    public ApiResponse<?> categories() {
        return ApiResponse.success(instrumentService.categories());
    }
}
