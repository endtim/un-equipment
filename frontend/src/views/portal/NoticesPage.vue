<template>
  <div class="portal-subpage">
    <div class="notice-layout">
      <div class="content-card notice-list-card">
        <div class="section-title">公告列表</div>
        <div
          v-for="item in notices"
          :key="item.id"
          class="notice-row"
          :class="{ active: currentNotice && currentNotice.id === item.id }"
          @click="selectNotice(item)"
        >
          <div class="notice-row-title">{{ item.title }}</div>
          <div class="notice-row-date">{{ formatDate(item.publishTime || item.createTime) }}</div>
        </div>
      </div>

      <div class="content-card notice-detail-card">
        <template v-if="currentNotice">
          <div class="notice-detail-title">{{ currentNotice.title }}</div>
          <div class="notice-detail-meta">发布时间：{{ formatDate(currentNotice.publishTime || currentNotice.createTime) }}</div>
          <div class="notice-detail-content">{{ currentNotice.content || currentNotice.summary || '暂无公告内容。' }}</div>
        </template>
        <template v-else>
          <div class="empty-tip">暂无公告信息</div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { getNotices, getNotice } from '../../api/content'

export default {
  data() {
    return {
      notices: [],
      currentNotice: null
    }
  },
  async created() {
    const notices = await getNotices().catch(() => [])
    this.notices = notices || []
    if (this.notices.length) {
      await this.selectNotice(this.notices[0])
    }
  },
  methods: {
    async selectNotice(item) {
      this.currentNotice = await getNotice(item.id).catch(() => item)
    },
    formatDate(value) {
      return value ? String(value).slice(0, 10) : ''
    }
  }
}
</script>

<style scoped>
.portal-subpage { display: flex; flex-direction: column; gap: 18px; }
.notice-layout { display: grid; grid-template-columns: 380px minmax(0, 1fr); gap: 18px; }
.notice-list-card, .notice-detail-card { padding: 24px; }
.notice-row { padding: 14px 0; border-bottom: 1px dashed #dbe4ef; cursor: pointer; }
.notice-row.active .notice-row-title { color: #0b4ea2; }
.notice-row-title { font-weight: 700; color: #304863; }
.notice-row-date { margin-top: 6px; color: #8192a8; font-size: 13px; }
.notice-detail-title { font-size: 28px; font-weight: 700; color: #163d77; }
.notice-detail-meta { margin-top: 12px; color: #7b8ea8; border-bottom: 1px solid #e1e8f2; padding-bottom: 14px; }
.notice-detail-content { margin-top: 22px; color: #44566f; line-height: 2; white-space: pre-wrap; }
.empty-tip { color: #7a8ba1; }
@media (max-width: 900px) { .notice-layout { grid-template-columns: 1fr; } }
</style>
