<template>
  <div class="admin-page">
    <admin-table-card title="维护记录">
      <template #toolbar>
        <div class="admin-toolbar maintenance-toolbar">
          <el-select
            v-model="query.instrumentId"
            clearable
            filterable
            placeholder="请选择仪器"
            class="maintenance-filter maintenance-filter--lg"
          >
            <el-option
              v-for="item in instruments"
              :key="item.id"
              :label="item.instrumentName || item.name"
              :value="item.id"
            />
          </el-select>
          <el-select
            v-model="query.status"
            clearable
            placeholder="维护状态"
            class="maintenance-filter maintenance-filter--sm"
          >
            <el-option label="待处理" value="PENDING" />
            <el-option label="处理中" value="PROCESSING" />
            <el-option label="已完成" value="FINISHED" />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button type="primary" plain @click="openCreate">新增记录</el-button>
        </div>
      </template>

      <el-table :data="records" border>
        <el-table-column label="仪器" min-width="220">
          <template #default="{ row }">
            {{ row.instrumentName || instrumentName(row.instrumentId) }}
          </template>
        </el-table-column>
        <el-table-column label="维护类型" width="120">
          <template #default="{ row }">{{ maintTypeLabel(row.maintType) }}</template>
        </el-table-column>
        <el-table-column prop="title" label="维护标题" min-width="180" />
        <el-table-column label="开始时间" prop="startTime" width="180" />
        <el-table-column label="结束时间" prop="endTime" width="180">
          <template #default="{ row }">{{ row.endTime || '未设置' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <status-tag :label="maintStatusLabel(row.status)" :type="maintStatusType(row.status)" />
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="120">
          <template #default="{ row }">{{ row.operatorUserName || '-' }}</template>
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
    </admin-table-card>

    <el-dialog
      v-model="dialogVisible"
      :title="form.id ? '编辑维护记录' : '新增维护记录'"
      width="760px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-position="top">
        <div class="grid-3">
          <el-form-item label="仪器" prop="instrumentId">
            <el-select v-model="form.instrumentId" filterable class="full-width" :disabled="Boolean(form.id)">
              <el-option
                v-for="item in instruments"
                :key="item.id"
                :label="item.instrumentName || item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="维护类型" prop="maintType">
            <el-select v-model="form.maintType" class="full-width">
              <el-option label="维护保养" value="MAINTENANCE" />
              <el-option label="故障处理" value="FAULT" />
              <el-option label="维修" value="REPAIR" />
              <el-option label="校准" value="CALIBRATION" />
            </el-select>
          </el-form-item>
          <el-form-item label="维护状态" prop="status">
            <el-select v-model="form.status" class="full-width">
              <el-option label="待处理" value="PENDING" />
              <el-option label="处理中" value="PROCESSING" />
              <el-option label="已完成" value="FINISHED" />
            </el-select>
          </el-form-item>
        </div>

        <div class="grid-2">
          <el-form-item label="开始时间" prop="startTime">
            <el-date-picker
              v-model="form.startTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              class="full-width"
            />
          </el-form-item>
          <el-form-item label="结束时间" prop="endTime">
            <el-date-picker
              v-model="form.endTime"
              type="datetime"
              value-format="YYYY-MM-DDTHH:mm:ss"
              class="full-width"
              clearable
            />
          </el-form-item>
        </div>

        <el-form-item label="维护标题" prop="title">
          <el-input v-model="form.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="维护说明" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
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
import AdminTableCard from '../../../components/admin/AdminTableCard.vue'
import StatusTag from '../../../components/admin/StatusTag.vue'
import {
  createMaintenanceRecord,
  deleteMaintenanceRecord,
  getAdminInstruments,
  getAdminMaintenanceRecordsPage,
  updateMaintenanceRecord
} from '../../../api/admin'

function defaultForm() {
  return {
    id: null,
    instrumentId: null,
    maintType: 'MAINTENANCE',
    title: '',
    content: '',
    startTime: '',
    endTime: '',
    status: 'PENDING'
  }
}

export default {
  components: {
    AdminTableCard,
    StatusTag
  },
  data() {
    return {
      records: [],
      instruments: [],
      total: 0,
      pageSizeOptions: [10, 20, 50, 100],
      dialogVisible: false,
      form: defaultForm(),
      query: {
        instrumentId: null,
        status: '',
        pageNum: 1,
        pageSize: 10
      },
      formRules: {
        instrumentId: [{ required: true, message: '请选择仪器', trigger: 'change' }],
        maintType: [{ required: true, message: '请选择维护类型', trigger: 'change' }],
        title: [{ required: true, message: '请输入维护标题', trigger: 'blur' }],
        startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
        status: [{ required: true, message: '请选择维护状态', trigger: 'change' }]
      }
    }
  },
  async created() {
    this.restoreQuery()
    await this.loadInstruments()
    await this.loadRecords()
  },
  methods: {
    restoreQuery() {
      const q = this.$route.query || {}
      this.query.instrumentId = q.instrumentId ? Number(q.instrumentId) : null
      this.query.status = q.status || ''
      this.query.pageNum = Number(q.pageNum || 1)
      this.query.pageSize = Number(q.pageSize || 10)
    },
    syncQuery() {
      this.$router.replace({
        path: this.$route.path,
        query: {
          instrumentId: this.query.instrumentId ? String(this.query.instrumentId) : undefined,
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
    async loadRecords() {
      const page = await getAdminMaintenanceRecordsPage(this.query)
      this.records = Array.isArray(page?.list) ? page.list : []
      this.total = Number(page?.total || 0)
      this.syncQuery()
    },
    async search() {
      this.query.pageNum = 1
      await this.loadRecords()
    },
    async changePage(pageNum) {
      this.query.pageNum = pageNum
      await this.loadRecords()
    },
    async changePageSize(pageSize) {
      this.query.pageSize = pageSize
      this.query.pageNum = 1
      await this.loadRecords()
    },
    maintTypeLabel(value) {
      return (
        {
          MAINTENANCE: '维护保养',
          FAULT: '故障处理',
          REPAIR: '维修',
          CALIBRATION: '校准'
        }[value] || value || '-'
      )
    },
    maintStatusLabel(value) {
      return (
        {
          PENDING: '待处理',
          PROCESSING: '处理中',
          FINISHED: '已完成'
        }[value] || value || '-'
      )
    },
    maintStatusType(value) {
      return (
        {
          PENDING: 'info',
          PROCESSING: 'warning',
          FINISHED: 'success'
        }[value] || 'info'
      )
    },
    instrumentName(instrumentId) {
      const hit = this.instruments.find((item) => item.id === instrumentId)
      return hit ? hit.instrumentName || hit.name || `#${instrumentId}` : `#${instrumentId}`
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
        maintType: row.maintType || 'MAINTENANCE',
        title: row.title || '',
        content: row.content || '',
        startTime: row.startTime || '',
        endTime: row.endTime || '',
        status: row.status || 'PENDING'
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
      if (this.form.endTime && this.form.endTime <= this.form.startTime) {
        ElMessage.error('结束时间必须晚于开始时间')
        return
      }
      if (this.form.status === 'FINISHED' && !this.form.endTime) {
        ElMessage.error('已完成维护必须填写结束时间')
        return
      }
      const payload = {
        instrumentId: this.form.instrumentId,
        maintType: this.form.maintType,
        title: this.form.title,
        content: this.form.content || '',
        startTime: this.form.startTime,
        endTime: this.form.endTime || null,
        status: this.form.status
      }
      if (this.form.id) {
        await updateMaintenanceRecord(this.form.id, payload)
        ElMessage.success('维护记录已更新')
      } else {
        await createMaintenanceRecord(payload)
        ElMessage.success('维护记录已创建')
      }
      this.closeDialog()
      await this.loadRecords()
    },
    async remove(row) {
      await ElMessageBox.confirm('确认删除该维护记录吗？', '删除确认', {
        type: 'warning'
      })
      await deleteMaintenanceRecord(row.id)
      ElMessage.success('维护记录已删除')
      await this.loadRecords()
    }
  }
}
</script>

<style scoped>
.maintenance-toolbar {
  align-items: center;
}

.maintenance-filter--lg {
  width: 300px;
}

.maintenance-filter--sm {
  width: 160px;
}

.grid-2 {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 1200px) {
  .maintenance-filter--lg,
  .maintenance-filter--sm {
    width: 220px;
  }
}

@media (max-width: 900px) {
  .maintenance-filter--lg,
  .maintenance-filter--sm,
  .grid-2 {
    width: 100%;
  }

  .grid-2 {
    grid-template-columns: 1fr;
  }
}
</style>
