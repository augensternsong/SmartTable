<template>
  <div>
    <n-page-header @back="goBack">
      <template #title>{{ form?.templateName }}</template>
      <template #subtitle>
        <n-space align="center" size="small">
          <n-tag size="small" type="success" round>v{{ form?.version }}</n-tag>
          <n-text depth="3" style="font-size: 12px">
            待填 {{ form?.pendingCount }} · 已超期 {{ form?.expiredCount }}
          </n-text>
        </n-space>
      </template>
    </n-page-header>

    <n-spin :show="loading" class="mt">
      <n-card v-if="form" :bordered="false">
        <n-alert v-if="form.description" type="info" :show-icon="true" class="mb">
          {{ form.description }}
        </n-alert>

        <n-form ref="formRef" :model="formData" label-placement="top" size="medium">
          <div v-for="f in form.fields" :key="f.id" class="field-row">
            <n-form-item :label="buildLabel(f)" :path="f.id" :required="f.required === 1">
              <template #label>
                <n-space align="center" size="small" style="width: 100%">
                  <span>{{ f.fieldName }}</span>
                  <n-tag v-if="f.expired" size="tiny" type="warning" round>已超期 · 需更新</n-tag>
                  <n-tag v-else-if="f.pending" size="tiny" type="info" round>未填写</n-tag>
                  <n-button
                    v-if="!f.pending"
                    text
                    size="tiny"
                    type="primary"
                    @click="openHistory(f)"
                  >
                    历史 (上次 {{ formatTime(f.filledAt) }})
                  </n-button>
                </n-space>
              </template>

              <component
                :is="resolveComponent(f.fieldType)"
                v-model:value="formData[f.id]"
                v-bind="resolveProps(f)"
                :placeholder="f.placeholder || `请输入${f.fieldName}`"
                :status="errors[f.id] ? 'error' : undefined"
                @blur="validateField(f)"
              />
              <n-text v-if="f.description" depth="3" style="font-size: 12px; display: block">
                {{ f.description }}
              </n-text>
              <n-text v-if="errors[f.id]" type="error" style="font-size: 12px">
                {{ errors[f.id] }}
              </n-text>
            </n-form-item>
          </div>
        </n-form>

        <n-space justify="end" class="mt">
          <n-button @click="goBack">取消</n-button>
          <n-button type="primary" :loading="submitting" @click="handleSubmit">
            提交填写
          </n-button>
        </n-space>
      </n-card>
    </n-spin>

    <!-- 栏位填写历史 -->
    <n-drawer v-model:show="historyVisible" :width="480" placement="right">
      <n-drawer-content :title="`填写历史 - ${currentField?.fieldName || ''}`" closable>
        <n-spin :show="historyLoading">
          <n-empty v-if="!history.length" description="暂无历史记录" />
          <n-timeline v-else>
            <n-timeline-item
              v-for="h in history"
              :key="h.id"
              :type="'default'"
              :time="formatTime(h.filledAt)"
            >
              <template #header>
                <n-text depth="2">{{ h.filledByNickname || h.filledByUsername || '用户' }}</n-text>
              </template>
              <div class="history-value">{{ displayValue(h.fieldValue, currentField) }}</div>
            </n-timeline-item>
          </n-timeline>
        </n-spin>
      </n-drawer-content>
    </n-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useMessage } from 'naive-ui'
import dayjs from 'dayjs'
import {
  NInput,
  NInputNumber,
  NDatePicker,
  NSelect,
  NDynamicTags
} from 'naive-ui'
import { getFormView, submitForm, getFieldHistory } from '@/api/form'

const route = useRoute()
const router = useRouter()
const message = useMessage()

const form = ref(null)
const formData = reactive({})
const errors = reactive({})
const loading = ref(false)
const submitting = ref(false)

// 历史
const historyVisible = ref(false)
const historyLoading = ref(false)
const history = ref([])
const currentField = ref(null)

function formatTime(t) {
  return t ? dayjs(t).format('YYYY-MM-DD HH:mm:ss') : '-'
}

function buildLabel(f) {
  return f.fieldName
}

function resolveComponent(type) {
  switch (type) {
    case 'TEXT':
    case 'TEXTAREA':
      return NInput
    case 'NUMBER':
      return NInputNumber
    case 'DATE':
    case 'DATETIME':
      return NDatePicker
    case 'SELECT_SINGLE':
    case 'SELECT_MULTI':
      return NSelect
    default:
      return NInput
  }

}

function resolveProps(f) {
  const type = f.fieldType
  const base = {}
  if (type === 'TEXT') {
    base.type = 'text'
    base.maxlength = f.maxLength || undefined
    base.showCount = !!f.maxLength
  } else if (type === 'TEXTAREA') {
    base.type = 'textarea'
    base.maxlength = f.maxLength || undefined
    base.showCount = !!f.maxLength
    base.autosize = { minRows: 2, maxRows: 6 }
  } else if (type === 'NUMBER') {
    base.showButton = true
    if (f.minValue != null) base.min = Number(f.minValue)
    if (f.maxValue != null) base.max = Number(f.maxValue)
  } else if (type === 'DATE') {
    base.type = 'date'
    base.format = 'yyyy-MM-dd'
    base.valueFormat = 'yyyy-MM-dd'
  } else if (type === 'DATETIME') {
    base.type = 'datetime'
    base.format = 'yyyy-MM-dd HH:mm:ss'
    base.valueFormat = 'yyyy-MM-dd HH:mm:ss'
  } else if (type === 'SELECT_SINGLE' || type === 'SELECT_MULTI') {
    base.options = (f.options || []).map((o) => ({ label: o.optionLabel, value: o.optionValue }))
    base.multiple = type === 'SELECT_MULTI'
    base.clearable = true
    base.filterable = true
    base.tag = false
    // 值类型: 单选 string; 多选 string[] -> 提交时序列化为 JSON 字符串
    if (type === 'SELECT_MULTI') {
      base.maxTagCount = 5
    }
  }
  return base
}

function displayValue(v, f) {
  if (v == null || v === '') return '（空）'
  if (f?.fieldType === 'SELECT_MULTI') {
    try {
      const arr = JSON.parse(v)
      const optMap = {}
      ;(f.options || []).forEach((o) => { optMap[o.optionValue] = o.optionLabel })
      return arr.map((x) => optMap[x] || x).join(', ')
    } catch (_) {
      return v
    }
  }
  if (f?.fieldType === 'SELECT_SINGLE') {
    const opt = (f.options || []).find((o) => o.optionValue === v)
    return opt ? opt.optionLabel : v
  }
  return v
}

function validateField(f) {
  const v = formData[f.id]
  if (f.required === 1 && (v === undefined || v === null || v === '' || (Array.isArray(v) && v.length === 0))) {
    errors[f.id] = `${f.fieldName} 为必填项`
    return false
  }
  if (v != null && v !== '' && f.fieldType === 'TEXT' && f.maxLength && String(v).length > f.maxLength) {
    errors[f.id] = `${f.fieldName} 长度不能超过 ${f.maxLength}`
    return false
  }
  if (v != null && v !== '' && f.fieldType === 'NUMBER') {
    if (f.minValue != null && Number(v) < Number(f.minValue)) {
      errors[f.id] = `${f.fieldName} 不能小于 ${f.minValue}`
      return false
    }
    if (f.maxValue != null && Number(v) > Number(f.maxValue)) {
      errors[f.id] = `${f.fieldName} 不能大于 ${f.maxValue}`
      return false
    }
  }
  errors[f.id] = ''
  return true
}

function validateAll() {
  let ok = true
  for (const f of form.value.fields || []) {
    if (!validateField(f)) ok = false
  }
  return ok
}

async function loadForm() {
  loading.value = true
  try {
    const data = await getFormView(route.params.templateId)
    form.value = data
    // 初始化表单值(根据类型转换为前端友好类型)
    for (const f of data.fields || []) {
      const cur = f.currentValue
      if (f.fieldType === 'SELECT_MULTI') {
        try {
          formData[f.id] = cur ? JSON.parse(cur) : []
        } catch (_) {
          formData[f.id] = []
        }
      } else if (f.fieldType === 'DATE' || f.fieldType === 'DATETIME') {
        // NDatePicker 期望时间戳(数字) 或 Date; 字符串需转
        formData[f.id] = cur ? new Date(cur).getTime() : null
      } else if (f.fieldType === 'NUMBER') {
        formData[f.id] = cur != null && cur !== '' ? Number(cur) : null
      } else {
        formData[f.id] = cur != null ? cur : ''
      }
    }
  } catch (e) {
    message.error(e.message)
    router.back()
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!validateAll()) {
    message.warning('请检查填写项')
    return
  }
  submitting.value = true
  try {
    const items = (form.value.fields || []).map((f) => {
      let value = formData[f.id]
      if (value == null) value = ''
      if (f.fieldType === 'SELECT_MULTI') {
        const arr = Array.isArray(value) ? value : []
        value = JSON.stringify(arr)
      } else if (f.fieldType === 'DATE' || f.fieldType === 'DATETIME') {
        if (value === '' || value == null) value = ''
        else value = dayjs(Number(value)).format(f.fieldType === 'DATE' ? 'YYYY-MM-DD' : 'YYYY-MM-DD HH:mm:ss')
      } else if (f.fieldType === 'NUMBER') {
        value = value === '' || value == null ? '' : String(value)
      } else {
        value = String(value)
      }
      return { fieldId: f.id, value }
    })
    await submitForm(route.params.templateId, items)
    message.success('提交成功')
    await loadForm()
  } catch (e) {
    message.error(e.message)
  } finally {
    submitting.value = false
  }
}

async function openHistory(f) {
  currentField.value = f
  historyVisible.value = true
  history.value = []
  historyLoading.value = true
  try {
    history.value = (await getFieldHistory(f.id)) || []
  } catch (e) {
    message.error(e.message)
  } finally {
    historyLoading.value = false
  }
}

function goBack() {
  router.back()
}

onMounted(loadForm)
</script>

<style scoped>
.mt {
  margin-top: 12px;
}
.mb {
  margin-bottom: 16px;
}
.field-row {
  margin-bottom: 12px;
}
.history-value {
  font-size: 14px;
  padding: 4px 8px;
  background: #f5f7fa;
  border-radius: 4px;
  margin-top: 4px;
  word-break: break-all;
}
</style>
