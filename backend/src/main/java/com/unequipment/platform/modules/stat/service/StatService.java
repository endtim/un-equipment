package com.unequipment.platform.modules.stat.service;

import com.unequipment.platform.modules.stat.entity.DailySnapshot;
import com.unequipment.platform.modules.stat.repository.DailySnapshotRepository;
import com.unequipment.platform.modules.stat.repository.StatQueryRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatService {

    private final DailySnapshotRepository dailySnapshotRepository;
    private final StatQueryRepository statQueryRepository;

    public List<Map<String, Object>> platformMembers() {
        return platformMembers(null);
    }

    /**
     * 平台成员统计：
     * 输出部门维度及其所属仪器数量，结果字段做统一归一化。
     */
    public List<Map<String, Object>> platformMembers(SysUser user) {
        String roleCode = roleCode(user);
        Long scopeDepartmentId = scopeDepartmentId(user);
        List<Map<String, Object>> rows = statQueryRepository.queryPlatformMembers(roleCode, scopeDepartmentId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : rows) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("departmentId", item.get("departmentId"));
            row.put("departmentName", item.get("departmentName"));
            row.put("instrumentCount", numberToLong(item.get("instrumentCount")));
            result.add(row);
        }
        return result;
    }

    public Map<String, Object> overview(LocalDateTime startTime, LocalDateTime endTime) {
        return overview(startTime, endTime, null);
    }

    /**
     * 统计总览统一口径：
     * - 所有指标按“时间范围 + 角色范围”过滤
     * - 指标异常时单项降级，不影响整体返回
     */
    public Map<String, Object> overview(LocalDateTime startTime, LocalDateTime endTime, SysUser user) {
        String roleCode = roleCode(user);
        Long scopeDepartmentId = scopeDepartmentId(user);
        // 统计总览口径：订单量、结算量、收入与客单价均按“时间范围 + 角色数据范围”过滤。
        long reservationCount = safeGet(
            () -> statQueryRepository.countOrders(startTime, endTime, roleCode, scopeDepartmentId), 0L
        );
        long completionCount = safeGet(
            () -> statQueryRepository.countOrdersByStatus("COMPLETED", startTime, endTime, roleCode, scopeDepartmentId), 0L
        );
        long waitingSettlementCount = safeGet(
            () -> statQueryRepository.countOrdersByStatus("WAITING_SETTLEMENT", startTime, endTime, roleCode, scopeDepartmentId), 0L
        );
        long settledCount = safeGet(
            () -> statQueryRepository.countSettlementsByStatus("CONFIRMED", startTime, endTime, roleCode, scopeDepartmentId), 0L
        );
        BigDecimal incomeAmount = nullSafe(safeGet(
            () -> statQueryRepository.sumSettlementAmountByStatus("CONFIRMED", startTime, endTime, roleCode, scopeDepartmentId),
            BigDecimal.ZERO
        ));
        BigDecimal averageOrderAmount = nullSafe(safeGet(
            () -> statQueryRepository.avgOrderFinalAmount(startTime, endTime, roleCode, scopeDepartmentId),
            BigDecimal.ZERO
        ))
            .setScale(2, RoundingMode.HALF_UP);

        Map<String, Object> result = new HashMap<>();
        result.put("instrumentCount", safeGet(statQueryRepository::countInstruments, 0L));
        result.put("reservationCount", reservationCount);
        result.put("completionCount", completionCount);
        result.put("departmentCount", safeGet(statQueryRepository::countDepartments, 0L));
        result.put("waitingSettlementCount", waitingSettlementCount);
        result.put("settledCount", settledCount);
        result.put("incomeAmount", incomeAmount);
        result.put("averageOrderAmount", averageOrderAmount);
        result.put("topInstruments", normalizeKeyValueList(safeGet(
            () -> statQueryRepository.queryTopInstruments(startTime, endTime, roleCode, scopeDepartmentId, 5),
            new ArrayList<>()
        )));
        result.put("departmentDistribution", toOrderedMap(safeGet(
            () -> statQueryRepository.queryDepartmentDistribution(startTime, endTime, roleCode, scopeDepartmentId),
            new ArrayList<>()
        )));
        result.put("orderTrend", buildTrend(startTime, endTime, roleCode, scopeDepartmentId));
        result.put("periodStart", startTime);
        result.put("periodEnd", endTime);
        return result;
    }

    /**
     * 趋势构建策略：
     * - 无时间筛选时优先读取日快照
     * - 有时间筛选时优先日聚合，缺失再回退月聚合
     */
    private Map<String, Long> buildTrend(LocalDateTime startTime, LocalDateTime endTime,
                                         String roleCode, Long scopeDepartmentId) {
        if (startTime == null && endTime == null) {
            // 无时间筛选时优先走快照，避免全量实时聚合带来性能波动。
            List<DailySnapshot> snapshots = safeGet(() -> dailySnapshotRepository.findLatestLimit(30), new ArrayList<>());
            if (snapshots != null && !snapshots.isEmpty()) {
                Map<String, Long> trend = new LinkedHashMap<>();
                List<DailySnapshot> ordered = new ArrayList<>(snapshots);
                ordered.sort((a, b) -> a.getStatDate().compareTo(b.getStatDate()));
                for (DailySnapshot item : ordered) {
                    long count = numberToLong(item.getMachineOrderTotal()) + numberToLong(item.getSampleOrderTotal());
                    trend.put(item.getStatDate().toString(), count);
                }
                return trend;
            }
        }

        // 有时间筛选时优先按日聚合，若没有数据再回退到按月聚合。
        List<Map<String, Object>> dailyRows = safeGet(
            () -> statQueryRepository.queryDailyTrend(startTime, endTime, roleCode, scopeDepartmentId),
            new ArrayList<>()
        );
        if (!dailyRows.isEmpty()) {
            return toOrderedMap(dailyRows);
        }

        List<Map<String, Object>> monthlyRows = safeGet(
            () -> statQueryRepository.queryMonthlyTrend(startTime, endTime, roleCode, scopeDepartmentId),
            new ArrayList<>()
        );
        return toOrderedMap(monthlyRows);
    }

    /**
     * 角色归一化：
     * 无登录上下文时按 ADMIN 口径统计（用于前台总览接口）。
     */
    private String roleCode(SysUser user) {
        if (user == null || user.getPrimaryRoleCode() == null) {
            return "ADMIN";
        }
        return user.getPrimaryRoleCode().toUpperCase();
    }

    /**
     * 角色范围辅助：部门管理员按 department_id 收敛范围。
     */
    private Long scopeDepartmentId(SysUser user) {
        if (user == null) {
            return null;
        }
        return user.getDepartmentId();
    }

    /**
     * 统计列表归一化为 key/value 结构，避免前端适配多种字段名。
     */
    private List<Map<String, Object>> normalizeKeyValueList(List<Map<String, Object>> rows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Map<String, Object> normalized = new LinkedHashMap<>();
            normalized.put("key", safeString(row.get("key"), "未知"));
            normalized.put("value", numberToLong(row.get("value")));
            result.add(normalized);
        }
        return result;
    }

    private Map<String, Long> toOrderedMap(List<Map<String, Object>> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String key = safeString(row.get("key"), "未知");
            result.put(key, numberToLong(row.get("value")));
        }
        return result;
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String safeString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? fallback : text;
    }

    private long numberToLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    /**
     * 统计接口降级器：
     * 任一指标查询失败时返回 fallback，保证总览接口稳定可用。
     */
    private <T> T safeGet(Supplier<T> supplier, T fallback) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (Exception ignored) {
            // 统计接口采用降级策略，单个指标异常不影响整体返回。
            return fallback;
        }
    }
}
