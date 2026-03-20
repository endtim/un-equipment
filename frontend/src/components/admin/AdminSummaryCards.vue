<template>
  <div class="admin-summary-grid">
    <div v-for="item in items" :key="item.label" class="admin-summary-card">
      <div class="admin-summary-label">{{ item.label }}</div>
      <div class="admin-summary-value">{{ item.value }}</div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'AdminSummaryCards',
  props: {
    items: {
      type: Array,
      default: () => []
    }
  }
}
</script>
