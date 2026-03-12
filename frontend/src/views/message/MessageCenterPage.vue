<template>
  <div class="content-card" style="padding: 24px;">
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px;">
      <div class="section-title" style="margin: 0;">消息中心</div>
      <el-button type="primary" plain @click="markAllRead">全部标记已读</el-button>
    </div>
    <el-table :data="messages" border>
      <el-table-column prop="title" label="消息标题" min-width="180" />
      <el-table-column prop="content" label="消息内容" />
      <el-table-column label="状态" width="120">
        <template #default="{ row }">
          <el-tag v-if="row.readStatus === 0" type="danger">未读</el-tag>
          <el-tag v-else type="success">已读</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="接收时间" width="180" :formatter="formatDateTimeCell" />
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.readStatus === 0" link type="primary" @click="markRead(row)">标记已读</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
import { getMessages, markAllMessagesRead, markMessageRead } from '../../api/content'
import { formatDateTime } from '../../utils/datetime'

export default {
  data() {
    return {
      messages: []
    }
  },
  async created() {
    await this.load()
  },
  methods: {
    formatDateTimeCell(row, column, value) {
      return formatDateTime(value)
    },
    async load() {
      this.messages = await getMessages()
    },
    async markRead(row) {
      await markMessageRead(row.id)
      this.$message.success('消息已标记为已读')
      await this.load()
    },
    async markAllRead() {
      await markAllMessagesRead()
      this.$message.success('全部消息已标记为已读')
      await this.load()
    }
  }
}
</script>
