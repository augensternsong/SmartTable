<template>
  <div class="login-container">
    <n-card class="login-card" :bordered="false" size="large">
      <div class="login-title">个人情况表单系统</div>
      <div class="login-subtitle">请登录您的账号</div>
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="top" size="large">
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="form.username" placeholder="请输入用户名" :input-props="{ autocomplete: 'username' }" />
        </n-form-item>
        <n-form-item label="密码" path="password">
          <n-input
            v-model:value="form.password"
            type="password"
            show-password-on="click"
            placeholder="请输入密码"
            :input-props="{ autocomplete: 'current-password' }"
            @keyup.enter="handleLogin"
          />
        </n-form-item>
        <n-button type="primary" block size="large" :loading="loading" @click="handleLogin">
          登 录
        </n-button>
      </n-form>
      <div class="login-tip">
        默认管理员: <strong>admin / admin123</strong>
      </div>
    </n-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
const rules = {
  username: { required: true, message: '请输入用户名', trigger: ['input', 'blur'] },
  password: { required: true, message: '请输入密码', trigger: ['input', 'blur'] }
}

async function handleLogin() {
  try {
    await formRef.value?.validate()
  } catch (_) {
    return
  }
  loading.value = true
  try {
    await auth.login({ username: form.username, password: form.password })
    message.success('登录成功')
    const redirect = route.query.redirect || '/dashboard'
    router.replace(redirect)
  } catch (e) {
    message.error(e.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}
.login-card {
  width: 380px;
  border-radius: 8px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}
.login-title {
  font-size: 22px;
  font-weight: 600;
  text-align: center;
  color: #2080f0;
  margin-bottom: 4px;
}
.login-subtitle {
  text-align: center;
  color: #999;
  margin-bottom: 24px;
  font-size: 13px;
}
.login-tip {
  margin-top: 16px;
  font-size: 12px;
  color: #999;
  text-align: center;
}
</style>
