<template>
  <n-card :bordered="false">
    <n-space align="center" class="mb" justify="space-between">
      <n-space align="center">
        <n-input v-model:value="query.username" placeholder="用户名" clearable style="width: 160px" @keyup.enter="search" />
        <n-input v-model:value="query.nickname" placeholder="昵称" clearable style="width: 160px" @keyup.enter="search" />
        <n-select v-model:value="query.roleId" :options="roleOptions" placeholder="角色" clearable style="width: 160px" />
        <n-select v-model:value="query.groupId" :options="groupOptions" placeholder="分组" clearable style="width: 160px" />
        <n-select v-model:value="query.status" :options="statusOptions" placeholder="状态" clearable style="width: 110px" />
        <n-button type="primary" @click="search">查询</n-button>
        <n-button @click="resetQuery">重置</n-button>
      </n-space>
      <n-button v-if="hasPerm('sys:user:create')" type="primary" @click="openCreate">新建用户</n-button>
    </n-space>

    <n-data-table
      :columns="columns"
      :data="list"
      :loading="loading"
      :pagination="pagination"
      remote
      @update:page="handlePage"
    />

    <!-- 新建/编辑 -->
    <n-modal v-model:show="formVisible" preset="card" :title="formMode === 'create' ? '新建用户' : '编辑用户'" style="width: 520px">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="90">
        <n-form-item label="用户名" path="username">
          <n-input v-model:value="form.username" :disabled="formMode === 'edit'" placeholder="字母数字下划线点连字符" />
        </n-form-item>
        <n-form-item label="昵称" path="nickname">
          <n-input v-model:value="form.nickname" placeholder="昵称" />
        </n-form-item>
        <n-form-item :label="formMode === 'create' ? '初始密码' : '新密码'" path="password">
          <n-input
            v-model:value="form.password"
            type="password"
            show-password-on="click"
            :placeholder="formMode === 'create' ? '必填, 6-32位' : '留空表示不修改'"
          />
        </n-form-item>
        <n-form-item label="邮箱" path="email">
          <n-input v-model:value="form.email" placeholder="邮箱" />
        </n-form-item>
        <n-form-item label="手机号" path="phone">
          <n-input v-model:value="form.phone" placeholder="手机号" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="form.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">禁用</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="备注" path="remark">
          <n-input v-model:value="form.remark" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="formVisible = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="save">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 分配角色 -->
    <n-modal v-model:show="roleVisible" preset="card" title="分配角色" style="width: 460px">
      <n-checkbox-group v-model:value="selectedRoleIds">
        <n-space vertical>
          <n-checkbox
            v-for="r in roleList"
            :key="r.id"
            :value="r.id"
            :disabled="disabledRoleIds.includes(r.id)"
            :label="`${r.roleName} (${r.roleCode})`"
          />
        </n-space>
      </n-checkbox-group>
      <template #footer>
        <n-space justify="end">
          <n-button @click="roleVisible = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="saveRoles">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 分配分组 -->
    <n-modal v-model:show="groupAssignVisible" preset="card" title="分配分组" style="width: 460px">
      <n-checkbox-group v-model:value="selectedGroupIds">
        <n-space vertical>
          <n-checkbox v-for="g in groupList" :key="g.id" :value="g.id" :label="`${g.groupName} (${g.groupCode})`" />
          <n-empty v-if="!groupList.length" description="尚无分组, 请先创建" size="small" />
        </n-space>
      </n-checkbox-group>
      <template #footer>
        <n-space justify="end">
          <n-button @click="groupAssignVisible = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="saveGroups">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 重置密码 -->
    <n-modal v-model:show="pwdVisible" preset="card" title="重置密码" style="width: 420px">
      <n-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-placement="left" label-width="90">
        <n-form-item label="新密码" path="newPassword">
          <n-input v-model:value="pwdForm.newPassword" type="password" show-password-on="click" placeholder="6-32位" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="pwdVisible = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="savePassword">确定</n-button>
        </n-space>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { useMessage, NButton, NSpace, NTag, NPopconfirm } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import {
  pageUsers,
  createUser,
  updateUser,
  deleteUser,
  assignRoles,
  assignGroups,
  resetPassword,
  getEditableRoles
} from '@/api/user'
import { allRoles } from '@/api/role'
import { allGroups } from '@/api/group'

const auth = useAuthStore()
const message = useMessage()
const hasPerm = (c) => auth.isSuperAdmin || auth.permissions.includes(c)

const statusOptions = [
  { label: '启用', value: 1 },
  { label: '禁用', value: 0 }
]

const query = reactive({ page: 1, size: 10, username: '', nickname: '', roleId: null, groupId: null, status: null })
const list = ref([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 10, itemCount: 0, showSizePicker: false })

const roleList = ref([])
const groupList = ref([])
const disabledRoleIds = ref([])
const roleOptions = ref([])
const groupOptions = ref([])

const columns = [
  { title: '用户名', key: 'username', width: 130 },
  { title: '昵称', key: 'nickname', width: 120, render: (r) => r.nickname || '-' },
  { title: '角色', key: 'roleCodes', render: (r) => (r.roleCodes || []).join('、') || '-' },
  { title: '手机号', key: 'phone', width: 130, render: (r) => r.phone || '-' },
  { title: '状态', key: 'status', width: 80, render: (r) => statusTag(r.status) },
  { title: '创建时间', key: 'createdAt', width: 160, render: (r) => formatTime(r.createdAt) },
  {
    title: '操作',
    key: 'actions',
    width: 300,
    fixed: 'right',
    render: (row) => {
      const actions = []
      if (hasPerm('sys:user:update')) {
        actions.push(h(NButton, { size: 'small', text: true, onClick: () => openEdit(row) }, { default: () => '编辑' }))
        actions.push(h(NButton, { size: 'small', text: true, type: 'info', onClick: () => openAssignRoles(row) }, { default: () => '分配角色' }))
        actions.push(h(NButton, { size: 'small', text: true, type: 'info', onClick: () => openAssignGroups(row) }, { default: () => '分配分组' }))
      }
      if (hasPerm('sys:user:reset-pwd')) {
        actions.push(h(NButton, { size: 'small', text: true, type: 'warning', onClick: () => openResetPwd(row) }, { default: () => '重置密码' }))
      }
      if (hasPerm('sys:user:delete') && row.id !== auth.userInfo?.id && !(row.roleCodes || []).includes('SUPER_ADMIN')) {
        actions.push(
          h(NPopconfirm, { onPositiveClick: () => remove(row) }, {
            default: () => '确认删除该用户?',
            trigger: () => h(NButton, { size: 'small', text: true, type: 'error' }, { default: () => '删除' })
          })
        )
      }
      return h(NSpace, { size: 'small' }, { default: () => actions })
    }
  }
]

function statusTag(s) {
  const enabled = s === 1
  return h(NTag, { size: 'small', type: enabled ? 'success' : 'error', round: true }, {
    default: () => (enabled ? '启用' : '禁用')
  })
}

function formatTime(t) {
  return t ? new Date(t).toLocaleString('zh-CN', { hour12: false }) : '-'
}

// 新建/编辑
const formVisible = ref(false)
const formMode = ref('create')
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  username: '',
  nickname: '',
  password: '',
  email: '',
  phone: '',
  status: 1,
  remark: ''
})
const rules = {
  username: { required: true, message: '请输入用户名', trigger: ['input', 'blur'] },
  password: {
    validator: (_rule, value) => {
      if (formMode.value === 'create' && !value) return new Error('新建用户必须设置初始密码')
      if (value && (value.length < 6 || value.length > 32)) return new Error('密码长度6-32位')
      return true
    },
    trigger: ['input', 'blur']
  },
  email: {
    validator: (_rule, value) => !value || /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value) || new Error('邮箱格式不正确'),
    trigger: ['input', 'blur']
  }
}

function openCreate() {
  formMode.value = 'create'
  Object.assign(form, { id: null, username: '', nickname: '', password: '', email: '', phone: '', status: 1, remark: '' })
  formVisible.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    username: row.username,
    nickname: row.nickname || '',
    password: '',
    email: row.email || '',
    phone: row.phone || '',
    status: row.status,
    remark: row.remark || ''
  })
  formVisible.value = true
}

async function save() {
  try {
    await formRef.value?.validate()
  } catch (_) {
    return
  }
  saving.value = true
  try {
    const payload = {
      username: form.username,
      nickname: form.nickname,
      email: form.email,
      phone: form.phone,
      status: form.status,
      remark: form.remark
    }
    if (form.password) payload.password = form.password
    if (formMode.value === 'create') {
      payload.password = form.password
      await createUser(payload)
      message.success('创建成功')
    } else {
      await updateUser(form.id, payload)
      message.success('更新成功')
    }
    formVisible.value = false
    await load()
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
}

async function remove(row) {
  try {
    await deleteUser(row.id)
    message.success('删除成功')
    await load()
  } catch (e) {
    message.error(e.message)
  }
}

// 分配角色
const roleVisible = ref(false)
const selectedRoleIds = ref([])
const currentUserId = ref(null)

async function openAssignRoles(row) {
  currentUserId.value = row.id
  selectedRoleIds.value = [...(row.roleIds || [])]
  roleVisible.value = true
}

async function saveRoles() {
  saving.value = true
  try {
    await assignRoles(currentUserId.value, selectedRoleIds.value)
    message.success('分配成功')
    roleVisible.value = false
    await load()
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
}

// 分配分组
const groupAssignVisible = ref(false)
const selectedGroupIds = ref([])

async function openAssignGroups(row) {
  currentUserId.value = row.id
  selectedGroupIds.value = [...(row.groupIds || [])]
  groupAssignVisible.value = true
}

async function saveGroups() {
  saving.value = true
  try {
    await assignGroups(currentUserId.value, selectedGroupIds.value)
    message.success('分配成功')
    groupAssignVisible.value = false
    await load()
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
}

// 重置密码
const pwdVisible = ref(false)
const pwdFormRef = ref(null)
const pwdForm = reactive({ id: null, newPassword: '' })
const pwdRules = {
  newPassword: {
    required: true,
    validator: (_rule, value) => {
      if (!value) return new Error('请输入新密码')
      if (value.length < 6 || value.length > 32) return new Error('密码长度6-32位')
      return true
    },
    trigger: ['input', 'blur']
  }
}

function openResetPwd(row) {
  pwdForm.id = row.id
  pwdForm.newPassword = ''
  pwdVisible.value = true
}

async function savePassword() {
  try {
    await pwdFormRef.value?.validate()
  } catch (_) {
    return
  }
  saving.value = true
  try {
    await resetPassword(pwdForm.id, pwdForm.newPassword)
    message.success('重置成功')
    pwdVisible.value = false
  } catch (e) {
    message.error(e.message)
  } finally {
    saving.value = false
  }
}

function search() {
  query.page = 1
  load()
}

function resetQuery() {
  Object.assign(query, { page: 1, username: '', nickname: '', roleId: null, groupId: null, status: null })
  load()
}

function handlePage(page) {
  query.page = page
  load()
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (query.username) params.username = query.username
    if (query.nickname) params.nickname = query.nickname
    if (query.roleId) params.roleId = query.roleId
    if (query.groupId) params.groupId = query.groupId
    if (query.status !== null) params.status = query.status
    const data = await pageUsers(params)
    list.value = data.records || []
    pagination.itemCount = data.total || 0
    pagination.page = query.page
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  try {
    const [roles, groups, editable] = await Promise.all([
      allRoles(),
      allGroups(),
      hasPerm('sys:user:update') ? getEditableRoles() : Promise.resolve({ disabledRoleIds: [] })
    ])
    roleList.value = roles || []
    groupList.value = groups || []
    disabledRoleIds.value = editable.disabledRoleIds || []
    roleOptions.value = (roles || []).map((r) => ({ label: `${r.roleName} (${r.roleCode})`, value: r.id }))
    groupOptions.value = (groups || []).map((g) => ({ label: `${g.groupName} (${g.groupCode})`, value: g.id }))
  } catch (e) {
    message.error(e.message)
  }
  load()
})
</script>

<style scoped>
.mb {
  margin-bottom: 16px;
}
</style>
