import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const service = axios.create({
  baseURL: '/api',
  timeout: 30000
})

// 请求拦截: 自动附加 access token
service.interceptors.request.use(
  (config) => {
    const auth = useAuthStore()
    if (auth.accessToken) {
      config.headers.Authorization = `Bearer ${auth.accessToken}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 是否正在刷新 token
let isRefreshing = false
let pendingQueue = []

function processQueue(error, token = null) {
  pendingQueue.forEach((cb) => cb(error, token))
  pendingQueue = []
}

// 响应拦截: 统一处理 code/401 自动刷新
service.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res && typeof res === 'object' && 'code' in res) {
      // 业务码 0 成功; 其他失败
      if (res.code === 0) {
        return res.data === undefined ? null : res.data
      }
      // 401: 未登录/过期 -> 尝试刷新
      if (res.code === 401) {
        return handleUnauthorized(response)
      }
      // 403: 无权限, 直接提示
      if (res.code === 403) {
        return Promise.reject(new Error(res.msg || '没有访问权限'))
      }
      // 其他业务错误
      return Promise.reject(new Error(res.msg || '请求失败'))
    }
    // 非标准响应(如二进制)直接返回
    return response.data
  },
  (error) => {
    // HTTP 401 由响应体处理; 这里处理网络错误
    if (error.response && error.response.status === 401) {
      return handleUnauthorized(error.response)
    }
    if (error.response && error.response.status === 403) {
      return Promise.reject(new Error('没有访问权限'))
    }
    const msg = error.response?.data?.msg || error.message || '网络异常'
    return Promise.reject(new Error(msg))
  }
)

function handleUnauthorized(response) {
  const auth = useAuthStore()
  // 没有 refresh token, 直接跳登录
  if (!auth.refreshToken) {
    auth.clear()
    router.replace({ name: 'login' })
    return Promise.reject(new Error('未登录或登录已过期'))
  }
  // 已在刷新中, 排队等待
  if (isRefreshing) {
    return new Promise((resolve, reject) => {
      pendingQueue.push((error, token) => {
        if (error) {
          reject(error)
        } else {
          response.config.headers.Authorization = `Bearer ${token}`
          resolve(service(response.config))
        }
      })
    })
  }
  // 启动刷新
  isRefreshing = true
  return auth
    .refresh()
    .then((token) => {
      processQueue(null, token)
      response.config.headers.Authorization = `Bearer ${token}`
      return service(response.config)
    })
    .catch((err) => {
      processQueue(err, null)
      auth.clear()
      router.replace({ name: 'login' })
      return Promise.reject(err || new Error('登录已过期, 请重新登录'))
    })
    .finally(() => {
      isRefreshing = false
    })
}

export default service
