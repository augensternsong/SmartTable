import { defineStore } from 'pinia'
import request from '@/utils/request'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    accessToken: localStorage.getItem('access_token') || '',
    refreshToken: localStorage.getItem('refresh_token') || '',
    userInfo: JSON.parse(localStorage.getItem('user_info') || 'null')
  }),

  getters: {
    isLoggedIn: (state) => !!state.accessToken,
    permissions: (state) => state.userInfo?.permissions || [],
    roles: (state) => state.userInfo?.roles || [],
    menus: (state) => state.userInfo?.menus || [],
    isSuperAdmin: (state) => (state.userInfo?.roles || []).includes('SUPER_ADMIN')
  },

  actions: {
    /**
     * 登录, 保存 token + 用户信息
     */
    async login(payload) {
      const data = await request.post('/auth/login', payload)
      this._persist(data)
      return data
    },

    /**
     * 刷新 token. 用 refreshToken 换新 accessToken
     */
    async refresh() {
      try {
        const data = await request.post('/auth/refresh', { refreshToken: this.refreshToken })
        this._persist(data)
        return data.accessToken
      } catch (e) {
        this.clear()
        throw e
      }
    },

    /**
     * 拉取最新的用户信息(菜单/权限)
     */
    async fetchMe() {
      const info = await request.get('/auth/me')
      this.userInfo = info
      localStorage.setItem('user_info', JSON.stringify(info))
      return info
    },

    async logout() {
      try {
        await request.post('/auth/logout')
      } catch (_) {
        // 忽略登出错误
      }
      this.clear()
    },

    clear() {
      this.accessToken = ''
      this.refreshToken = ''
      this.userInfo = null
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      localStorage.removeItem('user_info')
    },

    hasPermission(code) {
      if (this.isSuperAdmin) return true
      return this.permissions.includes(code)
    },

    _persist(data) {
      if (data.accessToken) {
        this.accessToken = data.accessToken
        localStorage.setItem('access_token', data.accessToken)
      }
      if (data.refreshToken) {
        this.refreshToken = data.refreshToken
        localStorage.setItem('refresh_token', data.refreshToken)
      }
      if (data.userInfo) {
        this.userInfo = data.userInfo
        localStorage.setItem('user_info', JSON.stringify(data.userInfo))
      }
    }
  }
})
