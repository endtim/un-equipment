package com.unequipment.platform.modules.instrument.service;

import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.common.exception.ErrorCodes;
import com.unequipment.platform.common.util.BizNoGenerator;
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
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.order.repository.ReservationOrderRepository;
import com.unequipment.platform.modules.order.repository.SampleOrderRepository;
import com.unequipment.platform.modules.order.repository.UsageRecordRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstrumentService {
    private static final Set<String> INSTRUMENT_STATUS_SET = new HashSet<>(Arrays.asList(
        "NORMAL", "DISABLED", "MAINTENANCE", "FAULT"
    ));
    private static final Set<String> OPEN_MODE_SET = new HashSet<>(Arrays.asList(
        "MACHINE", "SAMPLE", "BOTH"
    ));
    private static final Set<String> BOOKING_UNIT_SET = new HashSet<>(Arrays.asList(
        "HOUR", "ITEM"
    ));

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

    public PageResponse<Map<String, Object>> page(String keyword, Long categoryId, String status,
                                                  int pageNum, int pageSize, SysUser operator) {
        assertAdminOrInstrumentManager(operator);
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        String roleCode = operator == null ? "INTERNAL_USER" : defaultString(operator.getPrimaryRoleCode(), "INTERNAL_USER");
        List<Instrument> scopedList;
        long total;
        if ("ADMIN".equalsIgnoreCase(roleCode)) {
            scopedList = instrumentRepository.findPageByCondition(keyword, status, categoryId, offset, pageSize);
            total = instrumentRepository.countPageByCondition(keyword, status, categoryId);
        } else {
            scopedList = instrumentRepository.findPageByScope(keyword, status, categoryId, offset, pageSize,
                roleCode.toUpperCase(), operator.getId(), operator.getDepartmentId());
            total = instrumentRepository.countPageByScope(keyword, status, categoryId,
                roleCode.toUpperCase(), operator.getId(), operator.getDepartmentId());
        }
        List<Map<String, Object>> list = scopedList
            .stream()
            .map(this::toSummary)
            .collect(Collectors.toList());
        return new PageResponse<>(list, total, pageNum, pageSize);
    }

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
        result.put("metrics", buildMetrics(instrument.getId()));
        result.put("runtimeStatus", buildRuntimeStatus(instrument));
        result.put("attachments", attachmentRepository.findByInstrumentId(instrument.getId()));
        result.put("openRules", openRuleRepository.findByInstrumentId(instrument.getId()));
        return result;
    }

    public List<InstrumentCategory> categories() {
        return categoryRepository.findAll();
    }

    public PageResponse<InstrumentCategory> pageCategories(int pageNum, int pageSize, SysUser operator) {
        assertAdminOrInstrumentManager(operator);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        int offset = (safePageNum - 1) * safePageSize;
        List<InstrumentCategory> list = categoryRepository.findPage(offset, safePageSize);
        long total = categoryRepository.countPage();
        return new PageResponse<>(list, total, safePageNum, safePageSize);
    }

    @Transactional
    public InstrumentCategory saveCategory(Long id, InstrumentCategory request, SysUser operator) {
        assertAdmin(operator);
        InstrumentCategory category = id == null ? new InstrumentCategory() : categoryRepository.findById(id);
        if (id != null && category == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "仪器分类不存在");
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
        operationLogService.save(operator, "INSTRUMENT", id == null ? "CREATE_CATEGORY" : "UPDATE_CATEGORY", "category:" + category.getCategoryName());
        return category;
    }

    @Transactional
    public Instrument save(Long id, InstrumentSaveRequest request, SysUser operator) {
        Instrument instrument = id == null ? new Instrument() : getById(id);
        InstrumentCategory category = categoryRepository.findById(request.getCategoryId());
        if (category == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "仪器分类不存在");
        }
        if (id != null) {
            assertInstrumentManagePermission(instrument, operator);
        }
        instrument.setInstrumentNo(request.getCode() == null ? BizNoGenerator.next("INS") : request.getCode());
        instrument.setInstrumentName(request.getName());
        instrument.setModel(request.getModel());
        instrument.setBrand(request.getBrand());
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
        instrument.setStatus(validateInstrumentStatus(defaultString(request.getStatus(), "NORMAL")));

        if (id == null) {
            instrument.setDepartmentId(operator.getDepartmentId());
            instrument.setOwnerUserId(operator.getId());
            instrument.setOpenMode(validateOpenMode(defaultString(request.getOpenMode(), "BOTH")));
            instrument.setOpenStatus(defaultInt(request.getOpenStatus(), 1));
            instrument.setSupportExternal(defaultInt(request.getSupportExternal(), 1));
            instrument.setNeedAudit(defaultInt(request.getNeedAudit(), 1));
            instrument.setRequireTraining(defaultInt(request.getRequireTraining(), 0));
            instrument.setBookingUnit(validateBookingUnit(defaultString(request.getBookingUnit(), "HOUR")));
            instrument.setMinReserveMinutes(defaultInt(request.getMinReserveMinutes(), 30));
            instrument.setMaxReserveMinutes(defaultInt(request.getMaxReserveMinutes(), 480));
            instrument.setStepMinutes(defaultInt(request.getStepMinutes(), 30));
            instrument.setIsHot(0);
            instrument.setSortNo(0);
            instrument.setCreateTime(LocalDateTime.now());
            instrument.setDeleted(0);
        } else {
            instrument.setOpenMode(validateOpenMode(defaultString(request.getOpenMode(), instrument.getOpenMode())));
            instrument.setOpenStatus(defaultInt(request.getOpenStatus(), instrument.getOpenStatus()));
            instrument.setSupportExternal(defaultInt(request.getSupportExternal(), instrument.getSupportExternal()));
            instrument.setNeedAudit(defaultInt(request.getNeedAudit(), instrument.getNeedAudit()));
            instrument.setRequireTraining(defaultInt(request.getRequireTraining(), instrument.getRequireTraining()));
            instrument.setBookingUnit(validateBookingUnit(defaultString(request.getBookingUnit(), instrument.getBookingUnit())));
            instrument.setMinReserveMinutes(defaultInt(request.getMinReserveMinutes(), instrument.getMinReserveMinutes()));
            instrument.setMaxReserveMinutes(defaultInt(request.getMaxReserveMinutes(), instrument.getMaxReserveMinutes()));
            instrument.setStepMinutes(defaultInt(request.getStepMinutes(), instrument.getStepMinutes()));
        }

        validateReserveRuleConfig(instrument.getMinReserveMinutes(), instrument.getMaxReserveMinutes(), instrument.getStepMinutes());
        instrument.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            instrumentRepository.insert(instrument);
        } else {
            instrumentRepository.update(instrument);
        }
        operationLogService.save(operator, "INSTRUMENT", id == null ? "CREATE_INSTRUMENT" : "UPDATE_INSTRUMENT", "instrument:" + instrument.getInstrumentName());
        return instrument;
    }

    @Transactional
    public InstrumentOpenRule saveRule(OpenRuleSaveRequest request, SysUser operator) {
        return saveRule(null, request, operator);
    }

    @Transactional
    public InstrumentOpenRule saveRule(Long id, OpenRuleSaveRequest request, SysUser operator) {
        Instrument instrument = getById(request.getInstrumentId());
        assertInstrumentManagePermission(instrument, operator);
        InstrumentOpenRule rule = id == null ? new InstrumentOpenRule() : openRuleRepository.findById(id);
        if (id != null && rule == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "开放规则不存在");
        }
        if (id != null && (rule.getInstrumentId() == null || !rule.getInstrumentId().equals(request.getInstrumentId()))) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "已有开放规则不允许修改仪器");
        }
        List<Integer> weekDays = resolveWeekDays(request);
        LocalTime startTime = resolveStartTime(request);
        LocalTime endTime = resolveEndTime(request);
        if (!endTime.isAfter(startTime)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "开放时间范围不合法");
        }
        Integer stepMinutes = defaultInt(request.getStepMinutes(), 30);
        Integer maxReserveMinutes = defaultInt(request.getMaxReserveMinutes(), 480);
        validateReserveRuleConfig(stepMinutes, maxReserveMinutes, stepMinutes);
        if (maxReserveMinutes % stepMinutes != 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "开放规则中最长预约时长必须与时间步长对齐");
        }
        if (request.getEffectiveStartDate() != null && request.getEffectiveEndDate() != null
            && request.getEffectiveEndDate().isBefore(request.getEffectiveStartDate())) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "生效日期范围不合法");
        }
        assertOpenRuleNoOverlap(id, instrument.getId(), weekDays, startTime, endTime,
            request.getEffectiveStartDate(), request.getEffectiveEndDate());

        rule.setInstrumentId(instrument.getId());
        rule.setWeekDay(weekDays.get(0));
        rule.setWeekDays(joinWeekDays(weekDays));
        rule.setStartTime(startTime);
        rule.setEndTime(endTime);
        rule.setMaxReserveMinutes(maxReserveMinutes);
        rule.setStepMinutes(stepMinutes);
        rule.setEffectiveStartDate(request.getEffectiveStartDate());
        rule.setEffectiveEndDate(request.getEffectiveEndDate());
        rule.setStatus(defaultString(request.getStatus(), "ENABLED"));
        rule.setUpdateTime(LocalDateTime.now());
        if (id == null) {
            rule.setCreateTime(LocalDateTime.now());
            rule.setDeleted(0);
            openRuleRepository.insert(rule);
        } else {
            openRuleRepository.update(rule);
        }
        operationLogService.save(operator, "INSTRUMENT", id == null ? "CREATE_OPEN_RULE" : "UPDATE_OPEN_RULE", "ruleId:" + rule.getId());
        return rule;
    }

    public void ensureReservable(Instrument instrument) {
        if (!"NORMAL".equalsIgnoreCase(instrument.getStatus()) || instrument.getOpenStatus() == null || instrument.getOpenStatus() != 1) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "当前仪器不可预约");
        }
    }

    public void ensureOrderTypeSupported(Instrument instrument, String orderType, SysUser user) {
        String openMode = instrument.getOpenMode() == null ? "BOTH" : instrument.getOpenMode();
        if ("MACHINE".equalsIgnoreCase(orderType) && "SAMPLE".equalsIgnoreCase(openMode)) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "该仪器不支持上机预约");
        }
        if ("SAMPLE".equalsIgnoreCase(orderType) && "MACHINE".equalsIgnoreCase(openMode)) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "该仪器不支持送样预约");
        }
        if (user != null && isExternalUser(user)
            && (instrument.getSupportExternal() == null || instrument.getSupportExternal() != 1)) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "该仪器不支持校外用户预约");
        }
    }

    public void validateMachineReserveWindow(Instrument instrument, LocalDateTime reserveStart, LocalDateTime reserveEnd) {
        if (reserveStart == null || reserveEnd == null || !reserveEnd.isAfter(reserveStart)) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "预约时间范围不合法");
        }
        if (!reserveStart.toLocalDate().equals(reserveEnd.toLocalDate())) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "暂不支持跨天预约");
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
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "该仪器未配置开放规则");
        }
        LocalDate reserveDate = reserveStart.toLocalDate();
        int weekDay = reserveDate.getDayOfWeek().getValue();
        LocalTime startTime = reserveStart.toLocalTime();
        LocalTime endTime = reserveEnd.toLocalTime();
        boolean matched = rules.stream().anyMatch(rule ->
            containsWeekDay(rule, weekDay)
                && rule.getStartTime() != null && rule.getEndTime() != null
                && isDateInEffectiveRange(reserveDate, rule.getEffectiveStartDate(), rule.getEffectiveEndDate())
                && !startTime.isBefore(rule.getStartTime())
                && !endTime.isAfter(rule.getEndTime())
                && reserveMinutes <= defaultInt(rule.getMaxReserveMinutes(), maxMinutes)
                && reserveMinutes % defaultInt(rule.getStepMinutes(), stepMinutes) == 0
        );
        if (!matched) {
            throw new BizException(ErrorCodes.ORDER_TIME_RANGE_INVALID, "预约时间不符合开放规则");
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
    public void deleteCategory(Long id, SysUser operator) {
        assertAdmin(operator);
        if (categoryRepository.findById(id) == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "仪器分类不存在");
        }
        if (instrumentRepository.countByCategoryId(id) > 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "category is referenced by instruments");
        }
        categoryRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(operator, "INSTRUMENT", "DELETE_CATEGORY", "categoryId:" + id);
    }

    @Transactional
    public void deleteInstrument(Long id, SysUser operator) {
        Instrument instrument = instrumentRepository.findById(id);
        if (instrument == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "instrument not found");
        }
        assertInstrumentManagePermission(instrument, operator);
        if (reservationOrderRepository.countByInstrumentId(id) > 0) {
            throw new BizException(ErrorCodes.BIZ_ERROR, "instrument has reservation history and cannot be deleted");
        }
        LocalDateTime now = LocalDateTime.now();
        openRuleRepository.softDeleteByInstrumentId(id, now);
        attachmentRepository.softDeleteByInstrumentId(id, now);
        instrumentRepository.softDelete(id, now);
        operationLogService.save(operator, "INSTRUMENT", "DELETE_INSTRUMENT", "instrumentId:" + id);
    }

    public List<InstrumentOpenRule> allOpenRules(SysUser operator) {
        assertAdminOrInstrumentManager(operator);
        if (hasRole(operator, "ADMIN")) {
            return openRuleRepository.findAll();
        }
        Set<Long> scopedInstrumentIds = accessibleInstrumentIds(operator);
        return openRuleRepository.findAll().stream()
            .filter(rule -> rule.getInstrumentId() != null && scopedInstrumentIds.contains(rule.getInstrumentId()))
            .collect(Collectors.toList());
    }

    public PageResponse<InstrumentOpenRule> pageOpenRules(Long instrumentId, Integer weekDay, String status,
                                                          int pageNum, int pageSize, SysUser operator) {
        assertAdminOrInstrumentManager(operator);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        int offset = (safePageNum - 1) * safePageSize;
        String roleCode = defaultString(operator.getPrimaryRoleCode(), "INTERNAL_USER").toUpperCase();
        String normalizedStatus = status == null ? null : status.trim();
        List<InstrumentOpenRule> list = openRuleRepository.findPageByScope(
            instrumentId, weekDay, normalizedStatus, roleCode, operator.getId(), operator.getDepartmentId(), offset, safePageSize
        );
        long total = openRuleRepository.countPageByScope(
            instrumentId, weekDay, normalizedStatus, roleCode, operator.getId(), operator.getDepartmentId()
        );
        return new PageResponse<>(list, total, safePageNum, safePageSize);
    }

    public List<InstrumentAttachment> allAttachments(SysUser operator) {
        assertAdminOrInstrumentManager(operator);
        if (hasRole(operator, "ADMIN")) {
            return attachmentRepository.findAll();
        }
        Set<Long> scopedInstrumentIds = accessibleInstrumentIds(operator);
        return attachmentRepository.findAll().stream()
            .filter(item -> item.getInstrumentId() != null && scopedInstrumentIds.contains(item.getInstrumentId()))
            .collect(Collectors.toList());
    }

    public PageResponse<InstrumentAttachment> pageAttachments(SysUser operator, int pageNum, int pageSize) {
        assertAdminOrInstrumentManager(operator);
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = pageSize <= 0 ? 10 : Math.min(pageSize, 100);
        int offset = (safePageNum - 1) * safePageSize;
        String roleCode = defaultString(operator.getPrimaryRoleCode(), "INTERNAL_USER").toUpperCase();
        Long departmentId = operator.getDepartmentId();
        List<InstrumentAttachment> list = attachmentRepository.findPageByScope(
            roleCode, operator.getId(), departmentId, offset, safePageSize
        );
        long total = attachmentRepository.countByScope(roleCode, operator.getId(), departmentId);
        return new PageResponse<>(list, total, safePageNum, safePageSize);
    }

    @Transactional
    public InstrumentAttachment saveAttachment(Long id, InstrumentAttachment request, SysUser operator) {
        Instrument instrument = getById(request.getInstrumentId());
        assertInstrumentManagePermission(instrument, operator);
        InstrumentAttachment attachment = id == null ? new InstrumentAttachment() : attachmentRepository.findById(id);
        if (id != null && attachment == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "附件不存在");
        }
        if (id != null && (attachment.getInstrumentId() == null || !attachment.getInstrumentId().equals(request.getInstrumentId()))) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "instrumentId cannot be changed for existing attachment");
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
        operationLogService.save(operator, "INSTRUMENT", id == null ? "CREATE_ATTACHMENT" : "UPDATE_ATTACHMENT", "attachmentId:" + attachment.getId());
        return attachment;
    }

    @Transactional
    public void deleteOpenRule(Long id, SysUser operator) {
        InstrumentOpenRule rule = openRuleRepository.findById(id);
        if (rule == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "开放规则不存在");
        }
        Instrument instrument = getById(rule.getInstrumentId());
        assertInstrumentManagePermission(instrument, operator);
        openRuleRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(operator, "INSTRUMENT", "DELETE_OPEN_RULE", "ruleId:" + id);
    }

    @Transactional
    public void deleteAttachment(Long id, SysUser operator) {
        InstrumentAttachment attachment = attachmentRepository.findById(id);
        if (attachment == null) {
            throw new BizException(ErrorCodes.RESOURCE_NOT_FOUND, "附件不存在");
        }
        Instrument instrument = getById(attachment.getInstrumentId());
        assertInstrumentManagePermission(instrument, operator);
        attachmentRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(operator, "INSTRUMENT", "DELETE_ATTACHMENT", "attachmentId:" + id);
    }

    private Map<String, Object> toSummary(Instrument instrument) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", instrument.getId());
        result.put("name", instrument.getInstrumentName());
        result.put("instrumentName", instrument.getInstrumentName());
        result.put("code", instrument.getInstrumentNo());
        result.put("instrumentNo", instrument.getInstrumentNo());
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
            throw new BizException(ErrorCodes.INVALID_REQUEST, "预约规则配置不合法");
        }
        if (minReserveMinutes <= 0 || maxReserveMinutes <= 0 || stepMinutes <= 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "预约规则配置必须为正数");
        }
        if (maxReserveMinutes < minReserveMinutes) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "最长预约时长不能小于最短预约时长");
        }
        if (minReserveMinutes % stepMinutes != 0 || maxReserveMinutes % stepMinutes != 0) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "预约时长必须与时间步长对齐");
        }
    }

    private boolean isDateInEffectiveRange(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (startDate != null && date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    private boolean isExternalUser(SysUser user) {
        if (user == null) {
            return false;
        }
        if ("EXTERNAL_USER".equalsIgnoreCase(user.getPrimaryRoleCode())) {
            return true;
        }
        return "EXTERNAL".equalsIgnoreCase(user.getUserType());
    }

    private String validateInstrumentStatus(String status) {
        if (!INSTRUMENT_STATUS_SET.contains(status)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "仪器状态不合法");
        }
        return status;
    }

    private String validateOpenMode(String openMode) {
        if (!OPEN_MODE_SET.contains(openMode)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "开放模式不合法");
        }
        return openMode;
    }

    private String validateBookingUnit(String bookingUnit) {
        if (!BOOKING_UNIT_SET.contains(bookingUnit)) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "预约计费单位不合法");
        }
        return bookingUnit;
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

    private List<Integer> resolveWeekDays(OpenRuleSaveRequest request) {
        Set<Integer> weekDaySet = new java.util.LinkedHashSet<>();
        if (request.getWeekDay() != null) {
            weekDaySet.add(request.getWeekDay());
        }
        if (request.getOpenDays() != null && !request.getOpenDays().trim().isEmpty()) {
            String[] parts = request.getOpenDays().split(",");
            for (String part : parts) {
                String item = part == null ? "" : part.trim();
                if (item.isEmpty()) {
                    continue;
                }
                try {
                    weekDaySet.add(Integer.parseInt(item));
                } catch (NumberFormatException ex) {
                    throw new BizException(ErrorCodes.INVALID_REQUEST, "星期格式不正确");
                }
            }
        }
        if (weekDaySet.isEmpty()) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "至少选择一个开放星期");
        }
        for (Integer day : weekDaySet) {
            validateWeekDay(day);
        }
        return new java.util.ArrayList<>(weekDaySet);
    }

    private void validateWeekDay(Integer weekDay) {
        if (weekDay == null || weekDay < 1 || weekDay > 7) {
            throw new BizException(ErrorCodes.INVALID_REQUEST, "星期取值必须在1到7之间");
        }
    }

    private LocalTime resolveStartTime(OpenRuleSaveRequest request) {
        if (request.getStartTime() != null && !request.getStartTime().trim().isEmpty()) {
            return LocalTime.parse(request.getStartTime().trim());
        }
        if (request.getOpenTimeRange() != null && request.getOpenTimeRange().contains("-")) {
            return LocalTime.parse(request.getOpenTimeRange().split("-")[0].trim());
        }
        throw new BizException(ErrorCodes.INVALID_REQUEST, "开始时间不能为空");
    }

    private LocalTime resolveEndTime(OpenRuleSaveRequest request) {
        if (request.getEndTime() != null && !request.getEndTime().trim().isEmpty()) {
            return LocalTime.parse(request.getEndTime().trim());
        }
        if (request.getOpenTimeRange() != null && request.getOpenTimeRange().contains("-")) {
            return LocalTime.parse(request.getOpenTimeRange().split("-")[1].trim());
        }
        throw new BizException(ErrorCodes.INVALID_REQUEST, "结束时间不能为空");
    }

    private void assertOpenRuleNoOverlap(Long currentRuleId, Long instrumentId, List<Integer> weekDays,
                                         LocalTime startTime, LocalTime endTime,
                                         LocalDate effectiveStartDate, LocalDate effectiveEndDate) {
        List<InstrumentOpenRule> rules = openRuleRepository.findByInstrumentId(instrumentId).stream()
            .filter(rule -> "ENABLED".equalsIgnoreCase(rule.getStatus()))
            .filter(rule -> currentRuleId == null || !currentRuleId.equals(rule.getId()))
            .filter(rule -> hasWeekDayIntersection(rule, weekDays))
            .collect(Collectors.toList());
        for (InstrumentOpenRule rule : rules) {
            if (rule.getStartTime() == null || rule.getEndTime() == null) {
                continue;
            }
            boolean timeOverlap = startTime.isBefore(rule.getEndTime()) && endTime.isAfter(rule.getStartTime());
            if (!timeOverlap) {
                continue;
            }
            if (isEffectiveDateOverlap(effectiveStartDate, effectiveEndDate, rule.getEffectiveStartDate(), rule.getEffectiveEndDate())) {
                throw new BizException(ErrorCodes.INVALID_REQUEST, "开放规则与已有规则冲突");
            }
        }
    }

    private boolean containsWeekDay(InstrumentOpenRule rule, int targetWeekDay) {
        if (rule == null) {
            return false;
        }
        String weekDays = rule.getWeekDays();
        if (weekDays != null && !weekDays.trim().isEmpty()) {
            String[] parts = weekDays.split(",");
            for (String part : parts) {
                if (String.valueOf(targetWeekDay).equals(part == null ? "" : part.trim())) {
                    return true;
                }
            }
            return false;
        }
        return rule.getWeekDay() != null && rule.getWeekDay() == targetWeekDay;
    }

    private boolean hasWeekDayIntersection(InstrumentOpenRule rule, List<Integer> targetWeekDays) {
        if (targetWeekDays == null || targetWeekDays.isEmpty()) {
            return false;
        }
        for (Integer targetDay : targetWeekDays) {
            if (targetDay != null && containsWeekDay(rule, targetDay)) {
                return true;
            }
        }
        return false;
    }

    private String joinWeekDays(List<Integer> weekDays) {
        return weekDays.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private boolean isEffectiveDateOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        LocalDate s1 = start1 == null ? LocalDate.of(1970, 1, 1) : start1;
        LocalDate e1 = end1 == null ? LocalDate.of(2999, 12, 31) : end1;
        LocalDate s2 = start2 == null ? LocalDate.of(1970, 1, 1) : start2;
        LocalDate e2 = end2 == null ? LocalDate.of(2999, 12, 31) : end2;
        return !e1.isBefore(s2) && !e2.isBefore(s1);
    }

    private void assertInstrumentManagePermission(Instrument instrument, SysUser operator) {
        if (operator == null) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限执行该操作");
        }
        String roleCode = operator.getPrimaryRoleCode();
        if ("ADMIN".equalsIgnoreCase(roleCode)) {
            return;
        }
        if ("INSTRUMENT_OWNER".equalsIgnoreCase(roleCode)
            && instrument.getOwnerUserId() != null
            && instrument.getOwnerUserId().equals(operator.getId())) {
            return;
        }
        if ("DEPT_MANAGER".equalsIgnoreCase(roleCode)
            && instrument.getDepartmentId() != null
            && instrument.getDepartmentId().equals(operator.getDepartmentId())) {
            return;
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限管理该仪器");
    }

    private void assertAdmin(SysUser operator) {
        if (operator == null || !"ADMIN".equalsIgnoreCase(operator.getPrimaryRoleCode())) {
            throw new BizException(ErrorCodes.PERMISSION_DENIED, "仅管理员可执行该操作");
        }
    }

    public void assertAdminOrInstrumentManager(SysUser operator) {
        if (hasRole(operator, "ADMIN") || hasRole(operator, "INSTRUMENT_OWNER") || hasRole(operator, "DEPT_MANAGER")) {
            return;
        }
        throw new BizException(ErrorCodes.PERMISSION_DENIED, "无权限执行该操作");
    }

    private boolean hasRole(SysUser user, String roleCode) {
        return user != null && roleCode.equalsIgnoreCase(user.getPrimaryRoleCode());
    }

    private Set<Long> accessibleInstrumentIds(SysUser operator) {
        return instrumentRepository.findAll().stream()
            .filter(item -> canAccessByRole(item, operator))
            .map(Instrument::getId)
            .collect(Collectors.toSet());
    }

    private boolean canAccessByRole(Instrument instrument, SysUser operator) {
        if (hasRole(operator, "ADMIN")) {
            return true;
        }
        if (hasRole(operator, "INSTRUMENT_OWNER")) {
            return instrument.getOwnerUserId() != null && instrument.getOwnerUserId().equals(operator.getId());
        }
        if (hasRole(operator, "DEPT_MANAGER")) {
            return instrument.getDepartmentId() != null && instrument.getDepartmentId().equals(operator.getDepartmentId());
        }
        return false;
    }
}
