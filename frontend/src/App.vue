<template>
  <div id="app">
    <router-view v-if="!errorState.hasError" />
    <div v-else class="global-error-wrap">
      <div class="global-error-card">
        <h2 class="global-error-title">{{ errorState.title }}</h2>
        <p class="global-error-message">{{ errorState.message }}</p>
        <p class="global-error-meta">来源：{{ errorState.source || 'unknown' }}</p>
        <p class="global-error-meta">时间：{{ errorState.time }}</p>
        <div class="global-error-actions">
          <el-button @click="goHome">返回首页</el-button>
          <el-button type="primary" @click="retry">继续使用</el-button>
          <el-button type="danger" plain @click="reloadPage">刷新页面</el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { clearGlobalError, useGlobalErrorState } from './utils/error-center'

export default {
  name: 'App',
  data() {
    return {
      errorState: useGlobalErrorState()
    }
  },
  methods: {
    retry() {
      clearGlobalError()
    },
    reloadPage() {
      window.location.reload()
    },
    goHome() {
      clearGlobalError()
      this.$router.push('/home')
    }
  }
}
</script>

<style scoped>
.global-error-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f3f6fb;
}

.global-error-card {
  width: min(680px, 100%);
  background: #fff;
  border: 1px solid #dbe4f2;
  border-radius: 8px;
  box-shadow: 0 4px 14px rgba(17, 47, 89, 0.08);
  padding: 24px;
}

.global-error-title {
  margin: 0 0 10px;
  color: #19488a;
  font-size: 24px;
}

.global-error-message {
  margin: 0 0 10px;
  color: #2f3f56;
  line-height: 1.8;
}

.global-error-meta {
  margin: 4px 0;
  color: #7a8ba3;
  font-size: 13px;
}

.global-error-actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  flex-wrap: wrap;
}
</style>
