<template>
  <div>
    <div class="grid-3" style="margin-bottom: 20px;">
      <div v-if="showNotice" class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">{{ noticeForm.id ? '编辑公告' : '发布公告' }}</div>
        <el-form :model="noticeForm" label-position="top">
          <el-form-item label="公告标题">
            <el-input v-model="noticeForm.title" />
          </el-form-item>
          <el-form-item label="摘要">
            <el-input v-model="noticeForm.summary" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="正文内容">
            <el-input v-model="noticeForm.content" type="textarea" :rows="5" />
          </el-form-item>
          <div style="display: flex; gap: 8px;">
            <el-button type="primary" @click="submitNotice">保存</el-button>
            <el-button v-if="noticeForm.id" @click="resetNotice">取消</el-button>
          </div>
        </el-form>
      </div>
      <div v-if="showHelpDoc" class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">{{ helpDocForm.id ? '编辑帮助文档' : '发布帮助文档' }}</div>
        <el-form :model="helpDocForm" label-position="top">
          <el-form-item label="文档标题">
            <el-input v-model="helpDocForm.title" />
          </el-form-item>
          <el-form-item label="摘要">
            <el-input v-model="helpDocForm.summary" type="textarea" :rows="3" />
          </el-form-item>
          <el-form-item label="正文内容">
            <el-input v-model="helpDocForm.content" type="textarea" :rows="5" />
          </el-form-item>
          <div style="display: flex; gap: 8px;">
            <el-button type="primary" @click="submitHelpDoc">保存</el-button>
            <el-button v-if="helpDocForm.id" @click="resetHelpDoc">取消</el-button>
          </div>
        </el-form>
      </div>
      <div class="content-card" style="padding: 20px;">
        <div class="section-title" style="font-size: 18px;">内容概览</div>
        <div style="padding: 8px 0;">公告数量：{{ noticeTotal }}</div>
        <div style="padding: 8px 0;">帮助文档数量：{{ helpDocTotal }}</div>
      </div>
    </div>

    <div class="grid-3">
      <div v-if="showNotice" class="content-card" style="padding: 20px; grid-column: span 2;">
        <div style="display: flex; gap: 12px; margin-bottom: 16px;">
          <el-input v-model="noticeQuery.keyword" placeholder="搜索公告标题" />
          <el-select v-model="noticeQuery.publishStatus" clearable placeholder="发布状态" style="width: 140px;">
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="草稿" value="DRAFT" />
          </el-select>
          <el-button type="primary" @click="searchNotices">查询</el-button>
        </div>
        <div class="section-title" style="font-size: 18px;">公告列表</div>
        <el-table :data="notices" border size="small">
          <el-table-column prop="title" label="标题" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="publishTagType(row.publishStatus)" size="small">{{ publishStatusLabel(row.publishStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="publishTime" label="发布时间" width="180" />
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link @click="editNotice(row)">编辑</el-button>
              <el-button link type="danger" @click="removeNotice(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
          <el-pagination
            background
            layout="prev, pager, next, total"
            :current-page="noticeQuery.pageNum"
            :page-size="noticeQuery.pageSize"
            :total="noticeTotal"
            @current-change="changeNoticePage"
          />
        </div>
      </div>
      <div v-if="showHelpDoc" class="content-card" style="padding: 20px;" :style="showHelpOnly ? 'grid-column: span 3;' : ''">
        <div style="display: flex; gap: 12px; margin-bottom: 16px;">
          <el-input v-model="helpDocQuery.keyword" placeholder="搜索帮助文档标题" />
          <el-select v-model="helpDocQuery.publishStatus" clearable placeholder="发布状态" style="width: 140px;">
            <el-option label="已发布" value="PUBLISHED" />
            <el-option label="草稿" value="DRAFT" />
          </el-select>
          <el-button type="primary" @click="searchHelpDocs">查询</el-button>
        </div>
        <div class="section-title" style="font-size: 18px;">帮助文档列表</div>
        <el-table :data="helpDocs" border size="small">
          <el-table-column prop="title" label="标题" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="publishTagType(row.publishStatus)" size="small">{{ publishStatusLabel(row.publishStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="140">
            <template #default="{ row }">
              <el-button link @click="editHelpDoc(row)">编辑</el-button>
              <el-button link type="danger" @click="removeHelpDoc(row)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div style="display: flex; justify-content: flex-end; margin-top: 16px;">
          <el-pagination
            background
            layout="prev, pager, next, total"
            :current-page="helpDocQuery.pageNum"
            :page-size="helpDocQuery.pageSize"
            :total="helpDocTotal"
            @current-change="changeHelpDocPage"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createHelpDoc,
  createNotice,
  deleteHelpDoc,
  deleteNotice,
  getAdminHelpDocPage,
  getAdminNoticePage,
  updateHelpDoc,
  updateNotice
} from '../../api/admin'

function defaultNoticeForm() {
  return { id: null, title: '', summary: '', content: '', publishStatus: 'PUBLISHED' }
}

function defaultHelpDocForm() {
  return { id: null, title: '', summary: '', content: '', publishStatus: 'PUBLISHED' }
}

export default {
  props: {
    contentType: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      notices: [],
      helpDocs: [],
      noticeTotal: 0,
      helpDocTotal: 0,
      noticeQuery: {
        keyword: '',
        publishStatus: '',
        pageNum: 1,
        pageSize: 6
      },
      helpDocQuery: {
        keyword: '',
        publishStatus: '',
        pageNum: 1,
        pageSize: 6
      },
      noticeForm: defaultNoticeForm(),
      helpDocForm: defaultHelpDocForm()
    }
  },
  computed: {
    showNotice() {
      return this.contentType !== 'help'
    },
    showHelpDoc() {
      return this.contentType !== 'notice'
    },
    showHelpOnly() {
      return this.contentType === 'help'
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async load() {
      const tasks = []
      if (this.showNotice) {
        tasks.push(getAdminNoticePage(this.noticeQuery))
      } else {
        tasks.push(Promise.resolve({ list: [], total: 0 }))
      }
      if (this.showHelpDoc) {
        tasks.push(getAdminHelpDocPage(this.helpDocQuery))
      } else {
        tasks.push(Promise.resolve({ list: [], total: 0 }))
      }
      const [noticePage, helpDocPage] = await Promise.all(tasks)
      this.notices = noticePage.list || []
      this.noticeTotal = noticePage.total || 0
      this.helpDocs = helpDocPage.list || []
      this.helpDocTotal = helpDocPage.total || 0
    },
    async searchNotices() {
      this.noticeQuery.pageNum = 1
      await this.load()
    },
    async changeNoticePage(pageNum) {
      this.noticeQuery.pageNum = pageNum
      await this.load()
    },
    async searchHelpDocs() {
      this.helpDocQuery.pageNum = 1
      await this.load()
    },
    async changeHelpDocPage(pageNum) {
      this.helpDocQuery.pageNum = pageNum
      await this.load()
    },
    editNotice(row) {
      this.noticeForm = { ...row }
    },
    resetNotice() {
      this.noticeForm = defaultNoticeForm()
    },
    async submitNotice() {
      if (this.noticeForm.id) {
        await updateNotice(this.noticeForm.id, this.noticeForm)
        ElMessage.success('公告已更新')
      } else {
        await createNotice(this.noticeForm)
        ElMessage.success('公告已发布')
      }
      this.resetNotice()
      await this.load()
    },
    async removeNotice(row) {
      await ElMessageBox.confirm(`确认删除公告“${row.title}”吗？`, '删除确认')
      await deleteNotice(row.id)
      ElMessage.success('公告已删除')
      await this.load()
    },
    editHelpDoc(row) {
      this.helpDocForm = { ...row }
    },
    resetHelpDoc() {
      this.helpDocForm = defaultHelpDocForm()
    },
    async submitHelpDoc() {
      if (this.helpDocForm.id) {
        await updateHelpDoc(this.helpDocForm.id, this.helpDocForm)
        ElMessage.success('帮助文档已更新')
      } else {
        await createHelpDoc(this.helpDocForm)
        ElMessage.success('帮助文档已发布')
      }
      this.resetHelpDoc()
      await this.load()
    },
    async removeHelpDoc(row) {
      await ElMessageBox.confirm(`确认删除帮助文档“${row.title}”吗？`, '删除确认')
      await deleteHelpDoc(row.id)
      ElMessage.success('帮助文档已删除')
      await this.load()
    },
    publishStatusLabel(value) {
      const mapping = {
        PUBLISHED: '已发布',
        DRAFT: '草稿'
      }
      return mapping[value] || value || '-'
    },
    publishTagType(value) {
      const mapping = {
        PUBLISHED: 'success',
        DRAFT: 'info'
      }
      return mapping[value] || 'info'
    }
  }
}
</script>
