import request from '@/utils/request'

export const pageTemplates = (params) => request.get('/templates', { params })
export const getTemplateDetail = (id) => request.get(`/templates/${id}`)
export const createTemplate = (data) => request.post('/templates', data)
export const updateTemplate = (id, data) => request.put(`/templates/${id}`, data)
export const deleteTemplate = (id) => request.delete(`/templates/${id}`)
export const publishTemplate = (id) => request.put(`/templates/${id}/publish`)
export const archiveTemplate = (id) => request.put(`/templates/${id}/archive`)
export const assignTemplateGroups = (id, groupIds) =>
  request.put(`/templates/${id}/groups`, { groupIds })
export const myTemplates = () => request.get('/templates/mine')

// 栏位
export const listFields = (templateId) =>
  request.get(`/templates/${templateId}/fields`)
export const getField = (templateId, fieldId) =>
  request.get(`/templates/${templateId}/fields/${fieldId}`)
export const createField = (templateId, data) =>
  request.post(`/templates/${templateId}/fields`, data)
export const updateField = (templateId, fieldId, data) =>
  request.put(`/templates/${templateId}/fields/${fieldId}`, data)
export const deactivateField = (templateId, fieldId) =>
  request.delete(`/templates/${templateId}/fields/${fieldId}`)
export const activateField = (templateId, fieldId) =>
  request.put(`/templates/${templateId}/fields/${fieldId}/activate`)
