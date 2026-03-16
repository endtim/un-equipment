<template>
  <div class="admin-page">
    <div class="grid-3" style="margin-bottom: 12px;">
      <div class="content-card stat-card" v-for="card in dashboardCards" :key="card.label">
        <div class="stat-label">{{ card.label }}</div>
        <div class="stat-value">{{ card.value }}</div>
      </div>
    </div>

    <div class="content-card admin-table-card" style="margin-bottom: 14px;">
      <h3 class="admin-card-title">结算管理</h3>

      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          placeholder="结算单号/订单号/申请人/仪器"
          clearable
          style="width: 260px"
          @keyup.enter="onQueryChange"
        />
        <el-select v-model="query.status" clearable placeholder="结算状态" style="width: 160px" @change="onQueryChange">
          <el-option label="待结算" value="PENDING" />
          <el-option label="已结算" value="CONFIRMED" />
          <el-option label="退款待审批" value="REFUND_PENDING" />
          <el-option label="退款处理中" value="REFUNDING" />
          <el-option label="已退款" value="REFUNDED" />
          <el-option label="已作废" value="VOID" />
        </el-select>
        <el-select v-model="query.departmentId" clearable placeholder="申请部门" style="width: 180px" @change="onQueryChange">
          <el-option v-for="item in departments" :key="item.id" :label="item.deptName" :value="item.id" />
        </el-select>
        <el-input-number v-model="query.orderId" :min="1" :controls="false" placeholder="订单ID" style="width: 120px" />
        <el-date-picker
          v-model="query.createRange"
          type="datetimerange"
          start-placeholder="创建开始"
          end-placeholder="创建结束"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 340px"
          @change="onQueryChange"
        />
        <el-date-picker
          v-model="query.settledRange"
          type="datetimerange"
          start-placeholder="结算开始"
          end-placeholder="结算结束"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 340px"
          @change="onQueryChange"
        />
        <el-button type="primary" @click="onQueryChange">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table :data="list" border>
        <el-table-column prop="settlementNo" label="结算单号" width="170" />
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="userName" label="申请人" width="110" />
        <el-table-column prop="instrumentName" label="仪器名称" min-width="160" />
        <el-table-column prop="departmentName" label="申请部门" width="130" />
        <el-table-column prop="finalAmount" label="金额" width="100" />
        <el-table-column label="状态" width="130">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.settleStatus)">{{ statusLabel(row.settleStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="结算时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.settledTime) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column label="操作" min-width="320">
          <template #default="{ row }">
            <div class="action-wrap">
              <el-button link type="primary" @click="showDetail(row)">详情</el-button>
              <el-button v-if="row.settleStatus === 'PENDING'" link type="success" @click="doSettle(row)">结算完成</el-button>
              <el-button v-if="row.settleStatus === 'CONFIRMED'" link type="warning" @click="requestRefund(row)">申请退款</el-button>
              <el-button v-if="row.settleStatus === 'REFUND_PENDING'" link type="danger" @click="approveRefund(row)">审批退款</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        class="pagination"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
        :total="total"
        @current-change="load"
        @size-change="onSizeChange"
      />
    </div>

    <div class="content-card admin-table-card">
      <h3 class="admin-card-title">异常账清单</h3>
      <div class="toolbar">
        <el-select v-model="anomalyQuery.type" clearable placeholder="异常类型" style="width: 180px" @change="loadAnomalies">
          <el-option label="已完成未结算" value="COMPLETED_UNSETTLED" />
          <el-option label="待结算滞留" value="WAITING_SETTLEMENT" />
          <el-option label="已结算未支付" value="CONFIRMED_UNPAID" />
          <el-option label="已退款未冲正" value="REFUNDED_UNMATCHED" />
        </el-select>
      </div>
      <el-table :data="anomalyList" border>
        <el-table-column prop="anomalyLabel" label="异常类型" width="140" />
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="settlementNo" label="结算单号" width="180" />
        <el-table-column prop="userName" label="用户" width="120" />
        <el-table-column prop="detail" label="说明" min-width="260" />
        <el-table-column prop="createTime" label="订单创建时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="anomalyQuery.pageNum"
        v-model:page-size="anomalyQuery.pageSize"
        class="pagination"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
        :total="anomalyTotal"
        @current-change="loadAnomalies"
        @size-change="onAnomalySizeChange"
      />
    </div>

    <el-drawer v-model="detailVisible" title="结算详情" size="520px" :with-header="true">
      <div v-if="detail" class="detail-grid">
        <div><span class="k">结算单号：</span>{{ detail.settlementNo }}</div>
        <div><span class="k">订单号：</span>{{ detail.orderNo }}</div>
        <div><span class="k">申请人：</span>{{ detail.userName }}</div>
        <div><span class="k">仪器：</span>{{ detail.instrumentName }}</div>
        <div><span class="k">部门：</span>{{ detail.departmentName }}</div>
        <div><span class="k">账单类型：</span>{{ billTypeLabel(detail.billType) }}</div>
        <div><span class="k">预估金额：</span>{{ detail.estimatedAmount }}</div>
        <div><span class="k">优惠金额：</span>{{ detail.discountAmount }}</div>
        <div><span class="k">最终金额：</span>{{ detail.finalAmount }}</div>
        <div><span class="k">结算状态：</span>{{ statusLabel(detail.settleStatus) }}</div>
        <div><span class="k">订单状态：</span>{{ orderStatusLabel(detail.orderStatus) }}</div>
        <div><span class="k">支付状态：</span>{{ payStatusLabel(detail.payStatus) }}</div>
        <div><span class="k">创建时间：</span>{{ formatDateTime(detail.createTime) }}</div>
        <div><span class="k">结算时间：</span>{{ formatDateTime(detail.settledTime) }}</div>
        <div><span class="k">操作人：</span>{{ detail.operatorName || '-' }}</div>
      </div>
    </el-drawer>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminDepartments } from '../../../api/admin'
import {
  getAdminSettlementDetail,
  getAdminSettlements,
  refundAdminSettlement,
  requestRefundAdminSettlement,
  settleAdminSettlement
} from '../../../api/settlement'
import { getReconciliationAnomalies, getReconciliationOverview } from '../../../api/account'
import { formatDateTime } from '../../../utils/datetime'
import {
  billTypeLabel as billTypeLabelDict,
  orderStatusLabel as orderStatusLabelDict,
  payStatusLabel as payStatusLabelDict
} from '../../../utils/dicts'

const STATUS_LABEL = {
  PENDING: '待结算',
  CONFIRMED: '已结算',
  REFUND_PENDING: '退款待审批',
  REFUNDING: '退款处理中',
  REFUNDED: '已退款',
  VOID: '已作废'
}

const STATUS_TAG = {
  PENDING: 'warning',
  CONFIRMED: 'success',
  REFUND_PENDING: 'warning',
  REFUNDING: 'info',
  REFUNDED: 'danger',
  VOID: 'info'
}

export default {
  data() {
    return {
      list: [],
      total: 0,
      departments: [],
      detailVisible: false,
      detail: null,
      overview: {},
      anomalyList: [],
      anomalyTotal: 0,
      query: {
        keyword: '',
        status: '',
        departmentId: null,
        orderId: null,
        createRange: [],
        settledRange: [],
        pageNum: 1,
        pageSize: 10
      },
      anomalyQuery: {
        type: '',
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  computed: {
    dashboardCards() {
      return [
        { label: '充值金额', value: this.num(this.overview.rechargeAmount) },
        { label: '已结算金额', value: this.num(this.overview.settledAmount) },
        { label: '已退款金额', value: this.num(this.overview.refundedAmount) },
        { label: '异常账数量', value: String((this.overview.completedButUnsettled || 0) + (this.overview.confirmedButUnpaidOrders || 0)) },
        { label: '待结算订单', value: String(this.overview.waitingSettlementOrders || 0) },
        { label: '已结算未支付', value: String(this.overview.confirmedButUnpaidOrders || 0) }
      ]
    }
  },
  async created() {
    const routeOrderId = Number(this.$route.query.orderId || '')
    if (Number.isFinite(routeOrderId) && routeOrderId > 0) {
      this.query.orderId = routeOrderId
    }
    await this.executeSafely(async () => {
      this.departments = await getAdminDepartments()
      await this.load()
      await this.loadOverview()
      await this.loadAnomalies()
    })
  },
  methods: {
    formatDateTime,
    statusLabel(value) {
      return STATUS_LABEL[value] || value || '-'
    },
    statusTag(value) {
      return STATUS_TAG[value] || 'info'
    },
    num(v) {
      const n = Number(v || 0)
      return n.toFixed(2)
    },
    billTypeLabel(value) {
      return billTypeLabelDict(value)
    },
    orderStatusLabel(value) {
      return orderStatusLabelDict(value)
    },
    payStatusLabel(value) {
      return payStatusLabelDict(value)
    },
    buildParams() {
      const [createStart, createEnd] = this.query.createRange || []
      const [settledStart, settledEnd] = this.query.settledRange || []
      return {
        keyword: this.query.keyword || undefined,
        status: this.query.status || undefined,
        departmentId: this.query.departmentId || undefined,
        orderId: this.query.orderId || undefined,
        createStart: createStart || undefined,
        createEnd: createEnd || undefined,
        settledStart: settledStart || undefined,
        settledEnd: settledEnd || undefined,
        pageNum: this.query.pageNum,
        pageSize: this.query.pageSize
      }
    },
    async executeSafely(action) {
      try {
        await action()
      } catch (error) {
        // 请求层统一提示
      }
    },
    async load() {
      await this.executeSafely(async () => {
        const page = await getAdminSettlements(this.buildParams())
        this.list = page.list || []
        this.total = page.total || 0
      })
    },
    async loadOverview() {
      await this.executeSafely(async () => {
        this.overview = await getReconciliationOverview({})
      })
    },
    async loadAnomalies() {
      await this.executeSafely(async () => {
        const page = await getReconciliationAnomalies({
          type: this.anomalyQuery.type || undefined,
          pageNum: this.anomalyQuery.pageNum,
          pageSize: this.anomalyQuery.pageSize
        })
        this.anomalyList = page.list || []
        this.anomalyTotal = page.total || 0
      })
    },
    async onQueryChange() {
      this.query.pageNum = 1
      await this.load()
      await this.loadOverview()
    },
    async onSizeChange() {
      this.query.pageNum = 1
      await this.load()
    },
    async onAnomalySizeChange() {
      this.anomalyQuery.pageNum = 1
      await this.loadAnomalies()
    },
    async resetQuery() {
      this.query.keyword = ''
      this.query.status = ''
      this.query.departmentId = null
      this.query.orderId = null
      this.query.createRange = []
      this.query.settledRange = []
      this.query.pageNum = 1
      await this.load()
      await this.loadOverview()
    },
    async showDetail(row) {
      await this.executeSafely(async () => {
        this.detail = await getAdminSettlementDetail(row.id)
        this.detailVisible = true
      })
    },
    async doSettle(row) {
      await this.executeSafely(async () => {
        await ElMessageBox.confirm(`确认对结算单【${row.settlementNo}】执行结算？`, '结算确认', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          type: 'warning'
        })
        await settleAdminSettlement(row.id)
        ElMessage.success('结算已完成')
        await this.load()
        await this.loadOverview()
        await this.loadAnomalies()
      })
    },
    async requestRefund(row) {
      await this.executeSafely(async () => {
        const { value } = await ElMessageBox.prompt('请输入退款申请原因', '申请退款', {
          confirmButtonText: '提交申请',
          cancelButtonText: '取消',
          inputValidator: (v) => {
            if (!String(v || '').trim()) {
              return '退款原因不能为空'
            }
            return true
          }
        })
        await requestRefundAdminSettlement(row.id, { comment: String(value || '').trim() })
        ElMessage.success('已提交退款申请')
        await this.load()
      })
    },
    async approveRefund(row) {
      await this.executeSafely(async () => {
        const { value } = await ElMessageBox.prompt('请输入退款审批意见', '审批退款（全额）', {
          confirmButtonText: '确认退款',
          cancelButtonText: '取消',
          inputValidator: (v) => {
            if (!String(v || '').trim()) {
              return '审批意见不能为空'
            }
            return true
          }
        })
        await refundAdminSettlement(row.id, { comment: String(value || '').trim() })
        ElMessage.success('退款已完成')
        await this.load()
        await this.loadOverview()
        await this.loadAnomalies()
      })
    }
  }
}
</script>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 12px;
}

.action-wrap {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}

.detail-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  color: #1f2a44;
  font-size: 14px;
}

.detail-grid .k {
  color: #697b9a;
}

.stat-card {
  padding: 16px 18px;
}

.stat-label {
  color: var(--muted);
  font-size: 13px;
}

.stat-value {
  margin-top: 6px;
  font-size: 24px;
  font-weight: 700;
  color: var(--primary);
}
</style>
