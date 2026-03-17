<template>
  <div class="portal-subpage">
    <section class="content-card article-card" v-loading="loading">
      <div class="article-head">
        <h1 class="article-title">{{ detail.title || '正文' }}</h1>
        <el-button size="small" @click="goBack">返回</el-button>
      </div>

      <div class="article-meta">
        发布时间：{{ formatTime(detail.publishTime || detail.createTime) }}
      </div>

      <div class="article-content">{{ detail.content || detail.summary || '暂无内容' }}</div>

      <div v-if="detail.fileUrl" class="article-footer">
        附件下载：
        <a :href="detail.fileUrl" target="_blank" rel="noopener noreferrer">{{ detail.fileUrl }}</a>
      </div>
    </section>
  </div>
</template>

<script>
import { formatDateTime } from '../../utils/datetime'
import { getHelpDoc, getNotice } from '../../api/content'

export default {
  props: {
    contentType: {
      type: String,
      default: 'notice'
    }
  },
  data() {
    return {
      loading: false,
      detail: {}
    }
  },
  async created() {
    await this.loadDetail()
  },
  watch: {
    '$route.params.id': {
      handler() {
        this.loadDetail()
      }
    }
  },
  methods: {
    async loadDetail() {
      const id = this.$route.params.id
      if (!id) {
        this.detail = {}
        return
      }
      this.loading = true
      try {
        if (this.contentType === 'help') {
          this.detail = (await getHelpDoc(id).catch(() => ({}))) || {}
        } else {
          this.detail = (await getNotice(id).catch(() => ({}))) || {}
        }
      } finally {
        this.loading = false
      }
    },
    formatTime(value) {
      return formatDateTime(value, 'YYYY-MM-DD HH:mm:ss', '-')
    },
    goBack() {
      if (this.contentType === 'help') {
        this.$router.push({ path: '/help-center', query: this.$route.query })
        return
      }
      this.$router.push({ path: '/notices', query: this.$route.query })
    }
  }
}
</script>

<style scoped>
.article-card {
  padding: 0;
  border-radius: 10px;
  overflow: hidden;
}

.article-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 24px;
  border-bottom: 1px solid #d8e2ef;
}

.article-title {
  margin: 0;
  flex: 1;
  text-align: center;
  font-size: 24px;
  color: #1f3350;
}

.article-meta {
  padding: 12px 24px;
  border-top: 1px dashed #2d6fcf;
  border-bottom: 1px solid #e7edf5;
  color: #6c7f98;
  font-size: 13px;
}

.article-content {
  padding: 20px 24px 26px;
  color: #2a3d58;
  line-height: 2;
  white-space: pre-wrap;
  min-height: 460px;
}

.article-footer {
  padding: 0 24px 20px;
  color: #5f7390;
  font-size: 13px;
  word-break: break-all;
}

.article-footer a {
  color: #2b6cca;
}

@media (max-width: 900px) {
  .article-head {
    padding: 12px 14px;
    gap: 10px;
  }

  .article-title {
    text-align: left;
    font-size: 18px;
  }

  .article-meta,
  .article-content,
  .article-footer {
    padding-left: 14px;
    padding-right: 14px;
  }

  .article-content {
    min-height: 260px;
  }
}
</style>
