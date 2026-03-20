<template>
  <div class="register-page">
    <div class="register-topbar">
      <div class="topbar-inner">
        <span>欢迎访问高校大型仪器共享平台</span>
        <span>校外用户注册入口</span>
      </div>
    </div>

    <div class="register-bg"></div>

    <div class="register-shell">
      <section class="register-brand">
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
        <div class="brand-title">校外用户注册</div>
        <div class="brand-subtitle">校内用户账号由管理员统一分发，不开放个人注册</div>
      </section>

      <section class="register-card">
        <div class="card-kicker">注册入口</div>
        <h2 class="card-title">提交校外申请</h2>
        <p class="card-subtitle">注册成功后需管理员审核，审核通过后方可登录</p>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          @keyup.enter="submit"
        >
          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="用户名" prop="username">
                <el-input v-model="form.username" maxlength="50" placeholder="请输入用户名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="姓名" prop="realName">
                <el-input v-model="form.realName" maxlength="50" placeholder="请输入姓名" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="密码" prop="password">
                <el-input
                  v-model="form.password"
                  type="password"
                  show-password
                  maxlength="50"
                  placeholder="请输入密码"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="确认密码" prop="confirmPassword">
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  show-password
                  maxlength="50"
                  placeholder="请再次输入密码"
                />
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="12">
            <el-col :span="12">
              <el-form-item label="手机号" prop="phone">
                <el-input v-model="form.phone" maxlength="11" placeholder="请输入手机号" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="邮箱" prop="email">
                <el-input v-model="form.email" maxlength="100" placeholder="请输入邮箱（选填）" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="申请单位" prop="unitName">
            <el-input v-model="form.unitName" maxlength="100" placeholder="请输入单位全称" />
          </el-form-item>

          <div class="register-actions">
            <el-button @click="$router.push('/login')">返回登录</el-button>
            <el-button type="primary" :loading="submitting" @click="submit">提交注册</el-button>
          </div>
        </el-form>
      </section>
    </div>
  </div>
</template>

<script>
import { ElMessage } from 'element-plus'
import { register } from '../../api/auth'

export default {
  name: 'RegisterPage',
  data() {
    return {
      logoLoadFailed: false,
      submitting: false,
      form: {
        username: '',
        password: '',
        confirmPassword: '',
        realName: '',
        phone: '',
        email: '',
        unitName: ''
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
        confirmPassword: [
          {
            required: true,
            validator: (_, value, callback) => {
              if (!value) {
                callback(new Error('请再次输入密码'))
                return
              }
              if (value !== this.form.password) {
                callback(new Error('两次输入的密码不一致'))
                return
              }
              callback()
            },
            trigger: 'blur'
          }
        ],
        realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
        phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
        unitName: [{ required: true, message: '请输入申请单位', trigger: 'blur' }],
        email: [
          {
            validator: (_, value, callback) => {
              if (!value) {
                callback()
                return
              }
              const ok = /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)
              callback(ok ? undefined : new Error('邮箱格式不正确'))
            },
            trigger: 'blur'
          }
        ]
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
      const valid = await this.$refs.formRef.validate().catch(() => false)
      if (!valid) {
        return
      }
      this.submitting = true
      try {
        await register({
          username: this.form.username,
          password: this.form.password,
          realName: this.form.realName,
          phone: this.form.phone,
          email: this.form.email,
          unitName: this.form.unitName
        })
        ElMessage.success('注册申请已提交，请等待管理员审核')
        this.$router.push('/login')
      } finally {
        this.submitting = false
      }
    }
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 62px 16px 28px;
  overflow: hidden;
  background: #e8eef8;
}

.register-topbar {
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
  width: min(1180px, calc(100% - 24px));
  height: 100%;
  margin: 0 auto;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.register-bg {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 18% 22%, rgba(43, 103, 185, 0.2), transparent 38%),
    radial-gradient(circle at 78% 12%, rgba(24, 82, 160, 0.2), transparent 34%),
    linear-gradient(140deg, #edf3fd 0%, #dce8fa 100%);
}

.register-shell {
  width: min(1180px, 100%);
  min-height: 640px;
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: 0.9fr 1.3fr;
  background: rgba(255, 255, 255, 0.96);
  border: 1px solid #cfddf2;
  border-radius: 8px;
  box-shadow: 0 14px 30px rgba(15, 45, 90, 0.12);
  overflow: hidden;
}

.register-brand {
  padding: 48px 46px;
  color: #0f3977;
  background: linear-gradient(165deg, rgba(11, 78, 162, 0.1), rgba(11, 78, 162, 0.02)), #f4f8ff;
  border-right: 1px solid #d7e3f5;
}

.brand-mark {
  width: 82px;
  height: 82px;
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
  margin-top: 22px;
  font-size: 34px;
  line-height: 1.2;
  font-weight: 700;
}

.brand-subtitle {
  margin-top: 12px;
  color: #426796;
  font-size: 15px;
  line-height: 1.6;
}

.register-card {
  padding: 40px 46px;
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
  margin: 10px 0 18px;
  color: #6d84a3;
  font-size: 14px;
}

.register-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 12px;
}

@media (max-width: 980px) {
  .register-shell {
    grid-template-columns: 1fr;
  }

  .register-brand {
    border-right: none;
    border-bottom: 1px solid #d7e3f5;
  }
}
</style>
