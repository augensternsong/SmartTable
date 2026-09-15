import request from '@/utils/request'

export const permissionTree = () => request.get('/sys/permissions/tree')
