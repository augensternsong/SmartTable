<template>
  <div>
    <n-page-header @back="goBack">
      <template #title>
        <n-space align="center" size="small">
          <span>栏位管理</span>
          <n-tag v-if="tpl" size="small" :type="statusType(tpl.status)" round>
            {{ statusText(tpl.status) }}
          </n-tag>
        </n-space>
      </template>
      <template #subtitle>
        <span v-if="tpl">
          {{ tpl.templateName }} ({{ tpl.templateCode }}) · v{{ tpl.version }} · 栏位 {{ list.length }} 个
        </span>
      </template>
      <template #extra>
        <n-space>
          <n-button size="small" @click="load">刷新</n-button>
          <n-button
            v-if="hasPerm('form:field:create') && tpl && tpl.status !== 'ARCHIVED'"
            size="small"
            type="primary"
            @click="openCreate"
          >
            新建栏位
          </n-button>
        </n-space>
      </template>
    </n-page-header>

    <n-card class="mt" :bordered="false">
      <n-alert v-if="tpl && tpl.status === 'ARCHIVED'" type="warning" class="mb" :show-icon="true">
        模板已归档, 栏位不可修改. 如需调整请重新启用模板或新建模板版本.
      </n-alert>

      <n-data-table
        :columns="columns"
        :data="list"
        :loading="loading"
        :row-key="(r) => r.id"
        :pagination="false"
      />
    </n-card>

    <!-- 栏位编辑/新建 -->
    <n-modal
      v-model:show="formVisible"
      preset="card"
      :title="formMode === 'create' ? '新建栏位' : '编辑栏位'"
      style="width: 720px"
      :mask-closable="false"
    >
      <n-form ref="formRef" :model="form" :rules="rules" label-placement="left" label-width="110">
        <n-grid :cols="2" :x-gap="16">
          <n-form-item-gi label="栏位编码" path="fieldCode">
            <n-input
              v-model:value="form.fieldCode"
              :disabled="formMode === 'edit'"
              placeholder="字母开头, 字母数字下划线, 2-64位"
            />
          </n-form-item-gi>
          <n-form-item-gi label="栏位名称" path="fieldName">
            <n-input v-model:value="form.fieldName" placeholder="显示给用户看的名称" />
          </n-form-item-gi>
          <n-form-item-gi label="栏位类型" path="fieldType">
            <n-select
              v-model:value="form.fieldType"
              :options="fieldTypeOptions"
              :disabled="formMode === 'edit'"
              placeholder="选择类型"
            />
          </n-form-item-gi>
          <n-form-item-gi label="排序" path="sortOrder">
            <n-input-number v-model:value="form.sortOrder" :min="0" :max="9999" style="width: 100%" />
          </n-form-item-gi>
          <n-form-item-gi label="是否必填" path="required">
            <n-switch v-model:value="requiredBool" />
          </n-form-item-gi>
          <n-form-item-gi label="填写周期(天)" path="fillCycleDays">
            <n-input-number
              v-model:value="form.fillCycleDays"
              :min="1"
              :max="3650"
              placeholder="超过该天数视为超期"
              style="width: 100%"
            />
          </n-form-item-gi>

          <template v-if="form.fieldType === 'TEXT' || form.fieldType === 'TEXTAREA'">
            <n-form-item-gi label="最大长度" path="maxLength">
              <n-input-number v-model:value="form.maxLength" :min="1" :max="10000" style="width: 100%" />
            </n-form-item-gi>
            <n-form-item-gi label="正则校验" path="regexPattern">
              <n-input v-model:value="form.regexPattern" placeholder="可选, 例如 ^[A-Z]+$" />
            </n-form-item-gi>
          </template>

          <template v-if="form.fieldType === 'NUMBER'">
            <n-form-item-gi label="最小值" path="minValue">
              <n-input-number v-model:value="form.minValue" style="width: 100%" />
            </n-form-item-gi>
            <n-form-item-gi label="最大值" path="maxValue">
              <n-input-number v-model:value="form.maxValue" style="width: 100%" />
            </n-form-item-gi>
          </template>

          <n-form-item-gi :span="2" label="占位提示" path="placeholder">
            <n-input v-model:value="form.placeholder" :maxlength="255" placeholder="输入框占位文字" />
          </n-form-item-gi>
          <n-form-item-gi :span="2" label="栏位描述" path="description">
            <n-input v-model:value="form.description" type="textarea" :maxlength="512" :autosize="{ minRows: 2, maxRows: 4 }" />
          </n-form-item-gi>
        </n-grid>

        <!-- 选择类: 选项编辑 -->
        <template v-if="isSelectType(form.fieldType)">
          <n-divider>选项配置</n-divider>
          <n-space class="mb" justify="space-between" align="center">
            <n-text depth="3" style="font-size: 13px">
              {{ form.fieldType === 'SELECT_MULTI' ? '多选' : '单选' }} · 至少 1 个选项
            </n-text>
            <n-button size="small" type="primary" @click="addOption">添加选项</n-button>
          </n-space>
          <n-data-table
            :columns="optionColumns"
            :data="form.options"
            :pagination="false"
            size="small"
            :row-key="(r, i) => i"
            :max-height="280"
          />
        </template>
      </n-form>

      <template #footer>
        <n-space justify="end">
          <n-button @click="formVisible = false">取消</n-button>
          <n-button type="primary" :loading="saving" @click="save">保存</n-button>
        </n-space>
      </template>
    </n-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, h } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage, NButton, NSpace, NTag, NPopconfirm, NInput } from 'naive-ui'
import dayjs from 'dayjs'
import { useAuthStore } from '@/stores/auth'
import { getTemplateDetail } from '@/api/template'
import {
  listFields,
  createField,
  updateField,
  deactivateField,
  activateField
} from '@/api/template'

const route = useRoute()
const router = useRouter()
const message = useMessage()
const auth = useAuthStore()

const templateId = computed(() => String(route.params.id))
const hasPerm = (c) => auth.isSuperAdmin || auth.permissions.includes(c)

const tpl = ref(null)
const list = ref([])
const loading = ref(false)

const fieldTypeOptions = [
  { label: '短文本', value: 'TEXT' },
  { label: '长文本', value: 'TEXTAREA' },
  { label: '数字', value: 'NUMBER' },
  { label: '日期', value: 'DATE' },
  { label: '日期时间', value: 'DATETIME' },
  { label: '单选下拉', value: 'SELECT_SINGLE' },
  { label: '多选下拉', value: 'SELECT_MULTI' }
]

function isSelectType(t) {
  return t === 'SELECT_SINGLE' || t === 'SELECT_MULTI'
}

function statusText(s) {
  return { DRAFT: '草稿', PUBLISHED: '已发布', ARCHIVED: '已归档' }[s] || s
}
function statusType(s) {
  return { DRAFT: 'default', PUBLISHED: 'success', ARCHIVED: 'warning' }[s] || 'default'
}
function typeText(t) {
  return fieldTypeOptions.find((o) => o.value === t)?.label || t
}
function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm') : '-'
}

const columns = [
  { title: '排序', key: 'sortOrder', width: 70 },
  { title: '栏位编码', key: 'fieldCode', width: 160 },
  { title: '栏位名称', key: 'fieldName' },
  {
    title: '类型',
    key: 'fieldType',
    width: 120,
    render: (r) => h(NTag, { size: 'small', round: true, type: 'info' }, { default: () => typeText(r.fieldType) })
  },
  {
    title: '必填',
    key: 'required',
    width: 70,
    align: 'center',
    render: (r) => (r.required === 1 ? '是' : '否')
  },
  {
    title: '周期(天)',
    key: 'fillCycleDays',
    width: 100,
    render: (r) => r.fillCycleDays ?? '-'
  },
  {
    title: '状态',
    key: 'status',
    width: 90,
    render: (r) =>
      h(
        NTag,
        { size: 'small', round: true, type: r.status === 'ACTIVE' ? 'success' : 'default' },
        { default: () => (r.status === 'ACTIVE' ? '启用' : '停用') }
      )
  },
  {
    title: '选项',
    key: 'options',
    width: 70,
    align: 'center',
    render: (r) => (isSelectType(r.fieldType) ? (r.options ? r.options.length : 0) : '-')
  },
  {
    title: '更新时间',
    key: 'updatedAt',
    width: 150,
    render: (r) => formatTime(r.updatedAt)
  },
  {
    title: '操作',
    key: 'actions',
    width: 200,
    fixed: 'right',
    render: (row) => {
      const actions = []
      if (tpl.value && tpl.value.status !== 'ARCHIVED') {
        if (hasPerm('form:field:update')) {
          actions.push(h(NButton, { size: 'small', text: true, type: 'primary', onClick: () => openEdit(row) }, { default: () => '编辑' }))
        }
        if (row.status === 'ACTIVE') {
          if (hasPerm('form:field:delete')) {
            actions.push(
              h(NPopconfirm, { onPositiveClick: () => deactivate(row) }, {
                default: () => '确认停用该栏位? 停用后用户端不再显示.',
                trigger: () => h(NButton, { size: 'small', text: true, type: 'warning' }, { default: () => '停用' })
              })
            )
          }
        } else {
          if (hasPerm('form:field:update')) {
            actions.push(h(NButton, { size: 'small', text: true, type: 'success', onClick: () => activate(row) }, { default: () => '启用' }))
          }
        }
      }
      if (!actions.length) return '-'
      return h(NSpace, { size: 'small' }, { default: () => actions })
    }
  }
]

// 选项编辑列(列内可编辑)
const optionColumns = [
  {
    title: '排序',
    key: 'sortOrder',
    width: 80,
    render: (row, i) =>
      h(NInput, {
        value: String(row.sortOrder ?? i + 1),
        size: 'small',
        onUpdateValue: (v) => { row.sortOrder = Number(v) || 0 }
      })
  },
  {
    title: '选项值',
    key: 'optionValue',
    render: (row) =>
      h(NInput, {
        value: row.optionValue,
        size: 'small',
        placeholder: '提交时存储的值',
        onUpdateValue: (v) => { row.optionValue = v }
      })
  },
  {
    title: '选项标签',
    key: 'optionLabel',
    render: (row) =>
      h(NInput, {
        value: row.optionLabel,
        size: 'small',
        placeholder: '用户看到的文字',
        onUpdateValue: (v) => { row.optionLabel = v }
      })
  },
  {
    title: '操作',
    key: 'op',
    width: 80,
    align: 'center',
    render: (_, idx) =>
      h(
        NButton,
        { size: 'small', text: true, type: 'error', onClick: () => removeOption(idx) },
        { default: () => '删除' }
      )
  }
]

// 表单
const formVisible = ref(false)
const formMode = ref('create')
const saving = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  fieldCode: '',
  fieldName: '',
  fieldType: 'TEXT',
  sortOrder: 1,
  required: 0,
  maxLength: null,
  minValue: null,
  maxValue: null,
  regexPattern: '',
  fillCycleDays: 30,
  placeholder: '',
  description: '',
  options: []
})

const requiredBool = computed({
  get: () => form.required === 1,
  set: (v) => { form.required = v ? 1 : 0 }
})

const rules = {
  fieldCode: { required: true, message: '请输入栏位编码', trigger: ['input', 'blur'] },
  fieldName: { required: true, message: '请输入栏位名称', trigger: ['input', 'blur'] },
  fieldType: { required: true, message: '请选择栏位类型', trigger: ['change', 'blur'] },
  fillCycleDays: { required: true, type: 'number', message: '请输入填写周期', trigger: ['input', 'blur'] }
}

function defaultForm() {
  Object.assign(form, {
    id: null,
    fieldCode: '',
    fieldName: '',
    fieldType: 'TEXT',
    sortOrder: (list.value.length || 0) + 1,
    required: 0,
    maxLength: null,
    minValue: null,
    maxValue: null,
    regexPattern: '',
    fillCycleDays: 30,
    placeholder: '',
    description: '',
    options: []
  })
}

function openCreate() {
  formMode.value = 'create'
  defaultForm()
  formVisible.value = true
}

function openEdit(row) {
  formMode.value = 'edit'
  Object.assign(form, {
    id: row.id,
    fieldCode: row.fieldCode,
    fieldName: row.fieldName,
    fieldType: row.fieldType,
    sortOrder: row.sortOrder ?? 1,
    required: row.required ?? 0,
    maxLength: row.maxLength ?? null,
    minValue: row.minValue ?? null,
    maxValue: row.maxValue ?? null,
    regexPattern: row.regexPattern || '',
    fillCycleDays: row.fillCycleDays ?? 30,
    placeholder: row.placeholder || '',
    description: row.description || '',
    options: (row.options || []).map((o) => ({ ...o }))
  })
  formVisible.value = true
}

function addOption() {
  form.options.push({
    id: null,
    optionValue: '',
    optionLabel: '',
    sortOrder: form.options.length + 1,
    status: 1
  })
}

function removeOption(idx) {
  form.options.splice(idx, 1)
}

async function save() {
  try {
    await formRef.value?.validate()
  } catch (_) {
    return
  }
  if (isSelectType(form.fieldType)) {
    if (!form.options.length) {
      message.warning('请至少添加一个选项')
      return
    }
    for (const o of form.options) {
      if (!o.optionValue || !o.optionLabel) {
        message.warning('选项值/标签不能为空')
        return
      }
    }
  }

  saving.value = true
  try {
    const payload = {
      fieldCode: form.fieldCode,
      fieldName: form.fieldName,
      fieldType: form.fieldType,
      sortOrder: form.sortOrder,
      required: form.required,
      maxLength: ['TEXT', 'TEXTAREA'].includes(form.fieldType) ? form.maxLength : null,
      minValue: form.fieldType === 'NUMBER' ? form.minValue : null,
      maxValue: form.fieldType === 'NUMBER' ? form.maxValue : null,
      regexPattern: ['TEXT', 'TEXTAREA'].includes(form.fieldType) ? form.regexPattern || null : null,
      fillCycleDays: form.fillCycleDays,
      placeholder: form.placeholder || null,
      description: form.description || null,
      options: isSelectType(form.fieldType) ? form.options : []
    }
    if (formMode.value === 'create') {
      await createField(templateId.value, payload)
      message.success('创建成功')
    } else {
      await updateField(templateId.value, form.id, payload)
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

async function deactivate(row) {
  try {
    await deactivateField(templateId.value, row.id)
    message.success('已停用')
    await load()
  } catch (e) {
    message.error(e.message)
  }
}

async function activate(row) {
  try {
    await activateField(templateId.value, row.id)
    message.success('已启用')
    await load()
  } catch (e) {
    message.error(e.message)
  }
}

function goBack() {
  router.back()
}

async function load() {
  loading.value = true
  try {
    const detail = await getTemplateDetail(templateId.value)
    tpl.value = detail
    // 栏位按 sortOrder 排序
    list.value = (detail.fields || []).slice().sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0))
  } catch (e) {
    message.error(e.message)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.mt {
  margin-top: 12px;
}
.mb {
  margin-bottom: 12px;
}
</style>
