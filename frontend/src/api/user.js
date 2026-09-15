import request from '@/utils/request'

export const pageUsers = (params) => request.get('/sys/users', { params })
export const getUser = (id) => request.get(`/sys/users/${id}`)
export const createUser = (data) => request.post('/sys/users', data)
export const updateUser = (id, data) => request.put(`/sys/users/${id}`, data)
export const deleteUser = (id) => request.delete(`/sys/users/${id}`)
export const assignRoles = (id, roleIds) =>
  request.put(`/sys/users/${id}/roles`, { roleIds })
export const assignGroups = (id, groupIds) =>
  request.put(`/sys/users/${id}/groups`, { groupIds })
export const resetPassword = (id, newPassword) =>
  request.put(`/sys/users/${id}/password`, { newPassword })
export const changeMyPassword = (data) =>
  request.put('/sys/users/me/password', data)
export const getEditableRoles = () => request.get('/sys/users/editable-roles')
