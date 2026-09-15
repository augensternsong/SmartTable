<template>
  <n-card :bordered="false">
    <n-space align="center" class="mb" justify="space-between">
      <n-space align="center">
        <n-input v-model:value="query.keyword" placeholder="模板名称/编码" clearable style="width: 220px" @keyup.enter="load" />
        <n-select v-model:value="query.status" :options="statusOptions" placeholder="状态" clearable style="width: 140px" />
        <n-button type="primary" @click="load">查询</n-button>
        <n-button @click="resetQuery">重置</n-button>
      </n-space>
      <n-button v-if="hasPerm('form:template:create')" type="primary" @click="openCreate">新建模板</n-button>
    </n-space>

    <n-data-table
      :columns="columns"
      :data="list"
      :loading="loading"
      :pagination="pagination"
      remote
      @update:page="handlePage"
    />

    <!-- 编辑/新建 -->
    <n-modal v-model:show="formVisible" preset="card" :title="formMode === 'create' ? '新建模板' : '编辑模板'" style="width: 520px">
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="90">
        <n-form-item label="模板编码" path="templateCode">
          <n-input v-model:value="form.templateCode" :disabled="formMode === 'edit'" placeholder="字母开头, 字母数字下划线连字符" />
        </n-form-item>
        <n-form-item label="模板名称" path="templateName">
          <n-input v-model:value="form.templateName" placeholder="模板名称" />
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

    <!-- 分配分组 -->
    <n-modal v-model:show="groupVisible" preset="card" title="分配用户分组" style="width: 480px">
      <n-checkbox-group v-model:value="selectedGroupIds">
        <n-space vertical>
          <n-checkbox v-for="g in allGroups" :key="g.id" :value="g.id" :label="`${g.groupName} (${g.groupCode})`" />
          <n-empty v-if="!allGroups.length" description="尚无分组, 请先创建" size="small" />
        </n-space>
      </n-checkbox-group>
      <template #footer>
        <n-space justify="end">
          <n-button @click="groupVisible = false">取消</n-button>
          <n-button type="primary" @click="saveGroups">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </n-card>
</template>

<script setup>
import { ref, reactive, onMounted, h } from 'vue'
import { useRouter } from 'vue-router'
import { useMessage, useDialog, NButton, NSpace, NTag, NPopconfirm } from 'naive-ui'
import { useAuthStore } from '@/stores/auth'
import {
  pageTemplates,
  createTemplate,
  updateTemplate,
  deleteTemplate,
  publishTemplate,
  archiveTemplate,
  assignTemplateGroups,
  getTemplateDetail
} from '@/api/template'
import { allGroups } from '@/api/group'

const auth = useAuthStore()
const message = useMessage()
const dialog = useDialog()
const router = useRouter()

const hasPerm = (c) => auth.isSuperAdmin || auth.permissions.includes(c)

const statusOptions = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已归档', value: 'ARCHIVED' }
]

const query = reactive({ page: 1, size: 10, keyword: '', status: null })
const list = ref([])
const loading = ref(false)
const pagination = reactive({
  page: 1,
  pageSize: 10,
  itemCount: 0,
  showSizePicker: false
})

const columns = [
  { title: '模板编码', key: 'templateCode', width: 140 },
  { title: '模板名称', key: 'templateName' },
  { title: '状态', key: 'status', width: 100, render: (row) => statusTag(row.status) },
  { title: '版本', key: 'version', width: 70 },
  { title: '栏位数', key: 'fieldCount', width: 80 },
  { title: '分配分组', key: 'assignedGroupCount', width: 90 },
  { title: '更新时间', key: 'updatedAt', width: 160, render: (r) => formatTime(r.updatedAt) },
  {
    title: '操作',
    key: 'actions',
    width: 380,
    fixed: 'right',
    render: (row) => {
      const actions = [
        h(NButton, { size: 'small', text: true, type: 'primary', onClick: () => goField(row) }, { default: () => '栏位管理' }),
        h(NButton, { size: 'small', text: true, type: 'info', onClick: () => openAssignGroups(row) }, { default: () => '分配分组' })
      ]
      if (hasPerm('form:template:update')) {
        actions.push(h(NButton, { size: 'small', text: true, onClick: () => openEdit(row) }, { default: () => '编辑' }))
      }
      if (hasPerm('form:template:publish')) {
        if (row.status !== 'PUBLISHED') {
          actions.push(h(NButton, { size: 'small', text: true, type: 'success', onClick: () => publish(row) }, { default: () => '发布' }))
        } else {
          actions.push(h(NButton, { size: 'small', text: true, type: 'warning', onClick: () => archive(row) }, { default: () => '归档' }))
        }
      }
      if (hasPerm('form:template:delete') && row.status === 'DRAFT') {
        actions.push(
          h(NPopconfirm, { onPositiveClick: () => remove(row) }, {
            default: () => '确认删除该模板?',
            trigger: () => h(NButton, { size: 'small', text: true, type: 'error' }, { default: () => '删除' })
          })
        )
      }
      return h(NSpace, { size: 'small' }, { default: () => actions })
    }
  }
]

function statusTag(s) {
  const map = { DRAFT: { text: '草稿', type: 'default' }, PUBLISHED: { text: '已发布', type: 'success' }, ARCHIVED: { text: '已归档', type: 'warning' } }
  const m = map[s] || { text: s, type: 'default' }
  return h(NTag, { size: 'small', type: m.type, round: true }, { default: () => m.text })
}

function formatTime(t) {
  return t ? new Date(t).toLocaleString('zh-CN', { hour12: false }) : '-'
}

// 表单
const formVisible = ref(false)
const formMode = ref('create')
const saving = ref(false)
const formRef = ref(null)
const form = reactive({ id: null, templateCode: '', templateName: '', description: '' })
const rules = {
  templateCode: { required: true, message: '请输入模板编码', trigger: ['input', 'blur'] },
  templateName: { required: true, message: '请输入模板名称', trigger: ['input', 'blur'] }
}

function openCreate() {
  formMode.value = 'create'
  Object.assign(form, { id: null, templateCode: '', templateName: '', description: '' })
  formVisible.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  Object.assign(form, { id: row.id, templateCode: row.templateCode, templateName: row.templateName, description: row.description || '' })
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
    if (formMode.value === 'create') {
      await createTemplate({ templateCode: form.templateCode, templateName: form.templateName, description: form.description })
      message.success('创建成功')
    } else {
      await updateTemplate(form.id, { templateName: form.templateName, description: form.description })
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
    await deleteTemplate(row.id)
    message.success('删除成功')
    await load()
  } catch (e) {
    message.error(e.message)
  }
}

async function publish(row) {
  try {
    await publishTemplate(row.id)
    message.success('发布成功')
    await load()
  } catch (e) {
    message.error(e.message)
  }
}

async function archive(row) {
  try {
    await archiveTemplate(row.id)
    message.success('归档成功')
    await load()
  } catch (e) {
    message.error(e.message)
  }
}

function goField(row) {
  router.push({ name: 'TemplateField', params: { id: row.id } })
}

// 分配分组
const groupVisible = ref(false)
const groups = ref([])
const selectedGroupIds = ref([])

async function openAssignGroups(row) {
  try {
    const [detail, all] = await Promise.all([getTemplateDetail(row.id), allGroups()])
    groups.value = all || []
    selectedGroupIds.value = detail.assignedGroupIds || []
    groupVisible.value = true
    currentTemplateId.value = row.id
  } catch (e) {
    message.error(e.message)
  }
}

const currentTemplateId = ref(null)
async function saveGroups() {
  try {
    await assignTemplateGroups(currentTemplateId.value, selectedGroupIds.value)
    message.success('分配成功')
    groupVisible.value = false
    await load()
  } catch (e) {
    message.error(e.message)
  }
}

function resetQuery() {
  query.keyword = ''
  query.status = null
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
    const data = await pageTemplates(query)
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
