<template>
  <div class="portal-subpage">
    <div class="help-layout">
      <div class="content-card help-list-card">
        <div class="section-title">帮助文档</div>
        <div
          v-for="item in docs"
          :key="item.id"
          class="help-row"
          :class="{ active: currentDoc && currentDoc.id === item.id }"
          @click="selectDoc(item)"
        >
          {{ item.title }}
        </div>
      </div>

      <div class="content-card help-detail-card">
        <template v-if="currentDoc">
          <div class="help-detail-title">{{ currentDoc.title }}</div>
          <div class="help-detail-content">{{ currentDoc.content || currentDoc.summary || '暂无帮助内容。' }}</div>
        </template>
        <template v-else>
          <div class="empty-tip">暂无帮助文档</div>
        </template>
      </div>
    </div>
  </div>
</template>

<script>
import { getHelpDocs, getHelpDoc } from '../../api/content'

export default {
  data() {
    return {
      docs: [],
      currentDoc: null
    }
  },
  async created() {
    const docs = await getHelpDocs().catch(() => [])
    this.docs = docs || []
    if (this.docs.length) {
      await this.selectDoc(this.docs[0])
    }
  },
  methods: {
    async selectDoc(item) {
      this.currentDoc = await getHelpDoc(item.id).catch(() => item)
    }
  }
}
</script>

<style scoped>
.portal-subpage { display: flex; flex-direction: column; gap: 18px; }
.help-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 18px; }
.help-list-card, .help-detail-card { padding: 24px; }
.help-row { padding: 13px 0; border-bottom: 1px dashed #dbe4ef; cursor: pointer; color: #334d69; }
.help-row.active { color: #0b4ea2; font-weight: 700; }
.help-detail-title { font-size: 28px; font-weight: 700; color: #163d77; }
.help-detail-content { margin-top: 18px; color: #44566f; line-height: 2; white-space: pre-wrap; }
.empty-tip { color: #7a8ba1; }
@media (max-width: 900px) { .help-layout { grid-template-columns: 1fr; } }
</style>
