import request from '@/utils/request'

export const pageRoles = (params) => request.get('/sys/roles', { params })
export const allRoles = () => request.get('/sys/roles/all')
export const getRole = (id) => request.get(`/sys/roles/${id}`)
export const createRole = (data) => request.post('/sys/roles', data)
export const updateRole = (id, data) => request.put(`/sys/roles/${id}`, data)
export const deleteRole = (id) => request.delete(`/sys/roles/${id}`)
export const assignPermissions = (id, permissionIds) =>
  request.put(`/sys/roles/${id}/permissions`, { permissionIds })
