package com.unequipment.platform.modules.stat.service;

import com.unequipment.platform.modules.finance.entity.SettlementRecord;
import com.unequipment.platform.modules.finance.repository.SettlementRecordRepository;
import com.unequipment.platform.modules.instrument.repository.InstrumentRepository;
import com.unequipment.platform.modules.order.entity.ReservationOrder;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import com.unequipment.platform.modules.system.repository.SysDepartmentRepository;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    public Map<String, Object> overview() {
        List<ReservationOrder> orders = orderRepository.findAll();
        Map<String, Object> result = new HashMap<>();
        result.put("instrumentCount", instrumentRepository.countAll());
        result.put("reservationCount", orders.size());
        result.put("completionCount", orders.stream().filter(item -> "COMPLETED".equals(item.getOrderStatus())).count());
        result.put("departmentCount", departmentRepository.findAll().size());

        BigDecimal income = settlementRecordRepository.findAll().stream()
            .map(SettlementRecord::getFinalAmount)
            .map(item -> item == null ? BigDecimal.ZERO : item)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        result.put("incomeAmount", income);

        result.put("topInstruments", orders.stream()
            .collect(Collectors.groupingBy(ReservationOrder::getInstrumentName, Collectors.counting()))
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

        Map<String, Long> orderTrend = new LinkedHashMap<>();
        orders.stream()
            .filter(item -> item.getSubmitTime() != null)
            .collect(Collectors.groupingBy(item -> item.getSubmitTime().format(MONTH_FORMATTER), Collectors.counting()))
            .entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> orderTrend.put(entry.getKey(), entry.getValue()));
        result.put("orderTrend", orderTrend);
        return result;
    }
}
