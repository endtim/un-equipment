<template>
  <div class="admin-page">
    <admin-table-card title="仪器管理">
      <template #summary>
        <admin-summary-cards :items="summaryCards" style="margin-bottom: 14px" />
      </template>
      <template #toolbar>
        <div class="admin-toolbar">
          <el-input
            v-model="query.keyword"
            placeholder="请输入仪器名称或编码"
            clearable
            style="width: 260px"
          />
          <el-select v-model="query.categoryId" clearable placeholder="分类" style="width: 180px">
            <el-option
              v-for="item in categories"
              :key="item.id"
              :label="item.categoryName"
              :value="item.id"
            />
          </el-select>
          <el-select v-model="query.status" clearable placeholder="状态" style="width: 140px">
            <el-option label="正常" value="NORMAL" />
            <el-option label="禁用" value="DISABLED" />
            <el-option label="维护中" value="MAINTENANCE" />
            <el-option label="故障" value="FAULT" />
          </el-select>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button type="primary" plain @click="openCreate">新增仪器</el-button>
        </div>
      </template>

      <el-table :data="instruments" border>
        <el-table-column prop="instrumentName" label="仪器名称" min-width="180" />
        <el-table-column prop="instrumentNo" label="仪器编码" width="150" />
        <el-table-column prop="categoryName" label="分类" width="140" />
        <el-table-column prop="location" label="放置地点" min-width="160" />
        <el-table-column label="校内价格" width="110">
          <template #default="{ row }">{{
            formatPrice(row.priceInternal ?? row.machinePricePerHour)
          }}</template>
        </el-table-column>
        <el-table-column label="校外价格" width="110">
          <template #default="{ row }">{{
            formatPrice(row.priceExternal ?? row.samplePricePerItem)
          }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <status-tag
              :label="instrumentStatusLabel(row.status)"
              :type="instrumentStatusTagType(row.status)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goOpenRules(row)">开放规则</el-button>
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
      :title="form.id ? '编辑仪器' : '新增仪器'"
      width="980px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <div class="grid-3">
          <el-form-item label="仪器名称" prop="name">
            <el-input v-model="form.name" maxlength="200" />
          </el-form-item>
          <el-form-item label="仪器编码" prop="code">
            <el-input v-model="form.code" maxlength="50" />
          </el-form-item>
          <el-form-item label="仪器分类" prop="categoryId">
            <el-select v-model="form.categoryId" class="full-width">
              <el-option
                v-for="item in categories"
                :key="item.id"
                :label="item.categoryName"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>

        <div class="grid-3">
          <el-form-item label="品牌" prop="brand">
            <el-input v-model="form.brand" maxlength="100" />
          </el-form-item>
          <el-form-item label="型号" prop="model">
            <el-input v-model="form.model" maxlength="100" />
          </el-form-item>
          <el-form-item label="资产编号" prop="assetNo">
            <el-input v-model="form.assetNo" maxlength="50" />
          </el-form-item>
        </div>

        <div class="grid-3">
          <el-form-item label="放置地点" prop="location">
            <el-input v-model="form.location" maxlength="200" />
          </el-form-item>
          <el-form-item label="联系人" prop="serviceContactName">
            <el-input v-model="form.serviceContactName" maxlength="50" />
          </el-form-item>
          <el-form-item label="联系电话" prop="serviceContactPhone">
            <el-input v-model="form.serviceContactPhone" maxlength="20" />
          </el-form-item>
        </div>

        <div class="grid-3">
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" class="full-width">
              <el-option label="正常" value="NORMAL" />
              <el-option label="禁用" value="DISABLED" />
              <el-option label="维护中" value="MAINTENANCE" />
              <el-option label="故障" value="FAULT" />
            </el-select>
          </el-form-item>
          <el-form-item label="开放模式" prop="openMode">
            <el-select v-model="form.openMode" class="full-width">
              <el-option label="仅上机" value="MACHINE" />
              <el-option label="仅送样" value="SAMPLE" />
              <el-option label="上机/送样" value="BOTH" />
            </el-select>
          </el-form-item>
          <el-form-item label="开放预约" prop="openStatus">
            <el-select v-model="form.openStatus" class="full-width">
              <el-option label="开启" :value="1" />
              <el-option label="关闭" :value="0" />
            </el-select>
          </el-form-item>
        </div>

        <div class="grid-3">
          <el-form-item label="校内价格(元)" prop="priceInternal">
            <el-input-number
              v-model="form.priceInternal"
              :min="0"
              :precision="2"
              class="full-width"
            />
          </el-form-item>
          <el-form-item label="校外价格(元)" prop="priceExternal">
            <el-input-number
              v-model="form.priceExternal"
              :min="0"
              :precision="2"
              class="full-width"
            />
          </el-form-item>
          <el-form-item label="是否需要审核" prop="needAudit">
            <el-select v-model="form.needAudit" class="full-width">
              <el-option label="是" :value="1" />
              <el-option label="否" :value="0" />
            </el-select>
          </el-form-item>
        </div>

        <div class="grid-3">
          <el-form-item label="最短预约时长(分钟)" prop="minReserveMinutes">
            <el-input-number
              v-model="form.minReserveMinutes"
              :min="1"
              :step="1"
              class="full-width"
            />
          </el-form-item>
          <el-form-item label="最长预约时长(分钟)" prop="maxReserveMinutes">
            <el-input-number
              v-model="form.maxReserveMinutes"
              :min="1"
              :step="1"
              class="full-width"
            />
          </el-form-item>
          <el-form-item label="时间步长(分钟)" prop="stepMinutes">
            <el-input-number v-model="form.stepMinutes" :min="1" :step="1" class="full-width" />
          </el-form-item>
        </div>

        <el-form-item label="简介" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="使用说明" prop="usageDesc">
          <el-input v-model="form.usageDesc" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="送样说明" prop="sampleDesc">
          <el-input v-model="form.sampleDesc" type="textarea" :rows="3" />
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
import AdminSummaryCards from '../../../components/admin/AdminSummaryCards.vue'
import AdminTableCard from '../../../components/admin/AdminTableCard.vue'
import StatusTag from '../../../components/admin/StatusTag.vue'
import {
  createInstrument,
  deleteInstrument,
  getAdminInstrumentCategories,
  getAdminInstruments,
  updateInstrument
} from '../../../api/admin'
import { instrumentStatusLabel, instrumentStatusTagType } from '../../../utils/dicts'

function defaultForm() {
  return {
    id: null,
    name: '',
    code: '',
    model: '',
    brand: '',
    assetNo: '',
    categoryId: null,
    location: '',
    description: '',
    usageDesc: '',
    sampleDesc: '',
    serviceContactName: '',
    serviceContactPhone: '',
    priceInternal: 0,
    priceExternal: 0,
    status: 'NORMAL',
    openMode: 'BOTH',
    openStatus: 1,
    needAudit: 1,
    minReserveMinutes: 30,
    maxReserveMinutes: 480,
    stepMinutes: 30
  }
}

export default {
  components: {
    AdminSummaryCards,
    AdminTableCard,
    StatusTag
  },
  computed: {
    summaryCards() {
      const normal = this.instruments.filter((item) => item.status === 'NORMAL').length
      const maintain = this.instruments.filter((item) => item.status === 'MAINTENANCE').length
      const unavailable = this.instruments.filter((item) =>
        ['DISABLED', 'FAULT'].includes(item.status)
      ).length
      return [
        { label: '仪器总数', value: this.total || 0 },
        { label: '当前页正常', value: normal },
        { label: '当前页维护中', value: maintain },
        { label: '当前页不可用', value: unavailable }
      ]
    }
  },
  data() {
    return {
      categories: [],
      instruments: [],
      total: 0,
      pageSizeOptions: [10, 20, 50, 100],
      dialogVisible: false,
      query: {
        keyword: '',
        categoryId: null,
        status: '',
        pageNum: 1,
        pageSize: 10
      },
      form: defaultForm(),
      rules: {
        name: [
          { required: true, message: '请输入仪器名称', trigger: 'blur' },
          { min: 2, max: 200, message: '仪器名称长度为2-200字符', trigger: 'blur' }
        ],
        categoryId: [{ required: true, message: '请选择仪器分类', trigger: 'change' }],
        serviceContactPhone: [
          { pattern: /^[0-9+\-() ]{0,20}$/, message: '联系电话格式不正确', trigger: 'blur' }
        ],
        priceInternal: [{ required: true, message: '请输入校内价格', trigger: 'change' }],
        priceExternal: [{ required: true, message: '请输入校外价格', trigger: 'change' }],
        minReserveMinutes: [{ required: true, message: '请输入最短预约时长', trigger: 'change' }],
        maxReserveMinutes: [{ required: true, message: '请输入最长预约时长', trigger: 'change' }],
        stepMinutes: [{ required: true, message: '请输入时间步长', trigger: 'change' }]
      }
    }
  },
  async created() {
    this.restoreQuery()
    await this.load()
  },
  methods: {
    instrumentStatusLabel,
    instrumentStatusTagType,
    restoreQuery() {
      const query = this.$route.query || {}
      this.query.keyword = query.keyword || ''
      this.query.categoryId = query.categoryId ? Number(query.categoryId) : null
      this.query.status = query.status || ''
      this.query.pageNum = Number(query.pageNum || 1)
      this.query.pageSize = Number(query.pageSize || 10)
    },
    syncQuery() {
      this.$router.replace({
        path: this.$route.path,
        query: {
          keyword: this.query.keyword || undefined,
          categoryId: this.query.categoryId || undefined,
          status: this.query.status || undefined,
          pageNum: this.query.pageNum > 1 ? String(this.query.pageNum) : undefined,
          pageSize: this.query.pageSize !== 10 ? String(this.query.pageSize) : undefined
        }
      })
    },
    async load() {
      const [categories, page] = await Promise.all([
        getAdminInstrumentCategories(),
        getAdminInstruments(this.query)
      ])
      this.categories = categories || []
      this.instruments = page.list || []
      this.total = page.total || 0
      this.syncQuery()
    },
    async search() {
      this.query.pageNum = 1
      await this.load()
    },
    async changePage(pageNum) {
      this.query.pageNum = pageNum
      await this.load()
    },
    async changePageSize(pageSize) {
      this.query.pageSize = pageSize
      this.query.pageNum = 1
      await this.load()
    },
    openCreate() {
      this.form = defaultForm()
      if (this.categories.length) {
        this.form.categoryId = this.categories[0].id
      }
      this.dialogVisible = true
    },
    openEdit(row) {
      this.form = {
        id: row.id,
        name: row.instrumentName || row.name,
        code: row.instrumentNo || row.code,
        model: row.model || '',
        brand: row.brand || '',
        assetNo: row.assetNo || '',
        categoryId: row.categoryId,
        location: row.location || '',
        description: row.intro || row.description || '',
        usageDesc: row.usageDesc || '',
        sampleDesc: row.sampleDesc || '',
        serviceContactName: row.serviceContactName || '',
        serviceContactPhone: row.serviceContactPhone || '',
        priceInternal: Number(row.priceInternal ?? row.machinePricePerHour ?? 0),
        priceExternal: Number(row.priceExternal ?? row.samplePricePerItem ?? 0),
        status: row.status || 'NORMAL',
        openMode: row.openMode || 'BOTH',
        openStatus: Number(row.openStatus ?? 1),
        needAudit: Number(row.needAudit ?? 1),
        minReserveMinutes: Number(row.minReserveMinutes || 30),
        maxReserveMinutes: Number(row.maxReserveMinutes || 480),
        stepMinutes: Number(row.stepMinutes || 30)
      }
      this.dialogVisible = true
    },
    goOpenRules(row) {
      this.$router.push({
        path: '/admin/open-rules',
        query: { instrumentId: String(row.id) }
      })
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
      if (Number(this.form.maxReserveMinutes) < Number(this.form.minReserveMinutes)) {
        ElMessage.error('最长预约时长不能小于最短预约时长')
        return
      }
      if (
        Number(this.form.minReserveMinutes) % Number(this.form.stepMinutes) !== 0 ||
        Number(this.form.maxReserveMinutes) % Number(this.form.stepMinutes) !== 0
      ) {
        ElMessage.error('预约时长必须是时间步长的整数倍')
        return
      }

      const payload = {
        ...this.form,
        priceInternal: Number(this.form.priceInternal || 0),
        priceExternal: Number(this.form.priceExternal || 0),
        machinePricePerHour: Number(this.form.priceInternal || 0),
        samplePricePerItem: Number(this.form.priceExternal || 0),
        openStatus: Number(this.form.openStatus || 0),
        needAudit: Number(this.form.needAudit || 0),
        minReserveMinutes: Number(this.form.minReserveMinutes || 0),
        maxReserveMinutes: Number(this.form.maxReserveMinutes || 0),
        stepMinutes: Number(this.form.stepMinutes || 0)
      }
      if (this.form.id) {
        await updateInstrument(this.form.id, payload)
        ElMessage.success('仪器已更新')
      } else {
        await createInstrument(payload)
        ElMessage.success('仪器已创建')
      }
      this.closeDialog()
      await this.load()
    },
    async remove(row) {
      await ElMessageBox.confirm(`确认删除仪器“${row.instrumentName}”吗？`, '删除确认')
      await deleteInstrument(row.id)
      ElMessage.success('仪器已删除')
      await this.load()
    },
    formatPrice(value) {
      return Number(value || 0).toFixed(2)
    }
  }
}
</script>

<style scoped>
.admin-toolbar {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.admin-pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.full-width {
  width: 100%;
}
</style>
