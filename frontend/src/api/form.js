import request from '@/utils/request'

// 加载填写视图(含栏位 + 选项 + 当前值 + 超期标记)
export const getFormView = (templateId) =>
  request.get(`/forms/templates/${templateId}`)

// 提交填写
export const submitForm = (templateId, items) =>
  request.post(`/forms/templates/${templateId}/submit`, { items })

// 某栏位填写历史
export const getFieldHistory = (fieldId) =>
  request.get(`/forms/fields/${fieldId}/history`)

// 我的待办提醒
export const myReminders = () => request.get('/forms/reminders')
