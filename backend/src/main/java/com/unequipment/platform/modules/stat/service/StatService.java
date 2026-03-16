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

    public Map<String, Object> overview(LocalDateTime startTime, LocalDateTime endTime, SysUser user) {
        String roleCode = roleCode(user);
        Long scopeDepartmentId = scopeDepartmentId(user);
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

    private Map<String, Long> buildTrend(LocalDateTime startTime, LocalDateTime endTime,
                                         String roleCode, Long scopeDepartmentId) {
        if (startTime == null && endTime == null) {
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

    private String roleCode(SysUser user) {
        if (user == null || user.getPrimaryRoleCode() == null) {
            return "ADMIN";
        }
        return user.getPrimaryRoleCode().toUpperCase();
    }

    private Long scopeDepartmentId(SysUser user) {
        if (user == null) {
            return null;
        }
        return user.getDepartmentId();
    }

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

    private <T> T safeGet(Supplier<T> supplier, T fallback) {
        try {
            T value = supplier.get();
            return value == null ? fallback : value;
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
