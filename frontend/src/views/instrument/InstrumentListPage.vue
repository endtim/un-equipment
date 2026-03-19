<template>
  <div class="service-page">
    <div class="content-card service-hero">
      <div>
        <div class="service-kicker">预约服务</div>
        <div class="section-title service-title">仪器资源检索</div>
        <div class="service-desc">
          支持按关键字、分类与状态筛选仪器，点击卡片可查看详情或直接进入预约流程。
        </div>
      </div>
      <div class="service-summary">
        <div class="summary-number">{{ total }}</div>
        <div class="summary-label">当前检索结果</div>
      </div>
    </div>

    <div class="content-card filter-card">
      <el-form :inline="true" :model="query" class="filter-form">
        <el-form-item label="关键词">
          <el-input
            v-model="query.keyword"
            placeholder="仪器名称/编号"
            clearable
            class="filter-input-keyword"
          />
        </el-form-item>
        <el-form-item label="仪器分类">
          <el-select
            v-model="query.categoryId"
            clearable
            placeholder="全部分类"
            class="filter-select-category"
          >
            <el-option
              v-for="item in categories"
              :key="item.id"
              :label="item.categoryName"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="开放状态">
          <el-select
            v-model="query.status"
            clearable
            placeholder="全部状态"
            class="filter-select-status"
          >
            <el-option label="正常开放" value="NORMAL" />
            <el-option label="暂停开放" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="instrument-grid">
      <div v-for="item in list" :key="item.id" class="content-card instrument-card">
        <div class="instrument-cover" @click="openDetail(item)">
          <div class="instrument-badge">{{ item.categoryName || '未分类' }}</div>
          <div class="instrument-title">{{ item.name }}</div>
          <div class="instrument-code">{{ item.code || '暂无编号' }}</div>
        </div>
        <div class="instrument-body">
          <div class="instrument-meta">所属单位：{{ item.departmentName || '校级公共平台' }}</div>
          <div class="instrument-meta">负责人：{{ item.ownerUserName || '平台管理员' }}</div>
          <div class="instrument-meta">放置地点：{{ item.location || '暂未填写' }}</div>
          <div class="instrument-status-row">
            <el-tag :type="statusTagType(item.status)">{{
              statusLabel(item.status, item.openStatus)
            }}</el-tag>
            <span class="mode-text">{{ openModeLabel(item.openMode) }}</span>
          </div>
          <div class="price-row">
            <div>
              校内价格：<strong>{{
                formatAmount(item.priceInternal ?? item.machinePricePerHour)
              }}</strong>
              元/单位
            </div>
            <div>
              校外价格：<strong>{{
                formatAmount(item.priceExternal ?? item.samplePricePerItem)
              }}</strong>
              元/单位
            </div>
          </div>
          <div class="card-actions">
            <el-button @click="openDetail(item)">查看详情</el-button>
            <el-button type="primary" :disabled="!canReserve(item)" @click="quickReserve(item)">
              {{ reserveButtonText(item) }}
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <div class="content-card pagination-card">
      <div class="pagination-wrap">
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
  </div>
</template>

<script>
import { getCategories, getInstruments } from '../../api/instrument'
import { openModeLabel } from '../../utils/dicts'

function defaultQuery() {
  return {
    keyword: '',
    categoryId: null,
    status: '',
    pageNum: 1,
    pageSize: 8
  }
}

export default {
  data() {
    return {
      query: defaultQuery(),
      pageSizeOptions: [8, 16, 24, 40],
      categories: [],
      list: [],
      total: 0
    }
  },
  async created() {
    this.restoreQuery()
    await this.load()
  },
  methods: {
    openModeLabel,
    restoreQuery() {
      const q = this.$route.query
      this.query.keyword = q.keyword || ''
      this.query.categoryId = q.categoryId ? Number(q.categoryId) : null
      this.query.status = q.status || ''
      this.query.pageNum = q.pageNum ? Number(q.pageNum) : 1
      this.query.pageSize = q.pageSize ? Number(q.pageSize) : 8
    },
    syncQuery() {
      this.$router.replace({
        path: '/instruments',
        query: {
          ...(this.query.keyword ? { keyword: this.query.keyword } : {}),
          ...(this.query.categoryId ? { categoryId: String(this.query.categoryId) } : {}),
          ...(this.query.status ? { status: this.query.status } : {}),
          pageNum: String(this.query.pageNum),
          pageSize: String(this.query.pageSize)
        }
      })
    },
    async load() {
      const [categories, page] = await Promise.all([getCategories(), getInstruments(this.query)])
      this.categories = categories || []
      this.list = page.list || []
      this.total = page.total || 0
      this.syncQuery()
    },
    async search() {
      this.query.pageNum = 1
      await this.load()
    },
    async resetQuery() {
      this.query = defaultQuery()
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
    openDetail(item) {
      this.$router.push({
        path: `/instruments/${item.id}`,
        query: {
          returnPath: '/instruments',
          ...this.$route.query
        }
      })
    },
    quickReserve(item) {
      this.$router.push({
        path: `/instruments/${item.id}`,
        query: {
          returnPath: '/instruments',
          reserve: this.preferredReserveMode(item),
          ...this.$route.query
        }
      })
    },
    preferredReserveMode(item) {
      if (item.openMode === 'SAMPLE') {
        return 'sample'
      }
      return 'machine'
    },
    canReserve(item) {
      return item.status === 'NORMAL' && Number(item.openStatus || 1) === 1
    },
    reserveButtonText(item) {
      if (!this.canReserve(item)) {
        return '暂不可预约'
      }
      return item.openMode === 'SAMPLE' ? '立即送样' : '立即预约'
    },
    statusLabel(status, openStatus) {
      if (status !== 'NORMAL' || Number(openStatus || 1) !== 1) {
        return '暂停开放'
      }
      return '正常开放'
    },
    statusTagType(status) {
      return status === 'NORMAL' ? 'success' : 'danger'
    },
    formatAmount(value) {
      return value == null ? '0.00' : Number(value).toFixed(2)
    }
  }
}
</script>

<style scoped>
.service-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.service-hero {
  padding: 24px 28px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.service-kicker {
  color: var(--primary);
  font-size: 14px;
  margin-bottom: 8px;
}

.service-desc {
  color: var(--muted);
  line-height: 1.8;
}

.service-title {
  margin-bottom: 10px;
}

.service-summary {
  width: 180px;
  text-align: center;
  padding: 18px 12px;
  background: #f7fbff;
  border: 1px solid var(--line);
}

.summary-number {
  font-size: 40px;
  line-height: 1;
  color: var(--primary);
  font-weight: 700;
}

.summary-label {
  margin-top: 10px;
  color: var(--muted);
}

.filter-card {
  padding: 18px 22px 2px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.filter-input-keyword {
  width: 220px;
}

.filter-select-category {
  width: 180px;
}

.filter-select-status {
  width: 160px;
}

.instrument-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.instrument-card {
  overflow: hidden;
}

.instrument-cover {
  height: 164px;
  padding: 18px;
  background: linear-gradient(135deg, #0c4a95, #2c75c8);
  color: #fff;
  cursor: pointer;
  position: relative;
}

.instrument-badge {
  display: inline-block;
  font-size: 12px;
  padding: 4px 8px;
  border: 1px solid rgba(255, 255, 255, 0.25);
  background: rgba(255, 255, 255, 0.12);
}

.instrument-title {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 42px;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.4;
}

.instrument-code {
  position: absolute;
  left: 18px;
  bottom: 18px;
  font-size: 13px;
  opacity: 0.9;
}

.instrument-body {
  padding: 18px;
}

.instrument-meta {
  font-size: 14px;
  color: #43546d;
  line-height: 1.9;
}

.instrument-status-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
}

.mode-text {
  font-size: 13px;
  color: #6d8199;
}

.price-row {
  margin-top: 14px;
  padding: 12px 14px;
  background: #f8fbff;
  border: 1px solid var(--line);
  font-size: 14px;
  line-height: 1.9;
}

.card-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.card-actions .el-button {
  flex: 1;
}

.pagination-card {
  padding: 18px;
  margin-top: 18px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 1100px) {
  .instrument-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .service-hero,
  .instrument-grid {
    grid-template-columns: 1fr;
    display: grid;
  }
}
</style>
