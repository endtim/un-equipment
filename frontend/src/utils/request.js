import axios from 'axios'
import { ElMessage } from 'element-plus'
import store from '../store'
import router from '../router'

const PENDING_WINDOW_MS = 800
const pendingWriteMap = new Map()

const messageAliasMap = {
  'reservation time conflicts with existing order': '预约时间与已有订单冲突，请调整预约时段',
  'insufficient balance': '账户余额不足，请先充值',
  'instrument is not available': '当前仪器不可预约，请稍后再试',
  'request failed': '请求失败',
  'request timeout': '请求超时，请稍后重试'
}

function normalizeBizMessage(message, fallback = '服务异常，请稍后重试') {
  if (!message || typeof message !== 'string') {
    return fallback
  }
  const normalized = message.trim().toLowerCase()
  return messageAliasMap[normalized] || message
}

const service = axios.create({
  baseURL: '/',
  timeout: 10000
})

service.interceptors.request.use(config => {
  const token = store.state.token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  const method = (config.method || 'get').toUpperCase()
  const isWrite = ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)
  if (isWrite) {
    const requestKey = `${method}:${config.url}:${JSON.stringify(config.data || {})}`
    const now = Date.now()
    const last = pendingWriteMap.get(requestKey)
    if (last && now - last < PENDING_WINDOW_MS) {
      return Promise.reject(new Error('请勿重复提交'))
    }
    pendingWriteMap.set(requestKey, now)
    config.__requestKey = requestKey
  }
  return config
})

service.interceptors.response.use(
  response => {
    const requestKey = response.config && response.config.__requestKey
    if (requestKey) {
      pendingWriteMap.delete(requestKey)
    }
    const payload = response.data
    if (payload.code !== 200) {
      const msg = normalizeBizMessage(payload.msg, '请求失败')
      ElMessage.error(msg)
      return Promise.reject(new Error(msg))
    }
    return payload.data
  },
  error => {
    const requestKey = error.config && error.config.__requestKey
    if (requestKey) {
      pendingWriteMap.delete(requestKey)
    }
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      store.commit('clearAuth')
      const redirect = router.currentRoute.value && router.currentRoute.value.fullPath
      router.push({ path: '/login', query: redirect ? { redirect } : {} })
    }
    const msg = normalizeBizMessage(error.message)
    ElMessage.error(msg)
    return Promise.reject(error)
  }
)

export default service
