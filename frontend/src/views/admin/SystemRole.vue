<template>
  <n-card :bordered="false">
    <n-space align="center" class="mb" justify="space-between">
      <n-space align="center">
        <n-input v-model:value="keyword" placeholder="角色编码/名称" clearable style="width: 220px" @keyup.enter="search" />
        <n-button type="primary" @click="search">查询</n-button>
      </n-space>
      <n-button v-if="hasPerm('sys:role:create')" type="primary" @click="openCreate">新建角色</n-button>
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
    <n-modal v-model:show="formVisible" preset="card" :title="formMode === 'create' ? '新建角色' : '编辑角色'" style="width: 520px">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="90">
        <n-form-item label="角色编码" path="roleCode">
          <n-input v-model:value="form.roleCode" :disabled="formMode === 'edit'" placeholder="大写字母开头, 字母数字下划线" />
        </n-form-item>
        <n-form-item label="角色名称" path="roleName">
          <n-input v-model:value="form.roleName" placeholder="角色名称" />
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

    <!-- 分配权限 -->
    <n-modal v-model:show="permVisible" preset="card" title="分配权限" style="width: 520px">
      <n-spin :show="permLoading">
        <n-tree
          ref="treeRef"
          block-line
          cascade
          checkable
          :data="permTreeData"
          :checked-keys="checkedKeys"
          :default-expand-all="true"
          @update:checked-keys="onCheckedKeys"
        />
      </n-spin>
      <template #footer>
        <n-space justify="space-between">
          <n-space>
            <n-button size="small" @click="checkAll">全选</n-button>
            <n-button size="small" @click="checkedKeys = []">清空</n-button>
          </n-space>
          <n-space>
            <n-button @click="permVisible = false">取消</n-button>
            <n-button type="primary" :loading="saving" @click="savePermissions">保存</n-button>
          </n-space>
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
  pageRoles,
  createRole,
  updateRole,
  deleteRole,
  assignPermissions,
  getRole
} from '@/api/role'
import { permissionTree } from '@/api/permission'

const auth = useAuthStore()
const message = useMessage()
const hasPerm = (c) => auth.isSuperAdmin || auth.permissions.includes(c)

const BUILTIN_CODES = ['SUPER_ADMIN', 'ADMIN', 'USER']

const keyword = ref('')
const query = reactive({ page: 1, size: 10 })
const list = ref([])
const loading = ref(false)
const pagination = reactive({ page: 1, pageSize: 10, itemCount: 0, showSizePicker: false })

const columns = [
  { title: '角色编码', key: 'roleCode', width: 180 },
  { title: '角色名称', key: 'roleName', width: 160 },
  {
    title: '类型',
    key: 'builtin',
    width: 90,
    render: (row) =>
      BUILTIN_CODES.includes(row.roleCode)
        ? h(NTag, { size: 'small', type: 'info', round: true }, { default: () => '内置' })
        : h(NTag, { size: 'small', round: true }, { default: () => '自定义' })
  },
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
      if (hasPerm('sys:role:update')) {
        actions.push(h(NButton, { size: 'small', text: true, onClick: () => openEdit(row) }, { default: () => '编辑' }))
      }
      if (hasPerm('sys:role:assign-perm')) {
        actions.push(h(NButton, { size: 'small', text: true, type: 'info', onClick: () => openAssignPerms(row) }, { default: () => '分配权限' }))
      }
      if (hasPerm('sys:role:delete') && !BUILTIN_CODES.includes(row.roleCode)) {
        actions.push(
          h(NPopconfirm, { onPositiveClick: () => remove(row) }, {
            default: () => '确认删除该角色? 需先解除用户关联',
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
const form = reactive({ id: null, roleCode: '', roleName: '', status: 1, description: '' })
const rules = {
  roleCode: { required: true, message: '请输入角色编码', trigger: ['input', 'blur'] },
  roleName: { required: true, message: '请输入角色名称', trigger: ['input', 'blur'] }
}

function openCreate() {
  formMode.value = 'create'
  Object.assign(form, { id: null, roleCode: '', roleName: '', status: 1, description: '' })
  formVisible.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    roleCode: row.roleCode,
    roleName: row.roleName,
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
      roleCode: form.roleCode,
      roleName: form.roleName,
      status: form.status,
      description: form.description
    }
    if (formMode.value === 'create') {
      await createRole(payload)
      message.success('创建成功')
    } else {
      // 编码不可修改, 仅提交其余字段
      await updateRole(form.id, { ...payload, roleCode: form.roleCode })
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
    await deleteRole(row.id)
    message.success('删除成功')
    await load()
  } catch (e) {
    message.error(e.message)
  }
}

// 分配权限
const permVisible = ref(false)
const permLoading = ref(false)
const treeRef = ref(null)
const permTreeData = ref([])
const checkedKeys = ref([])
const currentRoleId = ref(null)

function toTreeData(nodes) {
  return (nodes || []).map((n) => ({
    key: n.id,
    label: `${n.permName} (${n.permCode})`,
    children: toTreeData(n.children)
  }))
}

function collectKeys(nodes, acc = []) {
  for (const n of nodes || []) {
    acc.push(n.key)
    if (n.children?.length) collectKeys(n.children, acc)
  }
  return acc
}

async function openAssignPerms(row) {
  currentRoleId.value = row.id
  permVisible.value = true
  permLoading.value = true
  checkedKeys.value = []
  try {
    const [tree, detail] = await Promise.all([
      permTreeData.value.length ? Promise.resolve(null) : permissionTree(),
      getRole(row.id)
    ])
    if (tree) {
      permTreeData.value = toTreeData(tree)
    }
    checkedKeys.value = detail.permissionIds || []
  } catch (e) {
    message.error(e.message)
  } finally {
    permLoading.value = false
  }
}

function onCheckedKeys(keys) {
  checkedKeys.value = keys
}

function checkAll() {
  checkedKeys.value = collectKeys(permTreeData.value)
}

async function savePermissions() {
  saving.value = true
  try {
    // 合并半选(indeterminate)的父节点, 保证上级菜单权限也入库
    const indeterminate = treeRef.value?.getIndeterminateKeys?.() || []
    const ids = Array.from(new Set([...checkedKeys.value, ...indeterminate]))
    await assignPermissions(currentRoleId.value, ids)
    message.success('分配成功')
    permVisible.value = false
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
    const data = await pageRoles(params)
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
