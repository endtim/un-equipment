<template>
  <div class="admin-page">
    <div class="content-card admin-table-card">
      <div class="admin-toolbar">
        <el-select
          v-model="query.instrumentId"
          clearable
          filterable
          placeholder="请选择仪器"
          style="width: 280px"
        >
          <el-option
            v-for="item in instruments"
            :key="item.id"
            :label="item.instrumentName || item.name"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="query.weekDay" clearable placeholder="星期" style="width: 140px">
          <el-option v-for="item in weekOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
        <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button type="primary" plain @click="openCreate">新增规则</el-button>
      </div>

      <el-table :data="rules" border>
        <el-table-column label="仪器" min-width="220">
          <template #default="{ row }">{{ instrumentName(row.instrumentId) }}</template>
        </el-table-column>
        <el-table-column label="星期" width="180">
          <template #default="{ row }">{{ weekLabels(row) }}</template>
        </el-table-column>
        <el-table-column label="开始时间" prop="startTime" width="120" />
        <el-table-column label="结束时间" prop="endTime" width="120" />
        <el-table-column label="最长预约时长(分钟)" prop="maxReserveMinutes" width="160" />
        <el-table-column label="时间步长(分钟)" prop="stepMinutes" width="150" />
        <el-table-column label="生效开始" prop="effectiveStartDate" width="130" />
        <el-table-column label="生效结束" prop="effectiveEndDate" width="130" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'">
              {{ row.status === 'ENABLED' ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button link @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="admin-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :page-sizes="pageSizeOptions"
          :total="total"
          @size-change="changePageSize"
          @current-change="changePage"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑开放规则' : '新增开放规则'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <div class="grid-3">
          <el-form-item label="仪器" prop="instrumentId">
            <el-select v-model="form.instrumentId" filterable class="full-width">
              <el-option
                v-for="item in instruments"
                :key="item.id"
                :label="item.instrumentName || item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="星期" prop="weekDays">
            <el-select
              v-model="form.weekDays"
              class="full-width"
              multiple
              collapse-tags
              collapse-tags-tooltip
              placeholder="请选择星期"
            >
              <el-option v-for="item in weekOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" class="full-width">
              <el-option label="启用" value="ENABLED" />
              <el-option label="停用" value="DISABLED" />
            </el-select>
          </el-form-item>
        </div>

        <div class="grid-3">
          <el-form-item label="开始时间" prop="startTime">
            <el-time-picker
              v-model="form.startTime"
              value-format="HH:mm"
              format="HH:mm"
              class="full-width"
              placeholder="开始时间"
            />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-time-picker
              v-model="form.endTime"
              value-format="HH:mm"
              format="HH:mm"
              class="full-width"
              placeholder="结束时间"
            />
          </el-form-item>
          <el-form-item label="时间步长(分钟)" prop="stepMinutes">
            <el-input-number v-model="form.stepMinutes" :min="1" :step="1" class="full-width" />
          </el-form-item>
        </div>

        <div class="grid-3">
          <el-form-item label="最长预约时长(分钟)" prop="maxReserveMinutes">
            <el-input-number v-model="form.maxReserveMinutes" :min="1" :step="1" class="full-width" />
          </el-form-item>
          <el-form-item label="生效开始日期" prop="effectiveStartDate">
            <el-date-picker
              v-model="form.effectiveStartDate"
              type="date"
              value-format="YYYY-MM-DD"
              class="full-width"
            />
          </el-form-item>
          <el-form-item label="生效结束日期" prop="effectiveEndDate">
            <el-date-picker
              v-model="form.effectiveEndDate"
              type="date"
              value-format="YYYY-MM-DD"
              class="full-width"
            />
          </el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createOpenRule,
  deleteOpenRule,
  getAdminInstruments,
  getAdminOpenRulesPage,
  updateOpenRule
} from '../../../api/admin'

function defaultForm() {
  return {
    id: null,
    instrumentId: null,
    weekDays: [1],
    startTime: '08:30',
    endTime: '17:30',
    maxReserveMinutes: 480,
    stepMinutes: 30,
    effectiveStartDate: '',
    effectiveEndDate: '',
    status: 'ENABLED'
  }
}

export default {
  data() {
    return {
      rules: [],
      instruments: [],
      total: 0,
      pageSizeOptions: [10, 20, 50, 100],
      dialogVisible: false,
      form: defaultForm(),
      query: {
        instrumentId: null,
        weekDay: null,
        status: '',
        pageNum: 1,
        pageSize: 10
      },
      weekOptions: [
        { label: '周一', value: 1 },
        { label: '周二', value: 2 },
        { label: '周三', value: 3 },
        { label: '周四', value: 4 },
        { label: '周五', value: 5 },
        { label: '周六', value: 6 },
        { label: '周日', value: 7 }
      ],
      formRules: {
        instrumentId: [{ required: true, message: '请选择仪器', trigger: 'change' }],
        weekDays: [{ type: 'array', required: true, min: 1, message: '请至少选择一个星期', trigger: 'change' }],
        startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
        endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
        maxReserveMinutes: [{ required: true, message: '请输入最长预约时长', trigger: 'change' }],
        stepMinutes: [{ required: true, message: '请输入时间步长', trigger: 'change' }]
      }
    }
  },
  async created() {
    this.restoreQuery()
    await this.loadInstruments()
    await this.loadRules()
  },
  methods: {
    restoreQuery() {
      const q = this.$route.query || {}
      this.query.instrumentId = q.instrumentId ? Number(q.instrumentId) : null
      this.query.weekDay = q.weekDay ? Number(q.weekDay) : null
      this.query.status = q.status || ''
      this.query.pageNum = Number(q.pageNum || 1)
      this.query.pageSize = Number(q.pageSize || 10)
    },
    syncQuery() {
      this.$router.replace({
        path: this.$route.path,
        query: {
          instrumentId: this.query.instrumentId ? String(this.query.instrumentId) : undefined,
          weekDay: this.query.weekDay ? String(this.query.weekDay) : undefined,
          status: this.query.status || undefined,
          pageNum: this.query.pageNum > 1 ? String(this.query.pageNum) : undefined,
          pageSize: this.query.pageSize !== 10 ? String(this.query.pageSize) : undefined
        }
      })
    },
    async loadInstruments() {
      const page = await getAdminInstruments({ pageNum: 1, pageSize: 1000 })
      this.instruments = Array.isArray(page?.list) ? page.list : []
    },
    async loadRules() {
      const page = await getAdminOpenRulesPage(this.query)
      this.rules = Array.isArray(page?.list) ? page.list : []
      this.total = Number(page?.total || 0)
      this.syncQuery()
    },
    async search() {
      this.query.pageNum = 1
      await this.loadRules()
    },
    async changePage(pageNum) {
      this.query.pageNum = pageNum
      await this.loadRules()
    },
    async changePageSize(pageSize) {
      this.query.pageSize = pageSize
      this.query.pageNum = 1
      await this.loadRules()
    },
    weekLabel(weekDay) {
      const hit = this.weekOptions.find(item => item.value === weekDay)
      return hit ? hit.label : '-'
    },
    weekLabels(row) {
      const text = row.weekDays || (row.weekDay != null ? String(row.weekDay) : '')
      if (!text) return '-'
      return text
        .split(',')
        .map(item => Number(item))
        .filter(item => item >= 1 && item <= 7)
        .map(item => this.weekLabel(item))
        .join('、')
    },
    instrumentName(instrumentId) {
      const hit = this.instruments.find(item => item.id === instrumentId)
      return hit ? (hit.instrumentName || hit.name || `#${instrumentId}`) : `#${instrumentId}`
    },
    openCreate() {
      this.form = defaultForm()
      if (this.query.instrumentId) {
        this.form.instrumentId = this.query.instrumentId
      } else if (this.instruments.length) {
        this.form.instrumentId = this.instruments[0].id
      }
      this.dialogVisible = true
    },
    openEdit(row) {
      this.form = {
        id: row.id,
        instrumentId: row.instrumentId,
        weekDays: (row.weekDays || String(row.weekDay || ''))
          .split(',')
          .map(item => Number(item))
          .filter(item => item >= 1 && item <= 7),
        startTime: String(row.startTime || '').slice(0, 5),
        endTime: String(row.endTime || '').slice(0, 5),
        maxReserveMinutes: Number(row.maxReserveMinutes || 480),
        stepMinutes: Number(row.stepMinutes || 30),
        effectiveStartDate: row.effectiveStartDate || '',
        effectiveEndDate: row.effectiveEndDate || '',
        status: row.status || 'ENABLED'
      }
      this.dialogVisible = true
    },
    closeDialog() {
      this.dialogVisible = false
      this.$nextTick(() => {
        if (this.$refs.formRef) {
          this.$refs.formRef.clearValidate()
        }
      })
    },
    async submit() {
      await this.$refs.formRef.validate()
      if (this.form.startTime >= this.form.endTime) {
        ElMessage.error('结束时间必须晚于开始时间')
        return
      }
      if (Number(this.form.maxReserveMinutes) % Number(this.form.stepMinutes) !== 0) {
        ElMessage.error('最长预约时长必须是时间步长的整数倍')
        return
      }

      const weekDays = Array.from(
        new Set((this.form.weekDays || []).map(item => Number(item)).filter(item => item >= 1 && item <= 7))
      )
      if (!weekDays.length) {
        ElMessage.error('请至少选择一个星期')
        return
      }

      if (
        this.form.effectiveStartDate &&
        this.form.effectiveEndDate &&
        this.form.effectiveEndDate < this.form.effectiveStartDate
      ) {
        ElMessage.error('生效结束日期不能早于生效开始日期')
        return
      }

      const payload = {
        instrumentId: this.form.instrumentId,
        weekDay: weekDays[0],
        openDays: weekDays.join(','),
        startTime: this.form.startTime,
        endTime: this.form.endTime,
        openTimeRange: `${this.form.startTime}-${this.form.endTime}`,
        maxReserveMinutes: Number(this.form.maxReserveMinutes),
        stepMinutes: Number(this.form.stepMinutes),
        effectiveStartDate: this.form.effectiveStartDate || null,
        effectiveEndDate: this.form.effectiveEndDate || null,
        status: this.form.status
      }

      if (this.form.id) {
        await updateOpenRule(this.form.id, payload)
        ElMessage.success('开放规则已更新')
      } else {
        await createOpenRule(payload)
        ElMessage.success('开放规则已创建')
      }
      this.closeDialog()
      await this.loadRules()
    },
    async remove(row) {
      await ElMessageBox.confirm('确认删除该开放规则吗？', '删除确认', {
        type: 'warning'
      })
      await deleteOpenRule(row.id)
      ElMessage.success('开放规则已删除')
      await this.loadRules()
    }
  }
}
</script>
