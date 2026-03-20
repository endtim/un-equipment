<template>
  <div class="admin-page">
    <admin-table-card
      v-if="isReportMode || isExpenseMode"
      :title="isExpenseMode ? '维护支出流水' : '经费明细报表'"
      style="margin-bottom: 14px"
    >
      <template #summary>
        <admin-summary-cards v-if="isReportMode" :items="summaryCards" style="margin-bottom: 14px" />
      </template>
      <template #toolbar>
      <div class="admin-toolbar">
        <el-input
          v-model="query.keyword"
          :placeholder="
            isExpenseMode ? '支出单号/仪器/部门/用户' : '业务单号/订单号/仪器/部门/用户'
          "
          clearable
          style="width: 260px"
          @keyup.enter="onQueryChange"
        />
        <el-select
          v-if="isReportMode"
          v-model="query.bizType"
          clearable
          placeholder="业务类型"
          style="width: 160px"
          @change="onQueryChange"
        >
          <el-option label="充值入账" value="RECHARGE" />
          <el-option label="仪器收费" value="SETTLEMENT" />
          <el-option label="订单退款" value="REFUND" />
          <el-option label="维护支出" value="MAINTENANCE_EXPENSE" />
        </el-select>
        <el-select
          v-if="isReportMode"
          v-model="query.inoutType"
          clearable
          placeholder="收支方向"
          style="width: 140px"
          @change="onQueryChange"
        >
          <el-option label="收入" value="IN" />
          <el-option label="支出" value="OUT" />
        </el-select>
        <el-select
          v-model="query.departmentId"
          clearable
          placeholder="部门"
          style="width: 180px"
          @change="onQueryChange"
        >
          <el-option
            v-for="item in departments"
            :key="item.id"
            :label="item.deptName"
            :value="item.id"
          />
        </el-select>
        <el-input-number
          v-model="query.instrumentId"
          :min="1"
          :controls="false"
          placeholder="仪器ID"
          style="width: 130px"
        />
        <el-date-picker
          v-model="query.timeRange"
          type="datetimerange"
          start-placeholder="开始时间"
          end-placeholder="结束时间"
          value-format="YYYY-MM-DDTHH:mm:ss"
          style="width: 340px"
          @change="onQueryChange"
        />
        <el-button type="primary" @click="onQueryChange">查询</el-button>
        <el-button v-if="isReportMode" type="success" plain @click="exportCsv">导出CSV</el-button>
        <el-button v-if="isExpenseMode" type="primary" plain @click="openExpenseDialog"
          >新增支出</el-button
        >
        <el-button @click="resetQuery">重置</el-button>
      </div>
      </template>

      <el-table :data="list" border>
        <el-table-column prop="bizTypeLabel" label="业务类型" width="120" />
        <el-table-column prop="bizNo" label="业务单号" width="180" />
        <el-table-column prop="orderNo" label="订单号" width="170" />
        <el-table-column prop="instrumentName" label="仪器名称" min-width="160" />
        <el-table-column prop="departmentName" label="部门" width="130" />
        <el-table-column prop="userName" label="用户" width="110" />
        <el-table-column label="收支" width="90">
          <template #default="{ row }">
            <status-tag
              :label="row.inoutTypeLabel"
              :type="row.inoutType === 'IN' ? 'success' : 'danger'"
            />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ row }">{{ formatAmount(row.amount) }}</template>
        </el-table-column>
        <el-table-column label="发生时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.occurTime) }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="240" show-overflow-tooltip />
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        class="admin-pagination"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
        :total="total"
        @current-change="load"
        @size-change="onSizeChange"
      />
    </admin-table-card>

    <admin-table-card
      v-if="isBudgetWarningMode"
      title="预算预警"
      style="margin-bottom: 14px"
    >
      <template #toolbar>
      <div class="admin-toolbar">
        <el-input-number v-model="budgetYear" :min="2020" :max="2100" style="width: 120px" />
        <el-button type="primary" @click="loadBudgetWarnings">查询</el-button>
        <el-button @click="resetBudgetYear">重置</el-button>
      </div>
      </template>
      <el-table :data="budgetWarnings" border>
        <el-table-column prop="budgetNo" label="预算单号" width="170" />
        <el-table-column prop="departmentName" label="部门" width="140" />
        <el-table-column prop="instrumentName" label="仪器" min-width="160" />
        <el-table-column label="预算金额" width="120">
          <template #default="{ row }">{{ formatAmount(row.budgetAmount) }}</template>
        </el-table-column>
        <el-table-column label="已用金额" width="120">
          <template #default="{ row }">{{ formatAmount(row.usedAmount) }}</template>
        </el-table-column>
        <el-table-column label="已用比例" width="120">
          <template #default="{ row }">{{ formatAmount(row.usedRatio) }}%</template>
        </el-table-column>
        <el-table-column label="预警阈值" width="120">
          <template #default="{ row }">{{ formatAmount(row.warningRatio) }}%</template>
        </el-table-column>
        <el-table-column label="预警级别" width="120">
          <template #default="{ row }">
            <status-tag :label="warningLabel(row.warningLevel)" :type="warningTag(row.warningLevel)" />
          </template>
        </el-table-column>
      </el-table>
    </admin-table-card>

    <admin-table-card
      v-if="isBudgetLedgerMode"
      title="预算台账"
      style="margin-bottom: 14px"
    >
      <template #toolbar>
      <div class="admin-toolbar">
        <el-input-number v-model="budgetYear" :min="2020" :max="2100" style="width: 120px" />
        <el-button type="primary" @click="loadBudgets">查询</el-button>
        <el-button type="primary" plain @click="openBudgetDialog">新增预算</el-button>
        <el-button @click="resetBudgetYear">重置</el-button>
      </div>
      </template>
      <el-table :data="budgets" border>
        <el-table-column prop="budgetNo" label="预算单号" width="170" />
        <el-table-column prop="budgetYear" label="年度" width="90" />
        <el-table-column prop="departmentName" label="部门" width="140" />
        <el-table-column prop="instrumentName" label="仪器" min-width="150" />
        <el-table-column label="预算金额" width="120">
          <template #default="{ row }">{{ formatAmount(row.budgetAmount) }}</template>
        </el-table-column>
        <el-table-column label="阈值" width="100">
          <template #default="{ row }">{{ formatAmount(row.warningRatio) }}%</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="220" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-model:current-page="budgetQuery.pageNum"
        v-model:page-size="budgetQuery.pageSize"
        class="admin-pagination"
        layout="total, sizes, prev, pager, next, jumper"
        :page-sizes="[10, 20, 50]"
        :total="budgetTotal"
        @current-change="loadBudgets"
        @size-change="onBudgetSizeChange"
      />
    </admin-table-card>

    <el-dialog
      v-model="expenseDialogVisible"
      title="新增维护支出"
      width="760px"
      destroy-on-close
      @closed="resetExpenseForm"
    >
      <el-form ref="expenseFormRef" :model="expenseForm" :rules="expenseRules" label-position="top">
        <div class="grid-3">
          <el-form-item label="仪器" prop="instrumentId">
            <el-select
              v-model="expenseForm.instrumentId"
              filterable
              clearable
              placeholder="请选择仪器"
              class="full-width"
            >
              <el-option
                v-for="item in instruments"
                :key="item.id"
                :label="`${item.instrumentName}（${item.instrumentNo}）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="支出类型" prop="expenseType">
            <el-select v-model="expenseForm.expenseType" class="full-width">
              <el-option label="日常维护" value="MAINTENANCE" />
              <el-option label="故障维修" value="REPAIR" />
              <el-option label="计量校准" value="CALIBRATION" />
              <el-option label="其他" value="OTHER" />
            </el-select>
          </el-form-item>
          <el-form-item label="支出金额（元）" prop="amount">
            <el-input-number
              v-model="expenseForm.amount"
              :min="0.01"
              :precision="2"
              class="full-width"
            />
          </el-form-item>
        </div>
        <div class="grid-3">
          <el-form-item label="支出标题" prop="title">
            <el-input v-model="expenseForm.title" maxlength="200" />
          </el-form-item>
          <el-form-item label="支出时间" prop="expenseTime">
            <el-date-picker
              v-model="expenseForm.expenseTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="请选择支出时间"
              class="full-width"
            />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="expenseForm.remark" maxlength="500" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="expenseDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExpense">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="budgetDialogVisible"
      title="新增预算"
      width="760px"
      destroy-on-close
      @closed="resetBudgetForm"
    >
      <el-form ref="budgetFormRef" :model="budgetForm" :rules="budgetRules" label-position="top">
        <div class="grid-3">
          <el-form-item label="预算年度" prop="budgetYear">
            <el-input-number
              v-model="budgetForm.budgetYear"
              :min="2020"
              :max="2100"
              class="full-width"
            />
          </el-form-item>
          <el-form-item label="部门" prop="departmentId">
            <el-select
              v-model="budgetForm.departmentId"
              clearable
              placeholder="请选择部门"
              class="full-width"
              @change="onBudgetDepartmentChange"
            >
              <el-option
                v-for="item in departments"
                :key="item.id"
                :label="item.deptName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="仪器（可选）" prop="instrumentId">
            <el-select
              v-model="budgetForm.instrumentId"
              clearable
              filterable
              placeholder="请选择仪器"
              class="full-width"
            >
              <el-option
                v-for="item in filteredBudgetInstruments"
                :key="item.id"
                :label="`${item.instrumentName}（${item.instrumentNo}）`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>
        <div class="grid-3">
          <el-form-item label="预算金额（元）" prop="budgetAmount">
            <el-input-number
              v-model="budgetForm.budgetAmount"
              :min="0.01"
              :precision="2"
              class="full-width"
            />
          </el-form-item>
          <el-form-item label="预警阈值（%）" prop="warningRatio">
            <el-input-number
              v-model="budgetForm.warningRatio"
              :min="1"
              :max="100"
              :precision="2"
              class="full-width"
            />
          </el-form-item>
          <el-form-item label="备注" prop="remark">
            <el-input v-model="budgetForm.remark" maxlength="500" />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="budgetDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBudget">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import AdminSummaryCards from '../../../components/admin/AdminSummaryCards.vue'
import AdminTableCard from '../../../components/admin/AdminTableCard.vue'
import StatusTag from '../../../components/admin/StatusTag.vue'
import { getAdminDepartments, getAdminInstruments } from '../../../api/admin'
import {
  createFinanceExpense,
  exportFinanceDetails,
  getFinanceBudgetWarnings,
  getFinanceBudgetsPage,
  getFinanceDetailsPage,
  getReconciliationOverview,
  saveFinanceBudget
} from '../../../api/account'
import { formatDateTime as formatDateTimeUtil } from '../../../utils/datetime'

function defaultExpenseForm() {
  return {
    instrumentId: null,
    expenseType: 'MAINTENANCE',
    amount: 0,
    title: '',
    expenseTime: dayjs().format('YYYY-MM-DDTHH:mm:ss'),
    remark: ''
  }
}

function defaultBudgetForm() {
  return {
    id: null,
    budgetYear: dayjs().year(),
    departmentId: null,
    instrumentId: null,
    budgetAmount: 0,
    warningRatio: 80,
    remark: ''
  }
}

export default {
  components: {
    AdminSummaryCards,
    AdminTableCard,
    StatusTag
  },
  props: {
    mode: {
      type: String,
      default: 'report'
    }
  },
  data() {
    return {
      departments: [],
      instruments: [],
      list: [],
      total: 0,
      overview: {},
      budgetYear: dayjs().year(),
      budgets: [],
      budgetTotal: 0,
      budgetWarnings: [],
      query: {
        keyword: '',
        bizType: '',
        inoutType: '',
        departmentId: null,
        instrumentId: null,
        timeRange: [],
        pageNum: 1,
        pageSize: 10
      },
      budgetQuery: {
        pageNum: 1,
        pageSize: 10
      },
      expenseForm: defaultExpenseForm(),
      expenseDialogVisible: false,
      budgetDialogVisible: false,
      budgetForm: defaultBudgetForm(),
      expenseRules: {
        instrumentId: [{ required: true, message: '请选择仪器', trigger: 'change' }],
        expenseType: [{ required: true, message: '请选择支出类型', trigger: 'change' }],
        amount: [{ required: true, message: '请输入支出金额', trigger: 'change' }],
        title: [
          { required: true, message: '请输入支出标题', trigger: 'blur' },
          { min: 2, max: 200, message: '支出标题长度为2-200字符', trigger: 'blur' }
        ],
        expenseTime: [{ required: true, message: '请选择支出时间', trigger: 'change' }]
      },
      budgetRules: {
        budgetYear: [{ required: true, message: '请输入预算年度', trigger: 'change' }],
        departmentId: [{ required: true, message: '请选择部门', trigger: 'change' }],
        budgetAmount: [{ required: true, message: '请输入预算金额', trigger: 'change' }],
        warningRatio: [{ required: true, message: '请输入预警阈值', trigger: 'change' }]
      }
    }
  },
  computed: {
    isReportMode() {
      return this.mode === 'report'
    },
    isExpenseMode() {
      return this.mode === 'expense'
    },
    isBudgetWarningMode() {
      return this.mode === 'budget-warning'
    },
    isBudgetLedgerMode() {
      return this.mode === 'budget-ledger'
    },
    filteredBudgetInstruments() {
      const departmentId = this.budgetForm.departmentId
      if (!departmentId) {
        return this.instruments
      }
      return this.instruments.filter(
        (item) => this.resolveInstrumentDepartmentId(item) === departmentId
      )
    },
    summaryCards() {
      return [
        { label: '充值金额', value: this.formatAmount(this.overview.rechargeAmount) },
        { label: '已结算金额', value: this.formatAmount(this.overview.settledAmount) },
        { label: '退款金额', value: this.formatAmount(this.overview.refundedAmount) },
        {
          label: '维护支出',
          value: this.formatAmount(this.overview.maintenanceExpenseAmount)
        },
        { label: '净收入', value: this.formatAmount(this.overview.netIncomeAmount) }
      ]
    }
  },
  watch: {
    mode: {
      immediate: true,
      async handler() {
        await this.initializeByMode()
      }
    }
  },
  async created() {
    await this.ensureBaseOptions()
  },
  methods: {
    async ensureBaseOptions() {
      if (this.departments.length > 0 && this.instruments.length > 0) {
        return
      }
      await this.executeSafely(async () => {
        const [departments, instruments] = await Promise.all([
          getAdminDepartments(),
          getAdminInstruments({ pageNum: 1, pageSize: 200 })
        ])
        this.departments = departments || []
        this.instruments = (instruments && instruments.list) || []
      })
    },
    async initializeByMode() {
      await this.ensureBaseOptions()
      if (this.isExpenseMode) {
        this.query.bizType = 'MAINTENANCE_EXPENSE'
        this.query.inoutType = 'OUT'
        this.query.pageNum = 1
        await this.load()
        return
      }
      if (this.isBudgetWarningMode) {
        await this.loadBudgetWarnings()
        return
      }
      if (this.isBudgetLedgerMode) {
        this.budgetQuery.pageNum = 1
        await this.loadBudgets()
        return
      }
      this.query.bizType = ''
      this.query.inoutType = ''
      this.query.pageNum = 1
      await this.load()
      await this.loadOverview()
    },
    formatDateTime(value) {
      return formatDateTimeUtil(value)
    },
    formatAmount(value) {
      return Number(value || 0).toFixed(2)
    },
    warningLabel(level) {
      if (level === 'OVER_BUDGET') {
        return '超预算'
      }
      if (level === 'WARNING') {
        return '预警'
      }
      return '正常'
    },
    warningTag(level) {
      if (level === 'OVER_BUDGET') {
        return 'danger'
      }
      if (level === 'WARNING') {
        return 'warning'
      }
      return 'success'
    },
    async executeSafely(action) {
      try {
        await action()
      } catch (error) {
        // 请求层统一提示
      }
    },
    buildParams() {
      const [startTime, endTime] = this.query.timeRange || []
      const bizType = this.isExpenseMode ? 'MAINTENANCE_EXPENSE' : this.query.bizType || undefined
      const inoutType = this.isExpenseMode ? 'OUT' : this.query.inoutType || undefined
      return {
        keyword: this.query.keyword || undefined,
        bizType,
        inoutType,
        departmentId: this.query.departmentId || undefined,
        instrumentId: this.query.instrumentId || undefined,
        startTime: startTime || undefined,
        endTime: endTime || undefined,
        pageNum: this.query.pageNum,
        pageSize: this.query.pageSize
      }
    },
    resolveInstrumentDepartmentId(item) {
      return Number(
        item?.departmentId ??
          item?.deptId ??
          item?.ownerDepartmentId ??
          item?.belongDepartmentId ??
          0
      )
    },
    onBudgetDepartmentChange() {
      if (!this.budgetForm.instrumentId) {
        return
      }
      const selected = this.instruments.find((item) => item.id === this.budgetForm.instrumentId)
      if (!selected) {
        this.budgetForm.instrumentId = null
        return
      }
      const departmentId = Number(this.budgetForm.departmentId || 0)
      if (departmentId > 0 && this.resolveInstrumentDepartmentId(selected) !== departmentId) {
        this.budgetForm.instrumentId = null
      }
    },
    async load() {
      await this.executeSafely(async () => {
        const page = await getFinanceDetailsPage(this.buildParams())
        this.list = page.list || []
        this.total = page.total || 0
      })
    },
    async loadOverview() {
      if (!this.isReportMode) {
        return
      }
      await this.executeSafely(async () => {
        const [startTime, endTime] = this.query.timeRange || []
        this.overview = await getReconciliationOverview({
          startTime: startTime || undefined,
          endTime: endTime || undefined
        })
      })
    },
    async loadBudgets() {
      await this.executeSafely(async () => {
        const page = await getFinanceBudgetsPage({
          budgetYear: this.budgetYear,
          pageNum: this.budgetQuery.pageNum,
          pageSize: this.budgetQuery.pageSize
        })
        this.budgets = page.list || []
        this.budgetTotal = page.total || 0
      })
    },
    async loadBudgetWarnings() {
      await this.executeSafely(async () => {
        this.budgetWarnings = await getFinanceBudgetWarnings({ budgetYear: this.budgetYear })
      })
    },
    async onQueryChange() {
      this.query.pageNum = 1
      await this.load()
      if (this.isReportMode) {
        await this.loadOverview()
      }
    },
    async onSizeChange() {
      this.query.pageNum = 1
      await this.load()
    },
    async onBudgetSizeChange() {
      this.budgetQuery.pageNum = 1
      await this.loadBudgets()
    },
    resetBudgetYear() {
      this.budgetYear = dayjs().year()
      if (this.isBudgetWarningMode) {
        this.loadBudgetWarnings()
      } else if (this.isBudgetLedgerMode) {
        this.budgetQuery.pageNum = 1
        this.loadBudgets()
      }
    },
    async resetQuery() {
      this.query.keyword = ''
      this.query.departmentId = null
      this.query.instrumentId = null
      this.query.timeRange = []
      this.query.pageNum = 1
      if (this.isExpenseMode) {
        this.query.bizType = 'MAINTENANCE_EXPENSE'
        this.query.inoutType = 'OUT'
      } else {
        this.query.bizType = ''
        this.query.inoutType = ''
      }
      await this.load()
      if (this.isReportMode) {
        await this.loadOverview()
      }
    },
    openExpenseDialog() {
      this.resetExpenseForm()
      this.expenseDialogVisible = true
    },
    async exportCsv() {
      await this.executeSafely(async () => {
        const params = this.buildParams()
        delete params.pageNum
        delete params.pageSize
        const blob = await exportFinanceDetails(params)
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.href = url
        link.download = '经费明细报表.csv'
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
        ElMessage.success('CSV导出成功')
      })
    },
    resetExpenseForm() {
      this.expenseForm = defaultExpenseForm()
      this.$nextTick(() => {
        if (this.$refs.expenseFormRef) {
          this.$refs.expenseFormRef.clearValidate()
        }
      })
    },
    async submitExpense() {
      await this.$refs.expenseFormRef.validate()
      await this.executeSafely(async () => {
        await createFinanceExpense({
          ...this.expenseForm,
          amount: Number(this.expenseForm.amount || 0)
        })
        ElMessage.success('维护支出登记成功')
        this.expenseDialogVisible = false
        await this.load()
      })
    },
    resetBudgetForm() {
      this.budgetForm = defaultBudgetForm()
      this.$nextTick(() => {
        if (this.$refs.budgetFormRef) {
          this.$refs.budgetFormRef.clearValidate()
        }
      })
    },
    openBudgetDialog() {
      this.resetBudgetForm()
      this.budgetForm.budgetYear = this.budgetYear
      this.budgetDialogVisible = true
    },
    async submitBudget() {
      await this.$refs.budgetFormRef.validate()
      await this.executeSafely(async () => {
        await saveFinanceBudget({
          ...this.budgetForm,
          budgetAmount: Number(this.budgetForm.budgetAmount || 0),
          warningRatio: Number(this.budgetForm.warningRatio || 0)
        })
        ElMessage.success('预算保存成功')
        this.budgetDialogVisible = false
        if (this.isBudgetLedgerMode) {
          await this.loadBudgets()
        } else if (this.isBudgetWarningMode) {
          await this.loadBudgetWarnings()
        }
      })
    }
  }
}
</script>

<style scoped>
.full-width {
  width: 100%;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
