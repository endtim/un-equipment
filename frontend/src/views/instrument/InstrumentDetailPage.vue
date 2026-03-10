<template>
  <div v-if="detail" class="detail-page">
    <div class="content-card detail-hero">
      <div class="hero-main">
        <div class="hero-code">{{ detail.code || '暂无编号' }}</div>
        <div class="section-title" style="margin-bottom: 10px;">{{ detail.name }}</div>
        <div class="hero-desc">{{ detail.description || '暂无仪器简介。' }}</div>
        <div class="hero-tags">
          <el-tag :type="statusTagType(detail.status)" size="small">{{ statusLabel }}</el-tag>
          <el-tag size="small">{{ modeLabel(detail.openMode) }}</el-tag>
          <el-tag v-if="Number(detail.needAudit) === 1" size="small" type="warning">需审核</el-tag>
          <el-tag v-if="Number(detail.requireTraining) === 1" size="small" type="info">需培训</el-tag>
        </div>
      </div>
      <div class="hero-side">
        <div class="price-box">
          <div class="price-item">
            <span>机时收费</span>
            <strong>{{ formatAmount(detail.machinePricePerHour) }}</strong>
            <em>元/小时</em>
          </div>
          <div class="price-item">
            <span>送样收费</span>
            <strong>{{ formatAmount(detail.samplePricePerItem) }}</strong>
            <em>元/次</em>
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
          </el-descriptions>
        </div>

        <div class="content-card block-card">
          <div class="block-title">开放与预约说明</div>
          <div class="rich-text">{{ detail.usageDesc || '暂无使用说明。' }}</div>
          <div class="rule-grid">
            <div class="rule-item">
              <span>开放模式</span>
              <strong>{{ modeLabel(detail.openMode) }}</strong>
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
              周{{ weekLabel(item.weekDay) }}：{{ String(item.startTime).slice(0, 5) }} - {{ String(item.endTime).slice(0, 5) }}
            </div>
          </div>
        </div>

        <div class="content-card block-card">
          <div class="block-title">送样说明</div>
          <div class="rich-text">{{ detail.sampleDesc || '暂无送样说明。' }}</div>
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
          <div class="block-title">预约提示</div>
          <ul class="tip-list">
            <li>预约前请确认登录状态、仪器状态和开放时段。</li>
            <li>需审核仪器提交后进入待审核流程。</li>
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
import { getInstrumentDetail } from '../../api/instrument'
import { createMachineReservation, createSampleReservation } from '../../api/order'

export default {
  props: ['id'],
  data() {
    return {
      detail: null,
      activeReserve: 'machine',
      machineForm: {
        reservedStart: '',
        reservedEnd: ''
      },
      sampleForm: {
        sampleName: '',
        sampleCount: 1
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
      return '当前仪器处于停用、维护或未开放状态，暂不可预约。'
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async load() {
      this.detail = await getInstrumentDetail(this.id)
      this.activeReserve = this.preferredReserveMode
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
      try {
        await createMachineReservation({
          instrumentId: this.detail.id,
          reservedStart: dayjs(this.machineForm.reservedStart).format('YYYY-MM-DDTHH:mm:ss'),
          reservedEnd: dayjs(this.machineForm.reservedEnd).format('YYYY-MM-DDTHH:mm:ss')
        })
        this.$message.success('上机预约已提交')
        this.$router.push('/center/orders')
      } catch (error) {
        // 错误提示统一由请求拦截器处理，页面侧避免未捕获异常。
      }
    },
    async submitSample() {
      try {
        await createSampleReservation({
          instrumentId: this.detail.id,
          sampleName: this.sampleForm.sampleName,
          sampleCount: this.sampleForm.sampleCount
        })
        this.$message.success('送样预约已提交')
        this.$router.push('/center/orders')
      } catch (error) {
        // 错误提示统一由请求拦截器处理，页面侧避免未捕获异常。
      }
    },
    modeLabel(value) {
      const mapping = {
        MACHINE: '仅支持上机',
        SAMPLE: '仅支持送样',
        BOTH: '支持上机/送样'
      }
      return mapping[value] || '支持上机预约'
    },
    statusTagType(status) {
      return status === 'NORMAL' ? 'success' : 'danger'
    },
    formatAmount(value) {
      return value == null ? '0.00' : Number(value).toFixed(2)
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

.tip-list {
  margin: 0;
  padding-left: 18px;
  color: #45566f;
  line-height: 2;
}

@media (max-width: 1100px) {
  .detail-grid,
  .detail-hero,
  .rule-grid {
    grid-template-columns: 1fr;
    display: grid;
  }

  .hero-side {
    width: auto;
  }
}
</style>
