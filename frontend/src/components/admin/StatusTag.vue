<template>
  <el-tag :type="resolvedType">
    {{ resolvedLabel }}
  </el-tag>
</template>

<script>
export default {
  name: 'StatusTag',
  props: {
    value: {
      type: [String, Number],
      default: ''
    },
    label: {
      type: String,
      default: ''
    },
    type: {
      type: String,
      default: ''
    },
    labelMap: {
      type: Object,
      default: () => ({})
    },
    typeMap: {
      type: Object,
      default: () => ({})
    },
    fallbackLabel: {
      type: String,
      default: '未知状态'
    },
    fallbackType: {
      type: String,
      default: 'info'
    }
  },
  computed: {
    resolvedLabel() {
      if (this.label) {
        return this.label
      }
      if (Object.prototype.hasOwnProperty.call(this.labelMap, this.value)) {
        return this.labelMap[this.value]
      }
      return this.fallbackLabel
    },
    resolvedType() {
      if (this.type) {
        return this.type
      }
      if (Object.prototype.hasOwnProperty.call(this.typeMap, this.value)) {
        return this.typeMap[this.value]
      }
      return this.fallbackType
    }
  }
}
</script>
