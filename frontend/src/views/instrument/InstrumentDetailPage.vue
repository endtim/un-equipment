<template>
  <div v-if="detail" class="detail-page">
    <div class="content-card detail-hero">
      <div class="hero-main">
        <div class="hero-code">{{ detail.code || '暂无编号' }}</div>
        <div class="section-title" style="margin-bottom: 10px;">{{ detail.name }}</div>
        <div class="hero-desc">{{ detail.description || '暂无仪器简介。' }}</div>
        <div class="hero-tags">
          <el-tag :type="statusTagType(detail.status)" size="small">{{ statusLabel }}</el-tag>
          <el-tag size="small">{{ openModeLabel(detail.openMode) }}</el-tag>
          <el-tag v-if="Number(detail.needAudit) === 1" size="small" type="warning">需审核</el-tag>
          <el-tag v-if="Number(detail.requireTraining) === 1" size="small" type="info">需培训</el-tag>
        </div>
        <div class="metrics-row">
          <div class="metric-item">
            <strong>{{ detail.metrics?.reserveCount || 0 }}</strong>
            <span>预约次数</span>
          </div>
          <div class="metric-item">
            <strong>{{ detail.metrics?.reserveUserCount || 0 }}</strong>
            <span>预约人数</span>
          </div>
          <div class="metric-item">
            <strong>{{ detail.metrics?.totalUsageMinutes || 0 }}</strong>
            <span>累计机时(分钟)</span>
          </div>
          <div class="metric-item">
            <strong>{{ detail.metrics?.sampleCount || 0 }}</strong>
            <span>样品数量</span>
          </div>
        </div>
      </div>
      <div class="hero-side">
        <div class="price-box">
          <div class="price-item">
            <span>校内价格</span>
            <strong>{{ formatAmount(detail.priceInternal ?? detail.machinePricePerHour) }}</strong>
            <em>元/{{ detail.bookingUnit === 'HOUR' ? '小时' : '单位' }}</em>
          </div>
          <div class="price-item">
            <span>校外价格</span>
            <strong>{{ formatAmount(detail.priceExternal ?? detail.samplePricePerItem) }}</strong>
            <em>元/{{ detail.bookingUnit === 'HOUR' ? '小时' : '单位' }}</em>
          </div>
        </div>
        <div class="hero-actions">
          <el-button @click="goBack">返回列表</el-button>
          <el-button type="primary" :disabled="!canReserve" @click="activateReserve(preferredReserveMode)">
            {{ reserveButtonText }}
          </el-button>
        </div>
      </div>
    </div>

    <div class="detail-grid">
      <div class="detail-main">
        <div class="content-card block-card">
          <div class="block-title">基础信息</div>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="仪器名称">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="仪器编号">{{ detail.code || '-' }}</el-descriptions-item>
            <el-descriptions-item label="仪器分类">{{ detail.categoryName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所属单位">{{ detail.departmentName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ detail.ownerUserName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="放置地点">{{ detail.location || '-' }}</el-descriptions-item>
            <el-descriptions-item label="品牌型号">{{ detail.brand || '-' }} {{ detail.model || '' }}</el-descriptions-item>
            <el-descriptions-item label="预约单位">{{ detail.bookingUnit || '-' }}</el-descriptions-item>
            <el-descriptions-item label="资产编号">{{ detail.assetNo || '-' }}</el-descriptions-item>
            <el-descriptions-item label="生产厂家">{{ detail.manufacturer || '-' }}</el-descriptions-item>
            <el-descriptions-item label="供应商">{{ detail.supplier || '-' }}</el-descriptions-item>
            <el-descriptions-item label="制造国家">{{ detail.originCountry || '-' }}</el-descriptions-item>
            <el-descriptions-item label="购置日期">{{ detail.purchaseDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="生产日期">{{ detail.productionDate || '-' }}</el-descriptions-item>
            <el-descriptions-item label="设备来源">{{ detail.equipmentSource || '-' }}</el-descriptions-item>
            <el-descriptions-item label="使用联系人">
              {{ detail.serviceContactName || '-' }} {{ detail.serviceContactPhone ? `(${detail.serviceContactPhone})` : '' }}
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <div class="content-card block-card">
          <div class="block-title">开放与预约说明</div>
          <div class="rich-text">{{ detail.usageDesc || '暂无使用说明。' }}</div>
          <div class="rule-grid">
            <div class="rule-item">
              <span>开放模式</span>
              <strong>{{ openModeLabel(detail.openMode) }}</strong>
            </div>
            <div class="rule-item">
              <span>最短预约</span>
              <strong>{{ detail.minReserveMinutes || 0 }} 分钟</strong>
            </div>
            <div class="rule-item">
              <span>最长预约</span>
              <strong>{{ detail.maxReserveMinutes || 0 }} 分钟</strong>
            </div>
            <div class="rule-item">
              <span>时间步长</span>
              <strong>{{ detail.stepMinutes || 0 }} 分钟</strong>
            </div>
          </div>
          <div class="open-rule-list">
            <div v-for="item in detail.openRules || []" :key="item.id" class="open-rule-row">
              {{ weekLabels(item) }}：{{ String(item.startTime).slice(0, 5) }} - {{ String(item.endTime).slice(0, 5) }}
            </div>
          </div>
        </div>

        <div class="content-card block-card">
          <div class="block-title">送样说明</div>
          <div class="rich-text">{{ detail.sampleDesc || '暂无送样说明。' }}</div>
        </div>

        <div class="content-card block-card">
          <div class="block-title">主要技术指标</div>
          <div class="rich-text">{{ detail.technicalSpecs || '暂无技术指标。' }}</div>
        </div>

        <div class="content-card block-card">
          <div class="block-title">主要功能</div>
          <div class="rich-text">{{ detail.mainFunctions || '暂无功能说明。' }}</div>
        </div>

        <div class="content-card block-card">
          <div class="block-title">服务内容</div>
          <div class="rich-text">{{ detail.serviceContent || '暂无服务内容。' }}</div>
        </div>

        <div class="content-card block-card">
          <div class="block-title">用户须知</div>
          <div class="rich-text">{{ detail.userNotice || detail.noticeText || '暂无用户须知。' }}</div>
        </div>

        <div class="content-card block-card">
          <div class="block-title">收费标准说明</div>
          <div class="rich-text">{{ detail.chargeStandard || '暂无收费说明。' }}</div>
        </div>

        <div class="content-card block-card">
          <div class="block-title">附件资料</div>
          <div v-if="detail.attachments && detail.attachments.length" class="attachment-list">
            <div v-for="item in detail.attachments" :key="item.id" class="attachment-row">
              <span>{{ item.fileName }}</span>
              <a :href="item.fileUrl" target="_blank" rel="noreferrer">查看附件</a>
            </div>
          </div>
          <div v-else class="empty-text">暂无附件资料</div>
        </div>
      </div>

      <div class="detail-side">
        <div class="content-card reserve-card">
          <div class="block-title">预约办理</div>
          <div v-if="!canReserve" class="reserve-disabled">{{ statusReason }}</div>

          <div class="mode-switch">
            <el-button
              v-if="supportsMachine"
              :type="activeReserve === 'machine' ? 'primary' : 'default'"
              @click="activateReserve('machine')"
            >
              上机预约
            </el-button>
            <el-button
              v-if="supportsSample"
              :type="activeReserve === 'sample' ? 'primary' : 'default'"
              @click="activateReserve('sample')"
            >
              送样预约
            </el-button>
          </div>

          <div v-if="activeReserve === 'machine'" class="reserved-slot-panel">
            <div class="reserved-slot-header">
              <span class="reserved-slot-title">已预约时间段</span>
              <el-date-picker
                v-model="slotDate"
                type="date"
                value-format="YYYY-MM-DD"
                format="YYYY-MM-DD"
                placeholder="选择日期"
                style="width: 170px;"
                @change="loadReservedSlots"
              />
            </div>
            <div v-loading="reservedSlotsLoading" class="reserved-slot-body">
              <div v-if="reservedSlots.length === 0" class="empty-text">当天暂无已预约记录</div>
              <div v-else class="reserved-slot-list">
                <div v-for="item in reservedSlots" :key="`${item.orderId}-${item.text}`" class="reserved-slot-item">
                  <span>{{ item.text }}</span>
                  <el-tag size="small" type="warning">{{ item.statusLabel || '已占用' }}</el-tag>
                </div>
              </div>
            </div>
          </div>

          <el-form v-if="activeReserve === 'machine'" :model="machineForm" label-position="top">
            <el-form-item label="预约开始时间">
              <el-date-picker v-model="machineForm.reservedStart" type="datetime" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="预约结束时间">
              <el-date-picker v-model="machineForm.reservedEnd" type="datetime" style="width: 100%;" />
            </el-form-item>
            <el-button type="primary" style="width: 100%;" :disabled="!canReserve" @click="submitMachine">提交上机预约</el-button>
          </el-form>

          <el-form v-if="activeReserve === 'sample'" :model="sampleForm" label-position="top">
            <el-form-item label="样品名称">
              <el-input v-model="sampleForm.sampleName" />
            </el-form-item>
            <el-form-item label="样品数量">
              <el-input-number v-model="sampleForm.sampleCount" :min="1" style="width: 100%;" />
            </el-form-item>
            <el-button type="primary" style="width: 100%;" :disabled="!canReserve" @click="submitSample">提交送样预约</el-button>
          </el-form>
        </div>

        <div class="content-card reserve-card">
          <div class="block-title">运行状态</div>
          <div class="runtime-status">
            <el-tag :type="detail.runtimeStatus?.available ? 'success' : 'danger'" size="small">
              {{ detail.runtimeStatus?.available ? '可预约' : '不可预约' }}
            </el-tag>
            <div class="runtime-text">
              {{ detail.runtimeStatus?.reason || '当前状态正常，可按开放规则预约。' }}
            </div>
            <div v-if="detail.runtimeStatus?.recoverTime" class="runtime-text">
              预计恢复时间：{{ formatDateTime(detail.runtimeStatus.recoverTime) }}
            </div>
          </div>
        </div>

        <div class="content-card reserve-card">
          <div class="block-title">预约提示</div>
          <ul class="tip-list">
            <li>预约前请确认登录状态、仪器状态和开放时段。</li>
            <li>需审核仪器提交后将进入待审核流程。</li>
            <li>若仪器维护、停用或故障，系统将禁止预约。</li>
            <li>提交成功后可在用户中心查看订单进度。</li>
          </ul>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import { getInstrumentDetail, getInstrumentReservedSlots } from '../../api/instrument'
import { createMachineReservation, createSampleReservation } from '../../api/order'
import { openModeLabel } from '../../utils/dicts'
import { formatDateTime as formatDateTimeUtil } from '../../utils/datetime'

export default {
  props: ['id'],
  data() {
    return {
      detail: null,
      activeReserve: 'machine',
      slotDate: dayjs().format('YYYY-MM-DD'),
      reservedSlots: [],
      reservedSlotsLoading: false,
      machineForm: {
        reservedStart: '',
        reservedEnd: '',
        projectName: '',
        purpose: ''
      },
      sampleForm: {
        sampleName: '',
        sampleCount: 1,
        projectName: '',
        purpose: ''
      }
    }
  },
  computed: {
    supportsMachine() {
      return ['MACHINE', 'BOTH', null, undefined, ''].includes(this.detail?.openMode)
    },
    supportsSample() {
      return ['SAMPLE', 'BOTH'].includes(this.detail?.openMode)
    },
    canReserve() {
      return this.detail?.status === 'NORMAL' && Number(this.detail?.openStatus || 1) === 1
    },
    preferredReserveMode() {
      if (this.$route.query.reserve === 'sample' && this.supportsSample) {
        return 'sample'
      }
      if (this.supportsMachine) {
        return 'machine'
      }
      return 'sample'
    },
    reserveButtonText() {
      if (!this.canReserve) {
        return '暂不可预约'
      }
      return this.preferredReserveMode === 'sample' ? '立即送样' : '立即预约'
    },
    statusLabel() {
      return this.canReserve ? '正常开放' : '暂停开放'
    },
    statusReason() {
      if (this.canReserve) {
        return ''
      }
      return this.detail?.runtimeStatus?.reason || '当前仪器处于停用、维护或未开放状态，暂不可预约。'
    }
  },
  watch: {
    'machineForm.reservedStart'(value) {
      if (!value) {
        return
      }
      const nextDate = dayjs(value).format('YYYY-MM-DD')
      if (nextDate && nextDate !== this.slotDate) {
        this.slotDate = nextDate
        this.loadReservedSlots()
      }
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    openModeLabel,
    async load() {
      this.detail = await getInstrumentDetail(this.id)
      this.activeReserve = this.preferredReserveMode
      await this.loadReservedSlots()
    },
    async loadReservedSlots() {
      if (!this.detail || !this.supportsMachine || !this.slotDate) {
        this.reservedSlots = []
        return
      }
      this.reservedSlotsLoading = true
      try {
        const list = await getInstrumentReservedSlots(this.id, this.slotDate)
        this.reservedSlots = (list || [])
          .map(item => this.normalizeReservedSlot(item))
          .filter(item => item.startAt && item.endAt)
      } catch (error) {
        this.reservedSlots = []
      } finally {
        this.reservedSlotsLoading = false
      }
    },
    normalizeReservedSlot(item) {
      const startAt = this.resolveDateTime(item.reservedStart, item.startTime)
      const endAt = this.resolveDateTime(item.reservedEnd, item.endTime)
      const startText = startAt ? dayjs(startAt).format('HH:mm') : ''
      const endText = endAt ? dayjs(endAt).format('HH:mm') : ''
      return {
        ...item,
        startAt,
        endAt,
        text: item.text || `${startText} - ${endText} 已占用`,
        statusLabel: item.statusLabel || '已占用'
      }
    },
    resolveDateTime(dateTimeValue, timeValue) {
      if (dateTimeValue) {
        const parsed = dayjs(dateTimeValue)
        if (parsed.isValid()) {
          return parsed.toDate()
        }
      }
      if (!timeValue || !this.slotDate) {
        return null
      }
      const normalizedTime = String(timeValue).slice(0, 8)
      const parsed = dayjs(`${this.slotDate} ${normalizedTime}`)
      return parsed.isValid() ? parsed.toDate() : null
    },
    hasReservedSlotConflict(start, end) {
      const startAt = dayjs(start)
      const endAt = dayjs(end)
      if (!startAt.isValid() || !endAt.isValid()) {
        return false
      }
      return this.reservedSlots.some(slot => {
        const slotStart = dayjs(slot.startAt)
        const slotEnd = dayjs(slot.endAt)
        return startAt.isBefore(slotEnd) && endAt.isAfter(slotStart)
      })
    },
    goBack() {
      const query = { ...this.$route.query }
      delete query.returnPath
      delete query.reserve
      this.$router.push({
        path: this.$route.query.returnPath || '/instruments',
        query
      })
    },
    activateReserve(mode) {
      this.activeReserve = mode
    },
    async submitMachine() {
      if (!this.machineForm.reservedStart || !this.machineForm.reservedEnd) {
        this.$message.warning('请选择预约开始时间和结束时间')
        return
      }
      if (!dayjs(this.machineForm.reservedEnd).isAfter(dayjs(this.machineForm.reservedStart))) {
        this.$message.warning('预约时间范围无效，请重新选择')
        return
      }
      const formDate = dayjs(this.machineForm.reservedStart).format('YYYY-MM-DD')
      if (formDate !== this.slotDate) {
        this.slotDate = formDate
        await this.loadReservedSlots()
      }
      if (this.hasReservedSlotConflict(this.machineForm.reservedStart, this.machineForm.reservedEnd)) {
        this.$message.error('所选时间段与已有预约冲突，请重新选择')
        return
      }
      try {
        await createMachineReservation({
          instrumentId: this.detail.id,
          reservedStart: dayjs(this.machineForm.reservedStart).format('YYYY-MM-DDTHH:mm:ss'),
          reservedEnd: dayjs(this.machineForm.reservedEnd).format('YYYY-MM-DDTHH:mm:ss'),
          projectName: this.machineForm.projectName || undefined,
          purpose: this.machineForm.purpose || undefined
        })
        this.$message.success('上机预约已提交')
        this.$router.push('/center/orders')
      } catch (error) {
        // 错误提示由请求拦截器统一处理
      }
    },
    async submitSample() {
      try {
        await createSampleReservation({
          instrumentId: this.detail.id,
          sampleName: this.sampleForm.sampleName,
          sampleCount: this.sampleForm.sampleCount,
          projectName: this.sampleForm.projectName || undefined,
          purpose: this.sampleForm.purpose || undefined
        })
        this.$message.success('送样预约已提交')
        this.$router.push('/center/orders')
      } catch (error) {
        // 错误提示由请求拦截器统一处理
      }
    },
    statusTagType(status) {
      return status === 'NORMAL' ? 'success' : 'danger'
    },
    formatAmount(value) {
      return value == null ? '0.00' : Number(value).toFixed(2)
    },
    formatDateTime(value) {
      return formatDateTimeUtil(value)
    },
    weekLabels(rule) {
      const text = rule?.weekDays || (rule?.weekDay != null ? String(rule.weekDay) : '')
      if (!text) {
        return '-'
      }
      return text
        .split(',')
        .map(item => Number(item))
        .filter(item => item >= 1 && item <= 7)
        .map(item => `周${this.weekLabel(item)}`)
        .join('、')
    },
    weekLabel(value) {
      const mapping = ['一', '二', '三', '四', '五', '六', '日']
      return mapping[(Number(value) || 1) - 1] || value
    }
  }
}
</script>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.detail-hero {
  padding: 26px 28px;
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.hero-main {
  flex: 1;
}

.hero-code {
  color: var(--primary);
  font-size: 14px;
  margin-bottom: 8px;
}

.hero-desc {
  color: var(--muted);
  line-height: 1.9;
  max-width: 780px;
}

.hero-tags {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 16px;
}

.metrics-row {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.metric-item {
  background: #f8fbff;
  border: 1px solid var(--line);
  padding: 10px 12px;
}

.metric-item strong {
  display: block;
  color: #cc4b00;
  font-size: 20px;
}

.metric-item span {
  color: var(--muted);
  font-size: 12px;
}

.hero-side {
  width: 320px;
}

.price-box {
  border: 1px solid var(--line);
  background: #f8fbff;
}

.price-item {
  padding: 16px 18px;
  border-bottom: 1px solid var(--line);
  display: flex;
  align-items: baseline;
  gap: 8px;
}

.price-item:last-child {
  border-bottom: 0;
}

.price-item span,
.price-item em {
  color: var(--muted);
  font-style: normal;
}

.price-item strong {
  font-size: 28px;
  color: var(--primary);
}

.hero-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.hero-actions .el-button {
  flex: 1;
}

.detail-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.5fr) 360px;
  gap: 18px;
}

.detail-main,
.detail-side {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.block-card,
.reserve-card {
  padding: 22px;
}

.block-title {
  font-size: 20px;
  font-weight: 700;
  margin-bottom: 16px;
  color: #1e4278;
}

.rich-text {
  color: #45566f;
  line-height: 1.9;
}

.rule-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.rule-item {
  padding: 14px;
  background: #f8fbff;
  border: 1px solid var(--line);
}

.rule-item span {
  display: block;
  color: var(--muted);
  font-size: 13px;
}

.rule-item strong {
  display: block;
  margin-top: 8px;
  color: var(--primary);
}

.open-rule-list {
  margin-top: 16px;
}

.open-rule-row,
.attachment-row {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px dashed #dbe3ef;
}

.attachment-row a {
  color: var(--primary);
}

.empty-text,
.reserve-disabled {
  color: var(--muted);
  line-height: 1.8;
}

.mode-switch {
  display: flex;
  gap: 10px;
  margin: 14px 0 18px;
}

.reserved-slot-panel {
  border: 1px solid var(--line);
  background: #f8fbff;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 14px;
}

.reserved-slot-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.reserved-slot-title {
  color: #1e4278;
  font-weight: 600;
}

.reserved-slot-body {
  margin-top: 10px;
  min-height: 56px;
}

.reserved-slot-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.reserved-slot-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 10px;
  border-radius: 4px;
  background: #fff;
  border: 1px solid #e3ebf5;
}

.tip-list {
  margin: 0;
  padding-left: 18px;
  color: #45566f;
  line-height: 2;
}

.runtime-status {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.runtime-text {
  color: #45566f;
  line-height: 1.8;
}

@media (max-width: 1100px) {
  .detail-grid,
  .detail-hero,
  .rule-grid,
  .metrics-row {
    grid-template-columns: 1fr;
    display: grid;
  }

  .hero-side {
    width: auto;
  }
}
</style>
