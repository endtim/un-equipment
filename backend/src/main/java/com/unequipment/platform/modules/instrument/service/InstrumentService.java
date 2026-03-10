package com.unequipment.platform.modules.instrument.service;

import com.unequipment.platform.common.api.PageResponse;
import com.unequipment.platform.common.exception.BizException;
import com.unequipment.platform.modules.log.service.OperationLogService;
import com.unequipment.platform.modules.instrument.dto.InstrumentSaveRequest;
import com.unequipment.platform.modules.instrument.dto.OpenRuleSaveRequest;
import com.unequipment.platform.modules.instrument.entity.Instrument;
import com.unequipment.platform.modules.instrument.entity.InstrumentAttachment;
import com.unequipment.platform.modules.instrument.entity.InstrumentCategory;
import com.unequipment.platform.modules.instrument.entity.InstrumentOpenRule;
import com.unequipment.platform.modules.instrument.repository.InstrumentAttachmentRepository;
import com.unequipment.platform.modules.instrument.repository.InstrumentCategoryRepository;
import com.unequipment.platform.modules.instrument.repository.InstrumentOpenRuleRepository;
import com.unequipment.platform.modules.instrument.repository.InstrumentRepository;
import com.unequipment.platform.modules.system.entity.SysUser;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
            throw new BizException("category not found");
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
            throw new BizException("category not found");
        }
        instrument.setInstrumentNo(request.getCode() == null ? "INST-" + System.currentTimeMillis() : request.getCode());
        instrument.setInstrumentName(request.getName());
        instrument.setCategoryId(category.getId());
        instrument.setDepartmentId(operator.getDepartmentId());
        instrument.setOwnerUserId(operator.getId());
        instrument.setLocation(request.getLocation());
        instrument.setCoverUrl(request.getCoverImage());
        instrument.setIntro(request.getDescription());
        instrument.setUsageDesc(request.getDescription());
        instrument.setSampleDesc(request.getDescription());
        instrument.setPriceInternal(defaultValue(firstNonNull(request.getPriceInternal(), request.getMachinePricePerHour())));
        instrument.setPriceExternal(defaultValue(firstNonNull(request.getPriceExternal(), request.getSamplePricePerItem())));
        instrument.setStatus(request.getStatus() == null ? "NORMAL" : request.getStatus());
        instrument.setOpenMode("BOTH");
        instrument.setOpenStatus(1);
        instrument.setSupportExternal(1);
        instrument.setNeedAudit(1);
        instrument.setRequireTraining(0);
        instrument.setBookingUnit("HOUR");
        instrument.setMinReserveMinutes(30);
        instrument.setMaxReserveMinutes(480);
        instrument.setStepMinutes(30);
        instrument.setIsHot(0);
        instrument.setSortNo(0);
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
        String[] range = request.getOpenTimeRange().split("-");
        InstrumentOpenRule rule = id == null ? new InstrumentOpenRule() : openRuleRepository.findById(id);
        if (id != null && rule == null) {
            throw new BizException("open rule not found");
        }
        rule.setInstrumentId(instrument.getId());
        rule.setWeekDay(Integer.parseInt(request.getOpenDays()));
        rule.setStartTime(java.time.LocalTime.parse(range[0].trim()));
        rule.setEndTime(java.time.LocalTime.parse(range[1].trim()));
        rule.setMaxReserveMinutes(480);
        rule.setStepMinutes(30);
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
            throw new BizException("instrument is not available");
        }
    }

    public Instrument getById(Long id) {
        Instrument instrument = instrumentRepository.findById(id);
        if (instrument == null) {
            throw new BizException("instrument not found");
        }
        return instrument;
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (categoryRepository.findById(id) == null) {
            throw new BizException("category not found");
        }
        categoryRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(null, "INSTRUMENT", "DELETE_CATEGORY", "categoryId:" + id);
    }

    @Transactional
    public void deleteInstrument(Long id) {
        if (instrumentRepository.findById(id) == null) {
            throw new BizException("instrument not found");
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
            throw new BizException("attachment not found");
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
            throw new BizException("open rule not found");
        }
        openRuleRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(null, "INSTRUMENT", "DELETE_OPEN_RULE", "ruleId:" + id);
    }

    @Transactional
    public void deleteAttachment(Long id) {
        if (attachmentRepository.findById(id) == null) {
            throw new BizException("attachment not found");
        }
        attachmentRepository.softDelete(id, LocalDateTime.now());
        operationLogService.save(null, "INSTRUMENT", "DELETE_ATTACHMENT", "attachmentId:" + id);
    }

    private Map<String, Object> toSummary(Instrument instrument) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", instrument.getId());
        result.put("name", instrument.getInstrumentName());
        result.put("code", instrument.getInstrumentNo());
        result.put("categoryName", instrument.getCategoryName());
        result.put("status", instrument.getStatus());
        result.put("location", instrument.getLocation());
        result.put("coverImage", instrument.getCoverUrl());
        result.put("description", instrument.getIntro());
        result.put("departmentName", instrument.getDepartmentName());
        result.put("ownerUserName", instrument.getOwnerUserName());
        result.put("openMode", instrument.getOpenMode());
        result.put("openStatus", instrument.getOpenStatus());
        result.put("supportExternal", instrument.getSupportExternal());
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
}
