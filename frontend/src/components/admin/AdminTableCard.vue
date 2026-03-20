<template>
  <div class="content-card admin-table-card">
    <div v-if="title || $slots.titleRight" class="admin-table-card-head">
      <h3 v-if="title" class="admin-card-title">{{ title }}</h3>
      <div class="admin-table-card-head-right">
        <slot name="titleRight" />
      </div>
    </div>
    <slot name="summary" />
    <slot name="toolbar" />
    <slot />
  </div>
</template>

<script>
export default {
  name: 'AdminTableCard',
  props: {
    title: {
      type: String,
      default: ''
    }
  }
}
</script>
