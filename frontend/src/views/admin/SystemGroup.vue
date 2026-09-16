<template>
  <n-card :bordered="false">
    <n-space align="center" class="mb" justify="space-between">
      <n-space align="center">
        <n-input v-model:value="keyword" placeholder="分组编码/名称" clearable style="width: 220px" @keyup.enter="search" />
        <n-button type="primary" @click="search">查询</n-button>
      </n-space>
      <n-button v-if="hasPerm('sys:group:create')" type="primary" @click="openCreate">新建分组</n-button>
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
    <n-modal v-model:show="formVisible" preset="card" :title="formMode === 'create' ? '新建分组' : '编辑分组'" style="width: 520px">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="90">
        <n-form-item label="分组编码" path="groupCode">
          <n-input v-model:value="form.groupCode" :disabled="formMode === 'edit'" placeholder="字母开头, 字母数字下划线连字符" />
        </n-form-item>
        <n-form-item label="分组名称" path="groupName">
          <n-input v-model:value="form.groupName" placeholder="分组名称" />
        </n-form-item>
        <n-form-item label="状态" path="status">
          <n-radio-group v-model:value="form.status">
            <n-radio :value="1">启用</n-radio>
            <n-radio :value="0">禁用</n-radio>
          </n-radio-group>
        </n-form-item>
        <n-form-item label="描述" path="description">
          <n-input v-model:value="form.description" type="textarea" :autosize="{ minRows: 2, maxRows: 4 }" />
        </n-form-item>
      </n-form>
      <template #footer>
        <n-space justify="end">
          <n-button @click="formVisible = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="save">保存</n-button>
        </n-space>
      </template>
    </n-modal>

    <!-- 分配用户 -->
    <n-modal v-model:show="userVisible" preset="card" title="分配用户" style="width: 520px">
      <n-input v-model:value="userFilter" placeholder="搜索用户名/昵称" clearable size="small" class="mb" />
      <n-spin :show="userLoading">
        <n-checkbox-group v-model:value="selectedUserIds">
          <n-space vertical style="max-height: 360px; overflow: auto">
            <n-checkbox
              v-for="u in filteredUsers"
              :key="u.id"
              :value="u.id"
              :label="`${u.nickname ? u.nickname + ' / ' : ''}${u.username}`"
            />
            <n-empty v-if="!filteredUsers.length" description="暂无用户" size="small" />
          </n-space>
        </n-checkbox-group>
      </n-spin>
      <template #footer>
        <n-space justify="end">
          <n-button @click="userVisible = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="saveUsers">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup>
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useMessage, NButton, NSpace, NTag, NPopconfirm } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import {
  pageGroups,
  createGroup,
  updateGroup,
  deleteGroup,
  assignUsers,
  getGroup
} from '@/api/group'
import { pageUsers } from '@/api/user'

const auth = useAuthStore()
const message = useMessage()
const hasPerm = (c) => auth.isSuperAdmin || auth.permissions.includes(c)

const keyword = ref('')
const query = reactive({ page: 1, size: 10 })
const list = ref([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 10, itemCount: 0, showSizePicker: false })

const columns = [
  { title: '分组编码', key: 'groupCode', width: 180 },
  { title: '分组名称', key: 'groupName', width: 180 },
  { title: '成员数', key: 'memberCount', width: 90 },
  { title: '状态', key: 'status', width: 80, render: (r) => statusTag(r.status) },
  { title: '描述', key: 'description', render: (r) => r.description || '-' },
  { title: '创建时间', key: 'createdAt', width: 160, render: (r) => formatTime(r.createdAt) },
  {
    title: '操作',
    key: 'actions',
    width: 240,
    fixed: 'right',
    render: (row) => {
      const actions = []
      if (hasPerm('sys:group:update')) {
        actions.push(h(NButton, { size: 'small', text: true, onClick: () => openEdit(row) }, { default: () => '编辑' }))
      }
      if (hasPerm('sys:group:assign-user')) {
        actions.push(h(NButton, { size: 'small', text: true, type: 'info', onClick: () => openAssignUsers(row) }, { default: () => '分配用户' }))
      }
      if (hasPerm('sys:group:delete')) {
        actions.push(
          h(NPopconfirm, { onPositiveClick: () => remove(row) }, {
            default: () => '删除分组将解除全部成员关联, 确认?',
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
const form = reactive({ id: null, groupCode: '', groupName: '', status: 1, description: '' })
const rules = {
  groupCode: { required: true, message: '请输入分组编码', trigger: ['input', 'blur'] },
  groupName: { required: true, message: '请输入分组名称', trigger: ['input', 'blur'] }
}

function openCreate() {
  formMode.value = 'create'
  Object.assign(form, { id: null, groupCode: '', groupName: '', status: 1, description: '' })
  formVisible.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    groupCode: row.groupCode,
    groupName: row.groupName,
    status: row.status,
    description: row.description || ''
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
      groupCode: form.groupCode,
      groupName: form.groupName,
      status: form.status,
      description: form.description
    }
    if (formMode.value === 'create') {
      await createGroup(payload)
      message.success('创建成功')
    } else {
      await updateGroup(form.id, payload)
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
    await deleteGroup(row.id)
    message.success('删除成功')
    await load()
  } catch (e) {
    message.error(e.message)
  }
}

// 分配用户
const userVisible = ref(false)
const userLoading = ref(false)
const allUsers = ref([])
const selectedUserIds = ref([])
const currentGroupId = ref(null)
const userFilter = ref('')

const filteredUsers = computed(() => {
  const kw = userFilter.value.trim().toLowerCase()
  if (!kw) return allUsers.value
  return allUsers.value.filter(
    (u) => u.username.toLowerCase().includes(kw) || (u.nickname || '').toLowerCase().includes(kw)
  )
})

async function openAssignUsers(row) {
  currentGroupId.value = row.id
  userVisible.value = true
  userFilter.value = ''
  userLoading.value = true
  try {
    const [detail, users] = await Promise.all([
      getGroup(row.id),
      allUsers.value.length ? Promise.resolve(null) : pageUsers({ page: 1, size: 10000 })
    ])
    if (users) {
      allUsers.value = users.records || []
    }
    selectedUserIds.value = detail.userIds || []
  } catch (e) {
    message.error(e.message)
  } finally {
    userLoading.value = false
  }
}

async function saveUsers() {
  saving.value = true
  try {
    await assignUsers(currentGroupId.value, selectedUserIds.value)
    message.success('分配成功')
    userVisible.value = false
    await load()
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

function handlePage(page) {
  query.page = page
  load()
}

async function load() {
  loading.value = true
  try {
    const params = { page: query.page, size: query.size }
    if (keyword.value) params.keyword = keyword.value
    const data = await pageGroups(params)
    list.value = data.records || []
    pagination.itemCount = data.total || 0
    pagination.page = query.page
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.mb {
  margin-bottom: 16px;
}
</style>
