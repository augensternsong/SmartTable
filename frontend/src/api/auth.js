import request from '@/utils/request'

export const login = (data) => request.post('/auth/login', data)
export const refresh = (data) => request.post('/auth/refresh', data)
export const logout = () => request.post('/auth/logout')
export const me = () => request.get('/auth/me')
