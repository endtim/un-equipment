package com.unequipment.platform.modules.stat.service;

import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import com.unequipment.platform.modules.finance.repository.SettlementRecordRepository;
import com.unequipment.platform.modules.instrument.repository.InstrumentRepository;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import com.unequipment.platform.modules.stat.entity.DailySnapshot;
import com.unequipment.platform.modules.stat.repository.DailySnapshotRepository;
import com.unequipment.platform.modules.system.repository.SysDepartmentRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatService {

    private final ReservationOrderRepository orderRepository;
    private final InstrumentRepository instrumentRepository;
    private final SysDepartmentRepository departmentRepository;
    private final SettlementRecordRepository settlementRecordRepository;
    private final DailySnapshotRepository dailySnapshotRepository;
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public Map<String, Object> overview(LocalDateTime startTime, LocalDateTime endTime) {
        List<ReservationOrder> orders = filterOrdersByTime(orderRepository.findAll(), startTime, endTime);
        List<SettlementRecord> settlements = filterSettlementsByTime(settlementRecordRepository.findAll(), startTime, endTime);
        Map<String, Object> result = new HashMap<>();
        result.put("instrumentCount", instrumentRepository.countAll());
        result.put("reservationCount", orders.size());
        result.put("completionCount", orders.stream().filter(item -> "COMPLETED".equals(item.getOrderStatus())).count());
        result.put("departmentCount", departmentRepository.findAll().size());
        result.put("waitingSettlementCount", orders.stream().filter(item -> "WAITING_SETTLEMENT".equals(item.getOrderStatus())).count());
        result.put("settledCount", settlements.stream().filter(item -> "CONFIRMED".equalsIgnoreCase(item.getSettleStatus())).count());

        BigDecimal income = settlements.stream()
            .filter(item -> "CONFIRMED".equalsIgnoreCase(item.getSettleStatus()))
            .map(SettlementRecord::getFinalAmount)
            .map(item -> item == null ? BigDecimal.ZERO : item)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("incomeAmount", income);
        result.put("averageOrderAmount", calculateAverageAmount(orders));

        result.put("topInstruments", orders.stream()
            .collect(Collectors.groupingBy(item -> defaultString(item.getInstrumentName(), "未命名仪器"), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .limit(5)
            .collect(Collectors.toList()));

        Map<Long, String> departmentNameMap = departmentRepository.findAll().stream()
            .collect(Collectors.toMap(item -> item.getId(), item -> item.getDeptName(), (left, right) -> left));
        Map<String, Long> distribution = new LinkedHashMap<>();
        orders.stream()
            .collect(Collectors.groupingBy(item -> departmentNameMap.getOrDefault(item.getDepartmentId(), "Unassigned"), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
            .forEach(entry -> distribution.put(entry.getKey(), entry.getValue()));
        result.put("departmentDistribution", distribution);

        result.put("orderTrend", buildTrend(orders, startTime, endTime));
        result.put("periodStart", startTime);
        result.put("periodEnd", endTime);
        return result;
    }

    private Map<String, Long> buildTrend(List<ReservationOrder> orders, LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null && endTime == null) {
            List<DailySnapshot> snapshots = dailySnapshotRepository.findLatestLimit(30);
            if (snapshots != null && !snapshots.isEmpty()) {
                Map<String, Long> trend = new LinkedHashMap<>();
                List<DailySnapshot> asc = new ArrayList<>(snapshots);
                asc.sort(Comparator.comparing(DailySnapshot::getStatDate));
                for (DailySnapshot item : asc) {
                    long count = (long) defaultInt(item.getMachineOrderTotal(), 0) + defaultInt(item.getSampleOrderTotal(), 0);
                    trend.put(item.getStatDate().format(DAY_FORMATTER), count);
                }
                return trend;
            }
        }

        Map<String, Long> dailyTrend = new LinkedHashMap<>();
        orders.stream()
            .filter(item -> item.getSubmitTime() != null)
            .collect(Collectors.groupingBy(item -> item.getSubmitTime().toLocalDate().format(DAY_FORMATTER), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> dailyTrend.put(entry.getKey(), entry.getValue()));

        if (!dailyTrend.isEmpty()) {
            return dailyTrend;
        }

        Map<String, Long> monthlyTrend = new LinkedHashMap<>();
        orders.stream()
            .filter(item -> item.getSubmitTime() != null)
            .collect(Collectors.groupingBy(item -> item.getSubmitTime().format(MONTH_FORMATTER), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> monthlyTrend.put(entry.getKey(), entry.getValue()));
        return monthlyTrend;
    }

    private List<ReservationOrder> filterOrdersByTime(List<ReservationOrder> all,
                                                      LocalDateTime startTime,
                                                      LocalDateTime endTime) {
        return all.stream()
            .filter(item -> item.getSubmitTime() != null)
            .filter(item -> startTime == null || !item.getSubmitTime().isBefore(startTime))
            .filter(item -> endTime == null || !item.getSubmitTime().isAfter(endTime))
            .collect(Collectors.toList());
    }

    private List<SettlementRecord> filterSettlementsByTime(List<SettlementRecord> all,
                                                           LocalDateTime startTime,
                                                           LocalDateTime endTime) {
        return all.stream()
            .filter(item -> {
                LocalDateTime ref = item.getCreateTime() == null ? item.getSettledTime() : item.getCreateTime();
                if (ref == null) {
                    return false;
                }
                if (startTime != null && ref.isBefore(startTime)) {
                    return false;
                }
                if (endTime != null && ref.isAfter(endTime)) {
                    return false;
                }
                return true;
            })
            .collect(Collectors.toList());
    }

    private BigDecimal calculateAverageAmount(List<ReservationOrder> orders) {
        if (orders == null || orders.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = orders.stream()
            .map(ReservationOrder::getFinalAmount)
            .map(this::nullSafe)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullSafe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private Integer defaultInt(Integer value, Integer fallback) {
        return value == null ? fallback : value;
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
