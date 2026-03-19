<template>
  <div class="portal-subpage content-list-page">
    <section class="content-card content-list-card">
      <div v-if="docs.length" class="content-list-body">
        <article v-for="item in docs" :key="item.id" class="content-item" @click="openDetail(item)">
          <h3 class="content-item-title">{{ item.title }}</h3>
          <p class="content-item-summary">{{ buildSummary(item) }}</p>
          <div class="content-item-time">
            <span class="time-dot" />
            {{ formatTime(item.publishTime || item.createTime) }}
          </div>
        </article>
      </div>
      <el-empty v-else description="暂无帮助文档" :image-size="92" />

      <div class="content-pagination">
        <el-pagination
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="total"
          :current-page="query.pageNum"
          :page-size="query.pageSize"
          :page-sizes="pageSizeOptions"
          @size-change="changePageSize"
          @current-change="changePage"
        />
      </div>
    </section>
  </div>
</template>

<script>
import { formatDateTime } from '../../utils/datetime'
import { getHelpDocs } from '../../api/content'

export default {
  data() {
    return {
      docs: [],
      total: 0,
      pageSizeOptions: [10, 20, 30],
      query: {
        publishStatus: 'PUBLISHED',
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  async created() {
    this.restoreQuery()
    await this.load()
  },
  methods: {
    restoreQuery() {
      const q = this.$route.query || {}
      this.query.pageNum = Number(q.pageNum || 1)
      this.query.pageSize = Number(q.pageSize || 10)
    },
    syncQuery() {
      this.$router.replace({
        path: this.$route.path,
        query: {
          pageNum: this.query.pageNum > 1 ? String(this.query.pageNum) : undefined,
          pageSize: this.query.pageSize !== 10 ? String(this.query.pageSize) : undefined
        }
      })
    },
    async load() {
      const page = await getHelpDocs(this.query).catch(() => null)
      this.docs = Array.isArray(page?.list) ? page.list : []
      this.total = Number(page?.total || 0)
      this.syncQuery()
    },
    buildSummary(item) {
      const raw = item.summary || item.content || ''
      const plain = String(raw).replace(/\s+/g, ' ').trim()
      if (!plain) {
        return '暂无摘要'
      }
      return plain.length > 170 ? `${plain.slice(0, 170)}...` : plain
    },
    formatTime(value) {
      return formatDateTime(value, 'YYYY-MM-DD HH:mm:ss', '-')
    },
    openDetail(item) {
      this.$router.push({
        path: `/help-center/${item.id}`,
        query: {
          pageNum: String(this.query.pageNum),
          pageSize: String(this.query.pageSize)
        }
      })
    },
    async changePage(pageNum) {
      this.query.pageNum = pageNum
      await this.load()
    },
    async changePageSize(pageSize) {
      this.query.pageSize = pageSize
      this.query.pageNum = 1
      await this.load()
    }
  }
}
</script>

<style scoped>
.content-list-page {
  display: flex;
  flex-direction: column;
}

.content-list-card {
  padding: 22px 38px 16px;
}

.content-list-body {
  min-height: 300px;
}

.content-item {
  padding: 26px 0 22px;
  border-bottom: 1px dashed #e4e9f2;
  cursor: pointer;
}

.content-item-title {
  margin: 0;
  color: #21354f;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.45;
}

.content-item-summary {
  margin: 14px 0 0;
  color: #7f8ea4;
  font-size: 14px;
  line-height: 1.8;
}

.content-item-time {
  margin-top: 20px;
  color: #8799b2;
  font-size: 13px;
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.time-dot {
  width: 14px;
  height: 14px;
  border: 2px solid #9bc0f9;
  border-radius: 50%;
  position: relative;
}

.time-dot::after {
  content: '';
  position: absolute;
  left: 5px;
  top: 1px;
  width: 2px;
  height: 5px;
  background: #9bc0f9;
  border-radius: 2px;
}

.content-pagination {
  display: flex;
  justify-content: center;
  padding: 18px 0 6px;
}

@media (max-width: 900px) {
  .content-list-card {
    padding: 14px 16px;
  }

  .content-item {
    padding: 16px 0 14px;
  }

  .content-item-title {
    font-size: 18px;
  }

  .content-item-summary {
    font-size: 14px;
    margin-top: 8px;
  }

  .content-item-time {
    font-size: 13px;
    margin-top: 12px;
  }

  .content-pagination {
    overflow-x: auto;
    justify-content: flex-start;
  }
}
</style>
