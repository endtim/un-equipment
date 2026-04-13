<template>
  <div class="portal-subpage">
    <div class="content-card sub-section info-banner">
      <div class="section-title">项目基本信息</div>
      <div class="info-table">
        <div class="info-row"><span>系统名称</span><strong>{{ siteInfo.systemName }}</strong></div>
        <div class="info-row"><span>版本号</span><strong>{{ siteInfo.version }}</strong></div>
        <div class="info-row"><span>指导老师</span><strong>{{ siteInfo.supervisorName }}</strong></div>
        <div class="info-row"><span>设计者</span><strong>{{ siteInfo.designerName }}</strong></div>
        <div class="info-row"><span>年级班级</span><strong>{{ siteInfo.gradeClass }}</strong></div>
        <div class="info-row"><span>专业</span><strong>{{ siteInfo.major }}</strong></div>
        <div class="info-row"><span>学校</span><strong>{{ siteInfo.school }}</strong></div>
        <div class="info-row"><span>学院</span><strong>{{ siteInfo.college }}</strong></div>
        <div class="info-row"><span>设计时间</span><strong>{{ siteInfo.designTime }}</strong></div>
        <div class="info-row info-row--wide"><span>版权信息</span><strong>{{ siteInfo.copyright }}</strong></div>
      </div>
    </div>

    <div class="content-card sub-section">
      <div class="section-title">建设目标</div>
      <p class="paragraph">
        高校大型仪器共享平台以“统一管理、开放共享、规范预约、服务科研”为建设目标，
        面向校内外用户提供大型仪器预约、送样测试、使用记录、费用结算、公告发布等综合服务。
      </p>
      <p class="paragraph">
        平台通过整合校级公共平台、院级测试平台和重点实验室资源，实现仪器信息集中展示、
        开放时段统一发布、业务流程标准化管理，提升仪器利用率和科研服务效率。
      </p>
    </div>

    <div class="info-grid">
      <div class="content-card sub-section">
        <div class="section-title">服务内容</div>
        <ul class="plain-list">
          <li>仪器开放信息统一发布</li>
          <li>上机预约与送样预约办理</li>
          <li>充值申请与费用结算管理</li>
          <li>通知公告与帮助文档服务</li>
          <li>预约统计与平台运行分析</li>
        </ul>
      </div>
      <div class="content-card sub-section">
        <div class="section-title">开放对象</div>
        <ul class="plain-list">
          <li>校内教师、科研人员、研究生、本科生</li>
          <li>院系实验平台主管与仪器负责人</li>
          <li>经审核通过的校外合作单位用户</li>
          <li>公共测试与分析服务申请单位</li>
        </ul>
      </div>
      <div class="content-card sub-section">
        <div class="section-title">服务原则</div>
        <ul class="plain-list">
          <li>资源共享，优先保障教学科研</li>
          <li>统一流程，规范预约与测试管理</li>
          <li>公开透明，强化服务质量监督</li>
          <li>数据留痕，支撑运行统计和考核</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<script>
import siteInfo from '../../config/siteInfo'

export default {
  computed: {
    siteInfo() {
      // 门户“平台简介”和全站页脚共用同一站点配置，避免多处文案维护不一致。
      return siteInfo
    }
  }
}
</script>

<style scoped>
.portal-subpage {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.sub-section {
  padding: 24px;
}

.info-banner {
  background: linear-gradient(180deg, #fbfdff, #f6faff);
}

.info-table {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 16px;
}

.info-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border-radius: 8px;
  border: 1px solid #dbe5f2;
  background: #fff;
}

.info-row span {
  font-size: 13px;
  color: #6f829a;
}

.info-row strong {
  font-size: 15px;
  line-height: 1.7;
  color: #173d73;
}

.info-row--wide {
  grid-column: 1 / -1;
}

.paragraph {
  margin: 0 0 14px;
  line-height: 1.9;
  color: #44566f;
}
.info-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}
.plain-list {
  margin: 0;
  padding-left: 18px;
  line-height: 2;
  color: #44566f;
}
@media (max-width: 900px) {
  .info-table,
  .info-grid {
    grid-template-columns: 1fr;
  }
}
</style>
