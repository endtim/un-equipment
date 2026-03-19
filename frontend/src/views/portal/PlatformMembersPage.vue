<template>
  <div class="portal-subpage members-page">
    <section class="content-card members-card">
      <div v-if="rows.length" class="members-grid">
        <article v-for="item in rows" :key="item.departmentId" class="member-item">
          <div class="member-title-line">
            <span class="member-icon" />
            <h3 class="member-title">{{ item.departmentName }}</h3>
          </div>
          <div class="member-count">仪器数量：{{ item.instrumentCount }}</div>
        </article>
      </div>
      <el-empty v-else description="暂无平台成员数据" :image-size="92" />
    </section>
  </div>
</template>

<script>
import { getPlatformMembers } from '../../api/stat'

export default {
  data() {
    return {
      rows: []
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    async load() {
      const result = await getPlatformMembers().catch(() => [])
      this.rows = Array.isArray(result) ? result : []
    }
  }
}
</script>

<style scoped>
.members-page {
  display: flex;
  flex-direction: column;
}

.members-card {
  padding: 26px 22px 28px;
}

.members-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 18px 22px;
}

.member-item {
  border: 1px solid #4f86dd;
  border-radius: 14px;
  background: #f7f9fd;
  padding: 18px 20px 20px;
  min-height: 116px;
}

.member-title-line {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.member-icon {
  width: 22px;
  height: 22px;
  border: 2px solid #1a5eb8;
  border-radius: 5px;
  position: relative;
  margin-top: 2px;
  flex: 0 0 22px;
}

.member-icon::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 3px;
  width: 7px;
  height: 2px;
  background: #1a5eb8;
}

.member-icon::after {
  content: '';
  position: absolute;
  left: 4px;
  top: 8px;
  width: 12px;
  height: 8px;
  border: 2px solid #1a5eb8;
  border-top: 0;
  border-radius: 0 0 3px 3px;
  box-sizing: border-box;
}

.member-title {
  margin: 0;
  color: #1d4f9a;
  font-size: 16px;
  font-weight: 600;
  line-height: 1.45;
  word-break: break-word;
}

.member-count {
  margin-left: 34px;
  margin-top: 12px;
  color: #3a4d67;
  font-size: 14px;
  line-height: 1.5;
}

@media (max-width: 1280px) {
  .members-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 960px) {
  .members-card {
    padding: 14px 12px;
  }

  .members-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px;
  }

  .member-title {
    font-size: 15px;
  }

  .member-count {
    margin-left: 34px;
    font-size: 13px;
    margin-top: 8px;
  }
}

@media (max-width: 680px) {
  .members-grid {
    grid-template-columns: 1fr;
  }
}
</style>
