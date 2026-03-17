<template>
  <div class="login-page">
    <div class="login-topbar">
      <div class="topbar-inner">
        <span>欢迎访问高校大型仪器共享平台</span>
        <span>服务时间：周一至周五 08:30-17:30</span>
      </div>
    </div>

    <div class="login-bg"></div>

    <div class="login-shell">
      <section class="login-brand">
        <div class="brand-mark">
          <img
            v-if="!logoLoadFailed"
            :src="schoolLogoSrc"
            alt="学校校徽"
            class="brand-mark-logo"
            @error="logoLoadFailed = true"
          />
          <div v-else class="brand-mark-fallback">校</div>
        </div>
        <div class="brand-title">高校大型仪器共享平台</div>
        <div class="brand-subtitle">统一预约 | 审核流转 | 结算管理</div>
        <div class="brand-tips">
          <div class="tip-item">支持上机预约与送样预约双流程</div>
          <div class="tip-item">支持部门管理员与仪器负责人协同管理</div>
          <div class="tip-item">支持充值、结算、退款全链路追踪</div>
        </div>
      </section>

      <section class="login-card">
        <div class="card-kicker">用户入口</div>
        <h2 class="card-title">账号登录</h2>
        <p class="card-subtitle">请输入账号和密码进入系统</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @keyup.enter="submit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="form.username" maxlength="50" placeholder="请输入用户名" />
          </el-form-item>

          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              show-password
              maxlength="100"
              placeholder="请输入密码"
            />
          </el-form-item>

          <el-button
            type="primary"
            class="submit-btn"
            :loading="submitting"
            @click="submit"
          >
            登录系统
          </el-button>
        </el-form>

        <div class="login-footer">提示：请使用已分配账号登录</div>
      </section>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { login } from '../../api/auth'

export default {
  name: 'LoginPage',
  data() {
    return {
      logoLoadFailed: false,
      submitting: false,
      form: {
        username: '',
        password: ''
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  computed: {
    schoolLogoSrc() {
      return `${process.env.BASE_URL || '/'}school-logo.png`
    }
  },
  methods: {
    async submit() {
      if (this.submitting) {
        return
      }
      const formRef = this.$refs.formRef
      if (!formRef) {
        return
      }
      const valid = await formRef.validate().catch(() => false)
      if (!valid) {
        return
      }
      this.submitting = true
      try {
        const result = await login(this.form)
        this.$store.commit('setAuth', {
          token: result.token,
          user: result.user
        })
        ElMessage.success('登录成功')
        const redirect = this.$route.query.redirect
        this.$router.replace(redirect || '/home')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 62px 16px 28px;
  overflow: hidden;
  background: #e8eef8;
}

.login-topbar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 38px;
  background: #0a3f84;
  color: #dbe8fb;
  z-index: 2;
}

.topbar-inner {
  width: min(1080px, calc(100% - 24px));
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.login-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 22%, rgba(43, 103, 185, 0.2), transparent 38%),
    radial-gradient(circle at 78% 12%, rgba(24, 82, 160, 0.2), transparent 34%),
    linear-gradient(140deg, #edf3fd 0%, #dce8fa 100%);
}

.login-shell {
  width: min(1080px, 100%);
  min-height: 620px;
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 1.2fr 0.95fr;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #cfddf2;
  border-radius: 8px;
  box-shadow: 0 14px 30px rgba(15, 45, 90, 0.12);
  overflow: hidden;
}

.login-brand {
  padding: 56px 56px 44px;
  color: #0f3977;
  background:
    linear-gradient(165deg, rgba(11, 78, 162, 0.1), rgba(11, 78, 162, 0.02)),
    #f4f8ff;
  border-right: 1px solid #d7e3f5;
}

.brand-mark {
  width: 74px;
  height: 74px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border: 1px solid #d2e1f4;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 8px 18px rgba(11, 78, 162, 0.24);
}

.brand-mark-logo {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #fff;
}

.brand-mark-fallback {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 34px;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(135deg, #1f5fae, #0b4ea2);
}

.brand-title {
  margin-top: 24px;
  font-size: 36px;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: 1px;
}

.brand-subtitle {
  margin-top: 12px;
  color: #426796;
  font-size: 16px;
}

.brand-tips {
  margin-top: 40px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tip-item {
  padding: 12px 14px;
  border: 1px solid #d2e0f4;
  border-radius: 6px;
  background: #fff;
  color: #335781;
  font-size: 14px;
}

.login-card {
  padding: 64px 54px 40px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.card-kicker {
  display: inline-flex;
  width: fit-content;
  margin-bottom: 8px;
  padding: 4px 10px;
  border-radius: 999px;
  border: 1px solid #d3e0f2;
  background: #f2f7ff;
  color: #3f5f86;
  font-size: 12px;
}

.card-title {
  margin: 0;
  font-size: 30px;
  color: #17427e;
}

.card-subtitle {
  margin: 10px 0 26px;
  color: #6d84a3;
  font-size: 14px;
}

.submit-btn {
  width: 100%;
  margin-top: 6px;
  height: 44px;
  font-size: 15px;
  letter-spacing: 1px;
}

.login-footer {
  margin-top: 16px;
  color: #8194af;
  font-size: 12px;
  text-align: center;
}

@media (max-width: 980px) {
  .login-page {
    padding-top: 52px;
  }

  .topbar-inner {
    font-size: 11px;
  }

  .login-shell {
    grid-template-columns: 1fr;
    min-height: 0;
  }

  .login-brand {
    border-right: none;
    border-bottom: 1px solid #d7e3f5;
    padding: 34px 24px 26px;
  }

  .brand-title {
    font-size: 28px;
  }

  .login-card {
    padding: 32px 24px 28px;
  }
}
</style>
