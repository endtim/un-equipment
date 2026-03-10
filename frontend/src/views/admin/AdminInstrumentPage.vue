<template>
  <div>
    <div class="grid-3" style="margin-bottom: 20px;">
      <div class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">{{ categoryForm.id ? 'Edit Category' : 'Create Category' }}</div>
        <el-form :model="categoryForm" label-position="top">
          <el-form-item label="Category Name">
            <el-input v-model="categoryForm.categoryName" />
          </el-form-item>
          <el-form-item label="Category Code">
            <el-input v-model="categoryForm.categoryCode" />
          </el-form-item>
          <div style="display: flex; gap: 8px;">
            <el-button type="primary" @click="submitCategory">Save</el-button>
            <el-button v-if="categoryForm.id" @click="resetCategory">Cancel</el-button>
          </div>
        </el-form>
      </div>
      <div class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">{{ instrumentForm.id ? 'Edit Instrument' : 'Create Instrument' }}</div>
        <el-form :model="instrumentForm" label-position="top">
          <el-form-item label="Name">
            <el-input v-model="instrumentForm.name" />
          </el-form-item>
          <el-form-item label="Code">
            <el-input v-model="instrumentForm.code" />
          </el-form-item>
          <el-form-item label="Category">
            <el-select v-model="instrumentForm.categoryId" style="width: 100%;">
              <el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="Location">
            <el-input v-model="instrumentForm.location" />
          </el-form-item>
          <el-form-item label="Description">
            <el-input v-model="instrumentForm.description" type="textarea" :rows="3" />
          </el-form-item>
          <div style="display: flex; gap: 8px;">
            <el-button type="primary" @click="submitInstrument">Save</el-button>
            <el-button v-if="instrumentForm.id" @click="resetInstrument">Cancel</el-button>
          </div>
        </el-form>
      </div>
      <div class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">{{ ruleForm.id ? 'Edit Open Rule' : 'Create Open Rule' }}</div>
        <el-form :model="ruleForm" label-position="top">
          <el-form-item label="Instrument">
            <el-select v-model="ruleForm.instrumentId" style="width: 100%;">
              <el-option v-for="item in instrumentOptions" :key="item.id" :label="item.instrumentName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="Week Day">
            <el-select v-model="ruleForm.openDays" style="width: 100%;">
              <el-option v-for="item in weekDays" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="Time Range">
            <el-input v-model="ruleForm.openTimeRange" placeholder="09:00-17:00" />
          </el-form-item>
          <div style="display: flex; gap: 8px;">
            <el-button type="primary" @click="submitRule">Save</el-button>
            <el-button v-if="ruleForm.id" @click="resetRule">Cancel</el-button>
          </div>
        </el-form>
      </div>
    </div>

    <div class="grid-3" style="margin-bottom: 20px;">
      <div class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">{{ attachmentForm.id ? 'Edit Attachment' : 'Create Attachment' }}</div>
        <el-form :model="attachmentForm" label-position="top">
          <el-form-item label="Instrument">
            <el-select v-model="attachmentForm.instrumentId" style="width: 100%;">
              <el-option v-for="item in instrumentOptions" :key="item.id" :label="item.instrumentName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="File Name">
            <el-input v-model="attachmentForm.fileName" />
          </el-form-item>
          <el-form-item label="File Url">
            <el-input v-model="attachmentForm.fileUrl" />
          </el-form-item>
          <div style="display: flex; gap: 8px;">
            <el-button type="primary" @click="submitAttachment">Save</el-button>
            <el-button v-if="attachmentForm.id" @click="resetAttachment">Cancel</el-button>
          </div>
        </el-form>
      </div>
      <div class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">Categories</div>
        <el-table :data="categories" border size="small">
          <el-table-column prop="categoryName" label="Name" />
          <el-table-column prop="categoryCode" label="Code" />
          <el-table-column prop="status" label="Status" />
          <el-table-column label="Action" width="140">
            <template #default="{ row }">
              <el-button link @click="editCategory(row)">Edit</el-button>
              <el-button link type="danger" @click="removeCategory(row)">Delete</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
      <div class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">Attachments</div>
        <el-table :data="attachments" border size="small">
          <el-table-column prop="fileName" label="Name" />
          <el-table-column prop="fileUrl" label="Url" />
          <el-table-column label="Action" width="140">
            <template #default="{ row }">
              <el-button link @click="editAttachment(row)">Edit</el-button>
              <el-button link type="danger" @click="removeAttachment(row)">Delete</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <div class="grid-3">
      <div class="content-card" style="padding: 20px; grid-column: span 2;">
        <div style="display: flex; gap: 12px; margin-bottom: 16px;">
          <el-input v-model="instrumentQuery.keyword" placeholder="Search instrument name or code" />
          <el-select v-model="instrumentQuery.categoryId" clearable placeholder="Category" style="width: 180px;">
            <el-option v-for="item in categories" :key="item.id" :label="item.categoryName" :value="item.id" />
          </el-select>
          <el-select v-model="instrumentQuery.status" clearable placeholder="Status" style="width: 140px;">
            <el-option label="NORMAL" value="NORMAL" />
            <el-option label="DISABLED" value="DISABLED" />
          </el-select>
          <el-button type="primary" @click="searchInstruments">Search</el-button>
        </div>
        <div class="section-title" style="font-size: 18px;">Instruments</div>
        <el-table :data="instruments" border size="small">
          <el-table-column prop="instrumentName" label="Name" />
          <el-table-column prop="instrumentNo" label="Code" />
          <el-table-column prop="categoryName" label="Category" />
          <el-table-column prop="location" label="Location" />
          <el-table-column prop="status" label="Status" />
          <el-table-column label="Action" width="140">
            <template #default="{ row }">
              <el-button link @click="editInstrument(row)">Edit</el-button>
              <el-button link type="danger" @click="removeInstrument(row)">Delete</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
          <el-pagination
            background
            layout="prev, pager, next, total"
            :current-page="instrumentQuery.pageNum"
            :page-size="instrumentQuery.pageSize"
            :total="instrumentTotal"
            @current-change="changeInstrumentPage"
          />
        </div>
      </div>
      <div class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">Open Rules</div>
        <el-table :data="openRules" border size="small">
          <el-table-column prop="weekDay" label="Day" width="70" />
          <el-table-column label="Time">
            <template #default="{ row }">
              {{ row.startTime }} - {{ row.endTime }}
            </template>
          </el-table-column>
          <el-table-column label="Action" width="140">
            <template #default="{ row }">
              <el-button link @click="editRule(row)">Edit</el-button>
              <el-button link type="danger" @click="removeRule(row)">Delete</el-button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAttachment,
  createInstrument,
  createInstrumentCategory,
  createOpenRule,
  deleteAttachment,
  deleteInstrument,
  deleteInstrumentCategory,
  deleteOpenRule,
  getAdminAttachments,
  getAdminInstrumentCategories,
  getAdminInstruments,
  getAdminOpenRules,
  updateAttachment,
  updateInstrument,
  updateInstrumentCategory,
  updateOpenRule
} from '../../api/admin'

function defaultCategoryForm() {
  return { id: null, categoryName: '', categoryCode: '' }
}

function defaultInstrumentForm() {
  return {
    id: null,
    name: '',
    code: '',
    categoryId: null,
    location: '',
    description: '',
    machinePricePerHour: 100,
    samplePricePerItem: 50,
    status: 'NORMAL'
  }
}

function defaultRuleForm() {
  return { id: null, instrumentId: null, openDays: '1', openTimeRange: '09:00-17:00' }
}

function defaultAttachmentForm() {
  return { id: null, instrumentId: null, fileName: '', fileUrl: '', fileType: 'FILE', sortNo: 0 }
}

export default {
  data() {
    return {
      categories: [],
      instruments: [],
      instrumentOptions: [],
      instrumentTotal: 0,
      openRules: [],
      attachments: [],
      instrumentQuery: {
        keyword: '',
        categoryId: null,
        status: '',
        pageNum: 1,
        pageSize: 8
      },
      weekDays: [
        { label: 'Monday', value: '1' },
        { label: 'Tuesday', value: '2' },
        { label: 'Wednesday', value: '3' },
        { label: 'Thursday', value: '4' },
        { label: 'Friday', value: '5' }
      ],
      categoryForm: defaultCategoryForm(),
      instrumentForm: defaultInstrumentForm(),
      ruleForm: defaultRuleForm(),
      attachmentForm: defaultAttachmentForm()
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async load() {
      const [categories, instrumentPage, instrumentOptionsPage, openRules, attachments] = await Promise.all([
        getAdminInstrumentCategories(),
        getAdminInstruments(this.instrumentQuery),
        getAdminInstruments({ pageNum: 1, pageSize: 1000 }),
        getAdminOpenRules(),
        getAdminAttachments()
      ])
      this.categories = categories
      this.instruments = instrumentPage.list || []
      this.instrumentOptions = instrumentOptionsPage.list || []
      this.instrumentTotal = instrumentPage.total || 0
      this.openRules = openRules
      this.attachments = attachments
      if (!this.instrumentForm.categoryId && categories.length) {
        this.instrumentForm.categoryId = categories[0].id
      }
      if (!this.ruleForm.instrumentId && this.instrumentOptions.length) {
        this.ruleForm.instrumentId = this.instrumentOptions[0].id
      }
      if (!this.attachmentForm.instrumentId && this.instrumentOptions.length) {
        this.attachmentForm.instrumentId = this.instrumentOptions[0].id
      }
    },
    async searchInstruments() {
      this.instrumentQuery.pageNum = 1
      await this.load()
    },
    async changeInstrumentPage(pageNum) {
      this.instrumentQuery.pageNum = pageNum
      await this.load()
    },
    editCategory(row) {
      this.categoryForm = { ...row }
    },
    resetCategory() {
      this.categoryForm = defaultCategoryForm()
    },
    async submitCategory() {
      const payload = { ...this.categoryForm, status: 'ENABLED' }
      if (payload.id) {
        await updateInstrumentCategory(payload.id, payload)
        ElMessage.success('Category updated')
      } else {
        await createInstrumentCategory(payload)
        ElMessage.success('Category created')
      }
      this.resetCategory()
      await this.load()
    },
    async removeCategory(row) {
      await ElMessageBox.confirm(`Delete category ${row.categoryName}?`, 'Confirm')
      await deleteInstrumentCategory(row.id)
      ElMessage.success('Category deleted')
      await this.load()
    },
    editInstrument(row) {
      this.instrumentForm = {
        id: row.id,
        name: row.instrumentName,
        code: row.instrumentNo,
        categoryId: row.categoryId,
        location: row.location,
        description: row.intro || '',
        machinePricePerHour: row.priceInternal || 100,
        samplePricePerItem: row.priceExternal || 50,
        status: row.status
      }
    },
    resetInstrument() {
      this.instrumentForm = defaultInstrumentForm()
      if (this.categories.length) {
        this.instrumentForm.categoryId = this.categories[0].id
      }
    },
    async submitInstrument() {
      if (this.instrumentForm.id) {
        await updateInstrument(this.instrumentForm.id, this.instrumentForm)
        ElMessage.success('Instrument updated')
      } else {
        await createInstrument(this.instrumentForm)
        ElMessage.success('Instrument created')
      }
      this.resetInstrument()
      await this.load()
    },
    async removeInstrument(row) {
      await ElMessageBox.confirm(`Delete instrument ${row.instrumentName}?`, 'Confirm')
      await deleteInstrument(row.id)
      ElMessage.success('Instrument deleted')
      await this.load()
    },
    editRule(row) {
      this.ruleForm = {
        id: row.id,
        instrumentId: row.instrumentId,
        openDays: String(row.weekDay),
        openTimeRange: `${String(row.startTime).slice(0, 5)}-${String(row.endTime).slice(0, 5)}`
      }
    },
    resetRule() {
      this.ruleForm = defaultRuleForm()
      if (this.instrumentOptions.length) {
        this.ruleForm.instrumentId = this.instrumentOptions[0].id
      }
    },
    async submitRule() {
      const payload = {
        instrumentId: this.ruleForm.instrumentId,
        openDays: this.ruleForm.openDays,
        openTimeRange: this.ruleForm.openTimeRange
      }
      if (this.ruleForm.id) {
        await updateOpenRule(this.ruleForm.id, payload)
        ElMessage.success('Open rule updated')
      } else {
        await createOpenRule(payload)
        ElMessage.success('Open rule created')
      }
      this.resetRule()
      await this.load()
    },
    async removeRule(row) {
      await ElMessageBox.confirm(`Delete open rule ${row.id}?`, 'Confirm')
      await deleteOpenRule(row.id)
      ElMessage.success('Open rule deleted')
      await this.load()
    },
    editAttachment(row) {
      this.attachmentForm = { ...row }
    },
    resetAttachment() {
      this.attachmentForm = defaultAttachmentForm()
      if (this.instrumentOptions.length) {
        this.attachmentForm.instrumentId = this.instrumentOptions[0].id
      }
    },
    async submitAttachment() {
      if (this.attachmentForm.id) {
        await updateAttachment(this.attachmentForm.id, this.attachmentForm)
        ElMessage.success('Attachment updated')
      } else {
        await createAttachment(this.attachmentForm)
        ElMessage.success('Attachment created')
      }
      this.resetAttachment()
      await this.load()
    },
    async removeAttachment(row) {
      await ElMessageBox.confirm(`Delete attachment ${row.fileName}?`, 'Confirm')
      await deleteAttachment(row.id)
      ElMessage.success('Attachment deleted')
      await this.load()
    }
  }
}
</script>
