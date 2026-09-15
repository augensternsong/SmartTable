<template>
  <n-card title="个人中心" :bordered="false">
    <n-descriptions :column="2" label-placement="left" bordered>
      <n-descriptions-item label="用户名">{{ auth.userInfo?.username }}</n-descriptions-item>
      <n-descriptions-item label="昵称">{{ auth.userInfo?.nickname || '-' }}</n-descriptions-item>
      <n-descriptions-item label="邮箱">{{ auth.userInfo?.email || '-' }}</n-descriptions-item>
      <n-descriptions-item label="手机">{{ auth.userInfo?.phone || '-' }}</n-descriptions-item>
      <n-descriptions-item label="角色">
        <n-space>
          <n-tag v-for="r in auth.roles" :key="r" type="info" size="small">{{ r }}</n-tag>
        </n-space>
      </n-descriptions-item>
      <n-descriptions-item label="权限数">{{ auth.permissions.length }}</n-descriptions-item>
    </n-descriptions>

    <n-divider />

    <n-h3>修改密码</n-h3>
    <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="100">
      <n-form-item label="原密码" path="oldPassword">
        <n-input v-model:value="form.oldPassword" type="password" show-password-on="click" />
      </n-form-item>
      <n-form-item label="新密码" path="newPassword">
        <n-input v-model:value="form.newPassword" type="password" show-password-on="click" />
      </n-form-item>
      <n-form-item label="确认密码" path="confirm">
        <n-input v-model:value="form.confirm" type="password" show-password-on="click" />
      </n-form-item>
      <n-space>
        <n-button type="primary" :loading="loading" @click="submit">提交</n-button>
        <n-button @click="reset">重置</n-button>
      </n-space>
    </n-form>
  </n-card>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useMessage } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import { changeMyPassword } from '@/api/user'

const auth = useAuthStore()
const message = useMessage()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const rules = {
  oldPassword: { required: true, message: '请输入原密码', trigger: ['input', 'blur'] },
  newPassword: {
    required: true,
    trigger: ['input', 'blur'],
    validator: (rule, v) => {
      if (!v) return new Error('请输入新密码')
      if (v.length < 6 || v.length > 32) return new Error('新密码长度6-32')
      return true
    }
  },
  confirm: {
    required: true,
    trigger: ['input', 'blur'],
    validator: (rule, v) => {
      if (!v) return new Error('请再次输入新密码')
      if (v !== form.newPassword) return new Error('两次输入的密码不一致')
      return true
    }
  }
}

async function submit() {
  try {
    await formRef.value?.validate()
  } catch (_) {
    return
  }
  loading.value = true
  try {
    await changeMyPassword({ oldPassword: form.oldPassword, newPassword: form.newPassword })
    message.success('密码修改成功, 请重新登录')
    await auth.logout()
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

function reset() {
  form.oldPassword = ''
  form.newPassword = ''
  form.confirm = ''
}
</script>
