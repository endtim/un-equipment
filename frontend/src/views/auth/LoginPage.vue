<template>
  <div class="page-shell login-shell">
    <div class="hero-card login-card">
      <div class="section-title">用户登录</div>
      <el-alert
        title="演示账号可使用：admin / owner_zhang / teacher_wang，默认密码 123456"
        type="info"
        :closable="false"
        style="margin-bottom: 18px;"
      />
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
        </el-form-item>
        <el-button type="primary" style="width: 100%;" @click="submit">登录</el-button>
      </el-form>
    </div>
  </div>
</template>

<script>
import { login } from '../../api/auth'

export default {
  data() {
    return {
      form: {
        username: 'teacher_wang',
        password: '123456'
      },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
      }
    }
  },
  methods: {
    async submit() {
      await this.$refs.formRef.validate()
      const data = await login(this.form)
      this.$store.commit('setAuth', data)
      const redirect = this.$route.query.redirect
      this.$router.push(redirect || '/home')
    }
  }
}
</script>

<style scoped>
.login-shell {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.login-card {
  width: 420px;
  padding: 28px;
}
</style>
