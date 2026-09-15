import request from '@/utils/request'

export const pageGroups = (params) => request.get('/sys/groups', { params })
export const allGroups = () => request.get('/sys/groups/all')
export const getGroup = (id) => request.get(`/sys/groups/${id}`)
export const createGroup = (data) => request.post('/sys/groups', data)
export const updateGroup = (id, data) => request.put(`/sys/groups/${id}`, data)
export const deleteGroup = (id) => request.delete(`/sys/groups/${id}`)
export const assignUsers = (id, userIds) =>
  request.put(`/sys/groups/${id}/users`, { userIds })
