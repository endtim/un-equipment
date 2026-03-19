<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <div class="page-head">
        <h3 class="admin-card-title page-title">{{ title }}</h3>
        <el-tag>{{ orderType === 'MACHINE' ? '上机流程' : '送样流程' }}</el-tag>
      </div>

      <div class="admin-summary-grid" style="margin-bottom: 14px">
        <div v-for="card in summaryCards" :key="card.label" class="admin-summary-card">
          <div class="admin-summary-label">{{ card.label }}</div>
          <div class="admin-summary-value">{{ card.value }}</div>
        </div>
      </div>

      <div class="toolbar">
        <el-input
          v-model="query.keyword"
          clearable
          placeholder="订单号/申请人/仪器名称"
          style="width: 240px"
          @keyup.enter="onQueryChange"
        />
        <el-select
          v-model="query.status"
          clearable
          placeholder="全部状态"
          style="width: 180px"
          @change="onQueryChange"
        >
          <el-option
            v-for="item in statusOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-date-picker
          v-model="query.submitRange"
          type="datetimerange"
          start-placeholder="提交开始时间"
          end-placeholder="提交结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 360px"
          @change="onQueryChange"
        />
        <el-select
          v-model="query.departmentId"
          clearable
          placeholder="申请部门"
          style="width: 170px"
          @change="onQueryChange"
        >
          <el-option
            v-for="item in departments"
            :key="item.id"
            :label="item.deptName"
            :value="item.id"
          />
        </el-select>
        <el-input
          v-model="query.auditorKeyword"
          clearable
          placeholder="处理人"
          style="width: 140px"
          @keyup.enter="onQueryChange"
        />
        <el-input-number
          v-model="query.minAmount"
          :min="0"
          :precision="2"
          :controls="false"
          placeholder="最小金额"
          style="width: 120px"
        />
        <el-input-number
          v-model="query.maxAmount"
          :min="0"
          :precision="2"
          :controls="false"
          placeholder="最大金额"
          style="width: 120px"
        />
        <el-button type="primary" @click="onQueryChange">查询</el-button>
        <el-button @click="resetQuery">重置</el-button>
      </div>

      <el-table :data="orders" border>
        <el-table-column prop="orderNo" label="订单编号" width="220" />
        <el-table-column prop="userName" label="申请人" width="130" />
        <el-table-column prop="instrumentName" label="仪器名称" min-width="170" />
        <el-table-column label="预约时段" min-width="300">
          <template #default="{ row }">
            <span v-if="row.orderType === 'MACHINE'">
              {{ formatRange(row.reservedStart, row.reservedEnd) }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="180">
          <template #default="{ row }">
            {{ formatDateTime(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="210">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="120" />
        <el-table-column label="操作" min-width="460">
          <template #default="{ row }">
            <div class="action-wrap">
              <el-button
                v-if="row.status === 'PENDING_AUDIT'"
                link
                type="primary"
                @click="audit(row, 'APPROVE')"
                >审核通过</el-button
              >
              <el-button
                v-if="row.status === 'PENDING_AUDIT'"
                link
                type="danger"
                @click="audit(row, 'REJECT')"
                >驳回</el-button
              >
              <el-button
                v-if="row.orderType === 'MACHINE' && row.status === 'WAITING_USE'"
                link
                @click="checkIn(row)"
                >签到</el-button
              >
              <el-button
                v-if="row.orderType === 'MACHINE' && row.status === 'IN_USE'"
                link
                @click="finish(row)"
                >结束使用</el-button
              >
              <el-button
                v-if="row.orderType === 'SAMPLE' && row.status === 'WAITING_RECEIVE'"
                link
                @click="receive(row)"
                >接样</el-button
              >
              <el-button
                v-if="row.orderType === 'SAMPLE' && row.status === 'TESTING'"
                link
                @click="uploadResult(row)"
                >上传结果</el-button
              >
              <el-button
                v-if="row.orderType === 'SAMPLE' && row.status === 'WAITING_SETTLEMENT'"
                link
                type="primary"
                @click="adjustAmount(row)"
                >调整金额</el-button
              >
              <el-button
                v-if="row.status === 'WAITING_SETTLEMENT'"
                link
                type="success"
                @click="goSettlement(row)"
                >去结算</el-button
              >
              <el-button v-if="canClose(row)" link type="warning" @click="closeOrder(row)"
                >关闭订单</el-button
              >
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

      <el-empty v-if="orders.length === 0" description="暂无数据" class="admin-empty" />
    </div>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  orderStatusLabel as orderStatusLabelDict,
  orderStatusTagType as orderStatusTagTypeDict
} from '../../../utils/dicts'
import { getAdminDepartments } from '../../../api/admin'
import { formatDateTime as formatDateTimeUtil } from '../../../utils/datetime'
import {
  adjustAdminOrderAmount,
  auditOrder,
  checkInOrder,
  closeAdminOrder,
  finishOrder,
  getAdminOrders,
  receiveSampleOrder,
  uploadAdminOrderResult
} from '../../../api/order'

const CLOSABLE_STATUS = [
  'PENDING_AUDIT',
  'WAITING_USE',
  'IN_USE',
  'WAITING_RECEIVE',
  'TESTING',
  'WAITING_SETTLEMENT'
]

export default {
  props: {
    orderType: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      orders: [],
      total: 0,
      departments: [],
      query: {
        keyword: '',
        status: '',
        submitRange: [],
        departmentId: null,
        auditorKeyword: '',
        minAmount: null,
        maxAmount: null,
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  computed: {
    title() {
      return this.orderType === 'MACHINE' ? '上机订单管理' : '送样订单管理'
    },
    statusOptions() {
      return [
        { value: 'PENDING_AUDIT', label: '待审核' },
        { value: 'WAITING_USE', label: '待使用' },
        { value: 'IN_USE', label: '使用中' },
        { value: 'WAITING_RECEIVE', label: '待接样' },
        { value: 'TESTING', label: '测试中' },
        { value: 'WAITING_SETTLEMENT', label: '待结算' },
        { value: 'COMPLETED', label: '已完成' },
        { value: 'CANCELED', label: '已取消' },
        { value: 'REJECTED', label: '已驳回' }
      ]
    },
    summaryCards() {
      const pending = this.orders.filter((item) => item.status === 'PENDING_AUDIT').length
      const waitingSettlement = this.orders.filter(
        (item) => item.status === 'WAITING_SETTLEMENT'
      ).length
      const completed = this.orders.filter((item) => item.status === 'COMPLETED').length
      return [
        { label: '订单总数', value: this.total || 0 },
        { label: '当前页待审核', value: pending },
        { label: '当前页待结算', value: waitingSettlement },
        { label: '当前页已完成', value: completed }
      ]
    }
  },
  async created() {
    await this.loadDepartments()
    await this.load()
  },
  methods: {
    async executeSafely(action) {
      try {
        await action()
      } catch (error) {
        // 请求层已统一提示，这里仅阻断异常冒泡。
      }
    },
    async loadDepartments() {
      this.departments = await getAdminDepartments()
    },
    buildParams() {
      const [submitStart, submitEnd] = this.query.submitRange || []
      return {
        orderType: this.orderType,
        status: this.query.status || undefined,
        keyword: this.query.keyword || undefined,
        submitStart: submitStart || undefined,
        submitEnd: submitEnd || undefined,
        departmentId: this.query.departmentId || undefined,
        auditorKeyword: this.query.auditorKeyword || undefined,
        minAmount: this.query.minAmount == null ? undefined : this.query.minAmount,
        maxAmount: this.query.maxAmount == null ? undefined : this.query.maxAmount,
        pageNum: this.query.pageNum,
        pageSize: this.query.pageSize
      }
    },
    async load() {
      await this.executeSafely(async () => {
        const page = await getAdminOrders(this.buildParams())
        this.orders = page.list || []
        this.total = page.total || 0
      })
    },
    async onQueryChange() {
      this.query.pageNum = 1
      await this.load()
    },
    async onSizeChange() {
      this.query.pageNum = 1
      await this.load()
    },
    async resetQuery() {
      this.query.keyword = ''
      this.query.status = ''
      this.query.submitRange = []
      this.query.departmentId = null
      this.query.auditorKeyword = ''
      this.query.minAmount = null
      this.query.maxAmount = null
      this.query.pageNum = 1
      await this.load()
    },
    async audit(row, action) {
      await this.executeSafely(async () => {
        const comment = await this.askComment(action === 'APPROVE' ? '审核通过' : '驳回订单')
        await auditOrder(row.id, { action, comment })
        ElMessage.success('审核状态已更新')
        await this.load()
      })
    },
    async checkIn(row) {
      await this.executeSafely(async () => {
        await checkInOrder(row.id)
        ElMessage.success('已签到')
        await this.load()
      })
    },
    async receive(row) {
      await this.executeSafely(async () => {
        await receiveSampleOrder(row.id)
        ElMessage.success('样品已接收')
        await this.load()
      })
    },
    async finish(row) {
      await this.executeSafely(async () => {
        const comment = await this.askComment('结束上机')
        await finishOrder(row.id, { comment })
        ElMessage.success('订单已结束')
        await this.load()
      })
    },
    async uploadResult(row) {
      await this.executeSafely(async () => {
        const comment = await this.askComment('上传送样结果')
        await uploadAdminOrderResult(row.id, { comment })
        ElMessage.success('结果已上传')
        await this.load()
      })
    },
    async adjustAmount(row) {
      try {
        const { value } = await ElMessageBox.prompt('请输入结算金额（元）', '调整订单金额', {
          confirmButtonText: '确认',
          cancelButtonText: '取消',
          inputValue: String(row.amount ?? ''),
          inputValidator: (val) => {
            const num = Number(val)
            if (!Number.isFinite(num) || num < 0) {
              return '请输入大于等于 0 的数字'
            }
            return true
          }
        })
        const finalAmount = Number(value)
        const comment = await this.askComment('金额调整说明')
        await this.executeSafely(async () => {
          await adjustAdminOrderAmount(row.id, { finalAmount, comment })
          ElMessage.success('订单金额已调整')
          await this.load()
        })
      } catch (error) {
        // 用户取消
      }
    },
    goSettlement(row) {
      this.$router.push({ path: '/admin/settlements', query: { orderId: String(row.id) } })
    },
    canClose(row) {
      return CLOSABLE_STATUS.includes(row.status)
    },
    async closeOrder(row) {
      await this.executeSafely(async () => {
        const comment = await this.askComment('关闭订单', true)
        if (!comment) {
          return
        }
        await closeAdminOrder(row.id, { comment })
        ElMessage.success('订单已关闭')
        await this.load()
      })
    },
    async askComment(title, required = false) {
      try {
        const { value } = await ElMessageBox.prompt(
          required ? '请填写原因' : '可选填写备注',
          title,
          {
            confirmButtonText: '确认',
            cancelButtonText: '取消',
            inputValue: '',
            inputValidator: (val) => {
              if (required && !String(val || '').trim()) {
                return '原因不能为空'
              }
              return true
            }
          }
        )
        return (value || '').trim()
      } catch (error) {
        return ''
      }
    },
    statusLabel(value) {
      return orderStatusLabelDict(value)
    },
    statusTagType(value) {
      return orderStatusTagTypeDict(value)
    },
    formatDateTime(value) {
      return formatDateTimeUtil(value)
    },
    formatRange(start, end) {
      const startText = this.formatDateTime(start)
      const endText = this.formatDateTime(end)
      if (startText === '-' && endText === '-') {
        return '-'
      }
      return `${startText} ~ ${endText}`
    }
  }
}
</script>

<style scoped>
.page-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.page-title {
  margin: 0;
}

.toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.action-wrap {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.pagination {
  margin-top: 14px;
  justify-content: flex-end;
}
</style>
