<template>
  <div class="portal-subpage">
    <div class="members-grid">
      <div v-for="item in members" :key="item.name" class="content-card member-card">
        <div class="member-avatar">{{ item.name.slice(0, 1) }}</div>
        <div class="member-name">{{ item.name }}</div>
        <div class="member-role">{{ item.role }}</div>
        <div class="member-dept">{{ item.dept }}</div>
        <div class="member-desc">{{ item.desc }}</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      members: [
        { name: '张老师', role: '平台负责人', dept: '分析测试中心', desc: '负责平台整体建设、资源协调与开放运行管理。' },
        { name: '李老师', role: '仪器平台主管', dept: '化学化工学院', desc: '负责院级平台仪器开放规则和测试服务调度。' },
        { name: '王老师', role: '仪器负责人', dept: '材料科学学院', desc: '负责重点仪器培训、审核与使用指导。' },
        { name: '赵老师', role: '技术管理员', dept: '生命科学学院', desc: '负责测试数据上传、样品接收与服务支撑。' },
        { name: '陈同学', role: '学生助理', dept: '分析测试中心', desc: '负责预约咨询、文档整理与日常值班。' },
        { name: '孙工', role: '校外联络员', dept: '开放共享办公室', desc: '负责校外单位测试对接与业务咨询。' }
      ]
    }
  }
}
</script>

<style scoped>
.portal-subpage { display: flex; flex-direction: column; gap: 18px; }
.members-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; }
.member-card { padding: 24px; text-align: center; }
.member-avatar { width: 72px; height: 72px; line-height: 72px; margin: 0 auto 16px; border-radius: 50%; background: linear-gradient(180deg, #1664ca, #0b4ea2); color: #fff; font-size: 28px; font-weight: 700; }
.member-name { font-size: 22px; font-weight: 700; color: #193f76; }
.member-role { margin-top: 8px; color: #0b4ea2; }
.member-dept { margin-top: 6px; color: #7b8ea8; }
.member-desc { margin-top: 14px; line-height: 1.8; color: #4e6077; text-align: left; }
@media (max-width: 900px) { .members-grid { grid-template-columns: 1fr; } }
</style>
