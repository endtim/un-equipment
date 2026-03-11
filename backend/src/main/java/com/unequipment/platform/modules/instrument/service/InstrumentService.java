package com.unequipment.platform.modules.instrument.service;

import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.BizNoGenerator;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.instrument.dto.InstrumentSaveRequest;
import com.unequipment.platform.modules.instrument.dto.OpenRuleSaveRequest;
import com.unequipment.platform.modules.instrument.entity.Instrument;
import com.unequipment.platform.modules.instrument.entity.InstrumentAttachment;
import com.unequipment.platform.modules.instrument.entity.InstrumentCategory;
import com.unequipment.platform.modules.instrument.entity.InstrumentClosePeriod;
import com.unequipment.platform.modules.instrument.entity.InstrumentOpenRule;
import com.unequipment.platform.modules.instrument.entity.MaintenanceRecord;
import com.unequipment.platform.modules.instrument.repository.InstrumentAttachmentRepository;
import com.unequipment.platform.modules.instrument.repository.InstrumentCategoryRepository;
import com.unequipment.platform.modules.instrument.repository.InstrumentClosePeriodRepository;
import com.unequipment.platform.modules.instrument.repository.InstrumentOpenRuleRepository;
import com.unequipment.platform.modules.instrument.repository.InstrumentRepository;
import com.unequipment.platform.modules.instrument.repository.MaintenanceRecordRepository;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import com.unequipment.platform.modules.order.repository.SampleOrderRepository;
import com.unequipment.platform.modules.order.repository.UsageRecordRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstrumentService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentCategoryRepository categoryRepository;
    private final InstrumentOpenRuleRepository openRuleRepository;
    private final InstrumentAttachmentRepository attachmentRepository;
    private final InstrumentClosePeriodRepository closePeriodRepository;
    private final MaintenanceRecordRepository maintenanceRecordRepository;
    private final ReservationOrderRepository reservationOrderRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final SampleOrderRepository sampleOrderRepository;
    private final OperationLogService operationLogService;

    public PageResponse<Map<String, Object>> page(String keyword, Long categoryId, String status, int pageNum, int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        List<Map<String, Object>> list = instrumentRepository.findPageByCondition(keyword, status, categoryId, offset, pageSize)
            .stream()
            .map(this::toSummary)
            .collect(Collectors.toList());
        return new PageResponse<>(list, instrumentRepository.countPageByCondition(keyword, status, categoryId), pageNum, pageSize);
    }

    public Map<String, Object> detail(Long id) {
        Instrument instrument = getById(id);
        Map<String, Object> result = toSummary(instrument);
        result.put("description", instrument.getIntro());
        result.put("usageDesc", instrument.getUsageDesc());
        result.put("sampleDesc", instrument.getSampleDesc());
        result.put("openMode", instrument.getOpenMode());
        result.put("openStatus", instrument.getOpenStatus());
        result.put("supportExternal", instrument.getSupportExternal());
        result.put("needAudit", instrument.getNeedAudit());
        result.put("requireTraining", instrument.getRequireTraining());
        result.put("bookingUnit", instrument.getBookingUnit());
        result.put("minReserveMinutes", instrument.getMinReserveMinutes());
        result.put("maxReserveMinutes", instrument.getMaxReserveMinutes());
        result.put("stepMinutes", instrument.getStepMinutes());
        result.put("departmentName", instrument.getDepartmentName());
        result.put("ownerUserName", instrument.getOwnerUserName());
        result.put("noticeText", instrument.getNoticeText());
        result.put("model", instrument.getModel());
        result.put("brand", instrument.getBrand());
        result.put("assetNo", instrument.getAssetNo());
        result.put("manufacturer", instrument.getManufacturer());
        result.put("supplier", instrument.getSupplier());
        result.put("originCountry", instrument.getOriginCountry());
        result.put("purchaseDate", instrument.getPurchaseDate());
        result.put("productionDate", instrument.getProductionDate());
        result.put("equipmentSource", instrument.getEquipmentSource());
        result.put("serviceContactName", instrument.getServiceContactName());
        result.put("serviceContactPhone", instrument.getServiceContactPhone());
        result.put("technicalSpecs", instrument.getTechnicalSpecs());
        result.put("mainFunctions", instrument.getMainFunctions());
        result.put("serviceContent", instrument.getServiceContent());
        result.put("userNotice", instrument.getUserNotice());
        result.put("chargeStandard", instrument.getChargeStandard());
        result.put("metrics", buildMetrics(instrument.getId()));
        result.put("runtimeStatus", buildRuntimeStatus(instrument));
        result.put("attachments", attachmentRepository.findByInstrumentId(instrument.getId()));
        result.put("openRules", openRuleRepository.findByInstrumentId(instrument.getId()));
        return result;
    }

    public List<InstrumentCategory> categories() {
        return categoryRepository.findAll();
    }

    @Transactional
    public InstrumentCategory saveCategory(Long id, InstrumentCategory request) {
        InstrumentCategory category = id == null ? new InstrumentCategory() : categoryRepository.findById(id);
        if (id != null && category == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "category not found");
        }
        category.setParentId(request.getParentId());
        category.setCategoryName(request.getCategoryName());
        category.setCategoryCode(request.getCategoryCode());
        category.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        category.setStatus(request.getStatus() == null ? "ENABLED" : request.getStatus());
        category.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            category.setCreateTime(LocalDateTime.now());
            category.setDeleted(0);
            categoryRepository.insert(category);
        } else {
            categoryRepository.update(category);
        }
        operationLogService.save(null, "INSTRUMENT", id == null ? "CREATE_CATEGORY" : "UPDATE_CATEGORY", "category:" + category.getCategoryName());
        return category;
    }

    @Transactional
    public Instrument save(Long id, InstrumentSaveRequest request, SysUser operator) {
        Instrument instrument = id == null ? new Instrument() : getById(id);
        InstrumentCategory category = categoryRepository.findById(request.getCategoryId());
        if (category == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "category not found");
        }
        instrument.setInstrumentNo(request.getCode() == null ? BizNoGenerator.next("INS") : request.getCode());
        instrument.setInstrumentName(request.getName());
        instrument.setAssetNo(request.getAssetNo());
        instrument.setManufacturer(request.getManufacturer());
        instrument.setSupplier(request.getSupplier());
        instrument.setOriginCountry(request.getOriginCountry());
        instrument.setPurchaseDate(request.getPurchaseDate());
        instrument.setProductionDate(request.getProductionDate());
        instrument.setEquipmentSource(request.getEquipmentSource());
        instrument.setServiceContactName(request.getServiceContactName());
        instrument.setServiceContactPhone(request.getServiceContactPhone());
        instrument.setCategoryId(category.getId());
        instrument.setLocation(request.getLocation());
        instrument.setCoverUrl(request.getCoverImage());
        instrument.setIntro(request.getDescription());
        instrument.setUsageDesc(defaultString(request.getUsageDesc(), request.getDescription()));
        instrument.setSampleDesc(defaultString(request.getSampleDesc(), request.getDescription()));
        instrument.setNoticeText(request.getNoticeText());
        instrument.setTechnicalSpecs(request.getTechnicalSpecs());
        instrument.setMainFunctions(request.getMainFunctions());
        instrument.setServiceContent(request.getServiceContent());
        instrument.setUserNotice(request.getUserNotice());
        instrument.setChargeStandard(request.getChargeStandard());
        instrument.setPriceInternal(defaultValue(firstNonNull(request.getPriceInternal(), request.getMachinePricePerHour())));
        instrument.setPriceExternal(defaultValue(firstNonNull(request.getPriceExternal(), request.getSamplePricePerItem())));
        instrument.setStatus(request.getStatus() == null ? "NORMAL" : request.getStatus());
        if (id == null) {
            instrument.setDepartmentId(operator.getDepartmentId());
            instrument.setOwnerUserId(operator.getId());
            instrument.setOpenMode(defaultString(request.getOpenMode(), "BOTH"));
            instrument.setOpenStatus(defaultInt(request.getOpenStatus(), 1));
            instrument.setSupportExternal(defaultInt(request.getSupportExternal(), 1));
            instrument.setNeedAudit(defaultInt(request.getNeedAudit(), 1));
            instrument.setRequireTraining(defaultInt(request.getRequireTraining(), 0));
            instrument.setBookingUnit(defaultString(request.getBookingUnit(), "HOUR"));
            instrument.setMinReserveMinutes(defaultInt(request.getMinReserveMinutes(), 30));
            instrument.setMaxReserveMinutes(defaultInt(request.getMaxReserveMinutes(), 480));
            instrument.setStepMinutes(defaultInt(request.getStepMinutes(), 30));
            instrument.setIsHot(0);
            instrument.setSortNo(0);
        } else {
            instrument.setOpenMode(defaultString(request.getOpenMode(), instrument.getOpenMode()));
            instrument.setOpenStatus(defaultInt(request.getOpenStatus(), instrument.getOpenStatus()));
            instrument.setSupportExternal(defaultInt(request.getSupportExternal(), instrument.getSupportExternal()));
            instrument.setNeedAudit(defaultInt(request.getNeedAudit(), instrument.getNeedAudit()));
            instrument.setRequireTraining(defaultInt(request.getRequireTraining(), instrument.getRequireTraining()));
            instrument.setBookingUnit(defaultString(request.getBookingUnit(), instrument.getBookingUnit()));
            instrument.setMinReserveMinutes(defaultInt(request.getMinReserveMinutes(), instrument.getMinReserveMinutes()));
            instrument.setMaxReserveMinutes(defaultInt(request.getMaxReserveMinutes(), instrument.getMaxReserveMinutes()));
            instrument.setStepMinutes(defaultInt(request.getStepMinutes(), instrument.getStepMinutes()));
        }
        validateReserveRuleConfig(instrument.getMinReserveMinutes(), instrument.getMaxReserveMinutes(), instrument.getStepMinutes());
        if (id == null) {
            instrument.setCreateTime(LocalDateTime.now());
            instrument.setUpdateTime(LocalDateTime.now());
            instrument.setDeleted(0);
            instrumentRepository.insert(instrument);
        } else {
            instrument.setUpdateTime(LocalDateTime.now());
            instrumentRepository.update(instrument);
        }
        operationLogService.save(operator, "INSTRUMENT", id == null ? "CREATE_INSTRUMENT" : "UPDATE_INSTRUMENT", "instrument:" + instrument.getInstrumentName());
        return instrument;
    }

    @Transactional
    public InstrumentOpenRule saveRule(OpenRuleSaveRequest request) {
        return saveRule(null, request);
    }

    @Transactional
    public InstrumentOpenRule saveRule(Long id, OpenRuleSaveRequest request) {
        Instrument instrument = getById(request.getInstrumentId());
        InstrumentOpenRule rule = id == null ? new InstrumentOpenRule() : openRuleRepository.findById(id);
        if (id != null && rule == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "open rule not found");
        }
        Integer weekDay = resolveWeekDay(request);
        LocalTime startTime = resolveStartTime(request);
        LocalTime endTime = resolveEndTime(request);
        if (!endTime.isAfter(startTime)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "open time range is invalid");
        }
        if (request.getEffectiveStartDate() != null && request.getEffectiveEndDate() != null
            && request.getEffectiveEndDate().isBefore(request.getEffectiveStartDate())) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "effective date range is invalid");
        }
        rule.setInstrumentId(instrument.getId());
        rule.setWeekDay(weekDay);
        rule.setStartTime(startTime);
        rule.setEndTime(endTime);
        rule.setMaxReserveMinutes(request.getMaxReserveMinutes() == null ? 480 : request.getMaxReserveMinutes());
        rule.setStepMinutes(request.getStepMinutes() == null ? 30 : request.getStepMinutes());
        rule.setEffectiveStartDate(request.getEffectiveStartDate());
        rule.setEffectiveEndDate(request.getEffectiveEndDate());
        rule.setStatus("ENABLED");
        rule.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            rule.setCreateTime(LocalDateTime.now());
            rule.setDeleted(0);
            openRuleRepository.insert(rule);
        } else {
            openRuleRepository.update(rule);
        }
        operationLogService.save(null, "INSTRUMENT", id == null ? "CREATE_OPEN_RULE" : "UPDATE_OPEN_RULE", "ruleId:" + rule.getId());
        return rule;
    }

    public void ensureReservable(Instrument instrument) {
        if (!"NORMAL".equalsIgnoreCase(instrument.getStatus()) || instrument.getOpenStatus() == null || instrument.getOpenStatus() != 1) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "instrument is not available");
        }
    }

    public void ensureOrderTypeSupported(Instrument instrument, String orderType, SysUser user) {
        String openMode = instrument.getOpenMode() == null ? "BOTH" : instrument.getOpenMode();
        if ("MACHINE".equalsIgnoreCase(orderType) && "SAMPLE".equalsIgnoreCase(openMode)) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "instrument does not support machine reservation");
        }
        if ("SAMPLE".equalsIgnoreCase(orderType) && "MACHINE".equalsIgnoreCase(openMode)) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "instrument does not support sample reservation");
        }
        if (user != null && "EXTERNAL".equalsIgnoreCase(user.getUserType())
            && (instrument.getSupportExternal() == null || instrument.getSupportExternal() != 1)) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "instrument does not support external users");
        }
    }

    public void validateMachineReserveWindow(Instrument instrument, LocalDateTime reserveStart, LocalDateTime reserveEnd) {
        if (reserveStart == null || reserveEnd == null || !reserveEnd.isAfter(reserveStart)) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "reserved time range is invalid");
        }
        if (!reserveStart.toLocalDate().equals(reserveEnd.toLocalDate())) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "cross-day reservation is not supported");
        }
        int reserveMinutes = (int) java.time.Duration.between(reserveStart, reserveEnd).toMinutes();
        Integer minMinutes = defaultInt(instrument.getMinReserveMinutes(), 30);
        Integer maxMinutes = defaultInt(instrument.getMaxReserveMinutes(), 480);
        Integer stepMinutes = defaultInt(instrument.getStepMinutes(), 30);
        validateReserveRuleConfig(minMinutes, maxMinutes, stepMinutes);
        if (reserveMinutes < minMinutes || reserveMinutes > maxMinutes) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "reserved duration exceeds limit");
        }
        if (reserveMinutes % stepMinutes != 0) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "reserved duration does not match step minutes");
        }

        List<InstrumentOpenRule> rules = openRuleRepository.findByInstrumentId(instrument.getId()).stream()
            .filter(rule -> "ENABLED".equalsIgnoreCase(rule.getStatus()))
            .collect(Collectors.toList());
        if (rules.isEmpty()) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "instrument open rule is not configured");
        }
        LocalDate reserveDate = reserveStart.toLocalDate();
        int weekDay = reserveDate.getDayOfWeek().getValue();
        LocalTime startTime = reserveStart.toLocalTime();
        LocalTime endTime = reserveEnd.toLocalTime();
        boolean matched = rules.stream().anyMatch(rule ->
            rule.getWeekDay() != null && rule.getWeekDay() == weekDay
                && rule.getStartTime() != null && rule.getEndTime() != null
                && isDateInEffectiveRange(reserveDate, rule.getEffectiveStartDate(), rule.getEffectiveEndDate())
                && !startTime.isBefore(rule.getStartTime())
                && !endTime.isAfter(rule.getEndTime())
                && reserveMinutes <= defaultInt(rule.getMaxReserveMinutes(), maxMinutes)
                && reserveMinutes % defaultInt(rule.getStepMinutes(), stepMinutes) == 0
        );
        if (!matched) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "reserved time does not match open rules");
        }
    }

    public Instrument getById(Long id) {
        Instrument instrument = instrumentRepository.findById(id);
        if (instrument == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "instrument not found");
        }
        return instrument;
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (categoryRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "category not found");
        }
        if (instrumentRepository.countByCategoryId(id) > 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "category is referenced by instruments");
        }
        categoryRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(null, "INSTRUMENT", "DELETE_CATEGORY", "categoryId:" + id);
    }

    @Transactional
    public void deleteInstrument(Long id) {
        if (instrumentRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "instrument not found");
        }
        if (reservationOrderRepository.countByInstrumentId(id) > 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "instrument has reservation history and cannot be deleted");
        }
        instrumentRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(null, "INSTRUMENT", "DELETE_INSTRUMENT", "instrumentId:" + id);
    }

    public List<InstrumentOpenRule> allOpenRules() {
        return openRuleRepository.findAll();
    }

    public List<InstrumentAttachment> allAttachments() {
        return attachmentRepository.findAll();
    }

    @Transactional
    public InstrumentAttachment saveAttachment(Long id, InstrumentAttachment request) {
        getById(request.getInstrumentId());
        InstrumentAttachment attachment = id == null ? new InstrumentAttachment() : attachmentRepository.findById(id);
        if (id != null && attachment == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "attachment not found");
        }
        attachment.setInstrumentId(request.getInstrumentId());
        attachment.setFileName(request.getFileName());
        attachment.setFileUrl(request.getFileUrl());
        attachment.setFileType(request.getFileType() == null ? "FILE" : request.getFileType());
        attachment.setSortNo(request.getSortNo() == null ? 0 : request.getSortNo());
        attachment.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            attachment.setCreateTime(LocalDateTime.now());
            attachment.setDeleted(0);
            attachmentRepository.insert(attachment);
        } else {
            attachmentRepository.update(attachment);
        }
        operationLogService.save(null, "INSTRUMENT", id == null ? "CREATE_ATTACHMENT" : "UPDATE_ATTACHMENT", "attachmentId:" + attachment.getId());
        return attachment;
    }

    @Transactional
    public void deleteOpenRule(Long id) {
        if (openRuleRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "open rule not found");
        }
        openRuleRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(null, "INSTRUMENT", "DELETE_OPEN_RULE", "ruleId:" + id);
    }

    @Transactional
    public void deleteAttachment(Long id) {
        if (attachmentRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "attachment not found");
        }
        attachmentRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(null, "INSTRUMENT", "DELETE_ATTACHMENT", "attachmentId:" + id);
    }

    private Map<String, Object> toSummary(Instrument instrument) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", instrument.getId());
        result.put("name", instrument.getInstrumentName());
        result.put("instrumentName", instrument.getInstrumentName());
        result.put("code", instrument.getInstrumentNo());
        result.put("instrumentNo", instrument.getInstrumentNo());
        result.put("assetNo", instrument.getAssetNo());
        result.put("manufacturer", instrument.getManufacturer());
        result.put("supplier", instrument.getSupplier());
        result.put("originCountry", instrument.getOriginCountry());
        result.put("purchaseDate", instrument.getPurchaseDate());
        result.put("productionDate", instrument.getProductionDate());
        result.put("equipmentSource", instrument.getEquipmentSource());
        result.put("serviceContactName", instrument.getServiceContactName());
        result.put("serviceContactPhone", instrument.getServiceContactPhone());
        result.put("categoryId", instrument.getCategoryId());
        result.put("categoryName", instrument.getCategoryName());
        result.put("status", instrument.getStatus());
        result.put("location", instrument.getLocation());
        result.put("coverImage", instrument.getCoverUrl());
        result.put("coverUrl", instrument.getCoverUrl());
        result.put("description", instrument.getIntro());
        result.put("intro", instrument.getIntro());
        result.put("usageDesc", instrument.getUsageDesc());
        result.put("sampleDesc", instrument.getSampleDesc());
        result.put("noticeText", instrument.getNoticeText());
        result.put("technicalSpecs", instrument.getTechnicalSpecs());
        result.put("mainFunctions", instrument.getMainFunctions());
        result.put("serviceContent", instrument.getServiceContent());
        result.put("userNotice", instrument.getUserNotice());
        result.put("chargeStandard", instrument.getChargeStandard());
        result.put("departmentName", instrument.getDepartmentName());
        result.put("departmentId", instrument.getDepartmentId());
        result.put("ownerUserName", instrument.getOwnerUserName());
        result.put("ownerUserId", instrument.getOwnerUserId());
        result.put("openMode", instrument.getOpenMode());
        result.put("openStatus", instrument.getOpenStatus());
        result.put("supportExternal", instrument.getSupportExternal());
        result.put("needAudit", instrument.getNeedAudit());
        result.put("requireTraining", instrument.getRequireTraining());
        result.put("bookingUnit", instrument.getBookingUnit());
        result.put("minReserveMinutes", instrument.getMinReserveMinutes());
        result.put("maxReserveMinutes", instrument.getMaxReserveMinutes());
        result.put("stepMinutes", instrument.getStepMinutes());
        result.put("priceInternal", instrument.getPriceInternal());
        result.put("priceExternal", instrument.getPriceExternal());
        result.put("machinePricePerHour", instrument.getPriceInternal());
        result.put("samplePricePerItem", instrument.getPriceExternal());
        return result;
    }

    private BigDecimal defaultValue(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal firstNonNull(BigDecimal primary, BigDecimal compatible) {
        return primary != null ? primary : compatible;
    }

    private String defaultString(String value, String defaultValue) {
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private Integer defaultInt(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    private void validateReserveRuleConfig(Integer minReserveMinutes, Integer maxReserveMinutes, Integer stepMinutes) {
        if (minReserveMinutes == null || maxReserveMinutes == null || stepMinutes == null) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "reserve rule config is invalid");
        }
        if (minReserveMinutes <= 0 || maxReserveMinutes <= 0 || stepMinutes <= 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "reserve rule config must be positive");
        }
        if (maxReserveMinutes < minReserveMinutes) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "maxReserveMinutes must be >= minReserveMinutes");
        }
        if (minReserveMinutes % stepMinutes != 0 || maxReserveMinutes % stepMinutes != 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "reserve minutes must align with stepMinutes");
        }
    }

    private boolean isDateInEffectiveRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    private Map<String, Object> buildMetrics(Long instrumentId) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("reserveCount", reservationOrderRepository.countByInstrumentId(instrumentId));
        metrics.put("reserveUserCount", reservationOrderRepository.countDistinctUsersByInstrumentId(instrumentId));
        metrics.put("totalUsageMinutes", usageRecordRepository.sumActualMinutesByInstrumentId(instrumentId));
        metrics.put("sampleCount", sampleOrderRepository.sumSampleCountByInstrumentId(instrumentId));
        return metrics;
    }

    private Map<String, Object> buildRuntimeStatus(Instrument instrument) {
        Map<String, Object> runtime = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();
        String reason = null;
        LocalDateTime recoverTime = null;
        String source = null;

        List<InstrumentClosePeriod> closePeriods = closePeriodRepository.findByInstrumentId(instrument.getId());
        InstrumentClosePeriod activeClose = closePeriods.stream()
            .filter(item -> item.getCloseStart() != null && item.getCloseEnd() != null
                && !now.isBefore(item.getCloseStart()) && now.isBefore(item.getCloseEnd()))
            .findFirst()
            .orElse(null);
        if (activeClose != null) {
            reason = defaultString(activeClose.getReason(), activeClose.getCloseType());
            recoverTime = activeClose.getCloseEnd();
            source = "CLOSE_PERIOD";
        }

        if (reason == null) {
            List<MaintenanceRecord> maintenanceRecords = maintenanceRecordRepository.findByInstrumentId(instrument.getId());
            MaintenanceRecord activeMaintenance = maintenanceRecords.stream()
                .filter(item -> item.getStartTime() != null
                    && !now.isBefore(item.getStartTime())
                    && (item.getEndTime() == null || now.isBefore(item.getEndTime()))
                    && !"FINISHED".equalsIgnoreCase(item.getStatus()))
                .findFirst()
                .orElse(null);
            if (activeMaintenance != null) {
                reason = defaultString(activeMaintenance.getTitle(), activeMaintenance.getMaintType());
                recoverTime = activeMaintenance.getEndTime();
                source = "MAINTENANCE";
            }
        }

        if (reason == null && !"NORMAL".equalsIgnoreCase(instrument.getStatus())) {
            reason = "仪器状态为" + instrument.getStatus();
            source = "INSTRUMENT_STATUS";
        }
        if (reason == null && (instrument.getOpenStatus() == null || instrument.getOpenStatus() != 1)) {
            reason = "当前未开放预约";
            source = "OPEN_STATUS";
        }

        runtime.put("available", reason == null);
        runtime.put("reason", reason);
        runtime.put("recoverTime", recoverTime);
        runtime.put("source", source);
        return runtime;
    }

    private Integer resolveWeekDay(OpenRuleSaveRequest request) {
        if (request.getWeekDay() != null) {
            return request.getWeekDay();
        }
        if (request.getOpenDays() != null && !request.getOpenDays().trim().isEmpty()) {
            return Integer.parseInt(request.getOpenDays().trim());
        }
        throw new BizException(ErrorCodes.INVALID_REQUEST, "week day is required");
    }

    private LocalTime resolveStartTime(OpenRuleSaveRequest request) {
        if (request.getStartTime() != null && !request.getStartTime().trim().isEmpty()) {
            return LocalTime.parse(request.getStartTime().trim());
        }
        if (request.getOpenTimeRange() != null && request.getOpenTimeRange().contains("-")) {
            return LocalTime.parse(request.getOpenTimeRange().split("-")[0].trim());
        }
        throw new BizException(ErrorCodes.INVALID_REQUEST, "startTime is required");
    }

    private LocalTime resolveEndTime(OpenRuleSaveRequest request) {
        if (request.getEndTime() != null && !request.getEndTime().trim().isEmpty()) {
            return LocalTime.parse(request.getEndTime().trim());
        }
        if (request.getOpenTimeRange() != null && request.getOpenTimeRange().contains("-")) {
            return LocalTime.parse(request.getOpenTimeRange().split("-")[1].trim());
        }
        throw new BizException(ErrorCodes.INVALID_REQUEST, "endTime is required");
    }
}
