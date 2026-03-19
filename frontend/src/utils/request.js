import axios from 'axios'
import { ElMessage } from 'element-plus'
import store from '../store'
import router from '../router'

const PENDING_WINDOW_MS = 800
const pendingWriteMap = new Map()

// 业务错误码统一中文映射：前端以 code 为准，避免耦合后端 msg 文案。
const codeAliasMap = {
  40000: '操作失败，请稍后重试',
  40001: '请求参数不合法，请检查后重试',
  401: '未登录或登录已失效，请重新登录',
  40300: '当前账号无权限执行该操作',
  40400: '请求资源不存在或已删除',
  41001: '用户名或密码错误',
  41002: '用户名已存在',
  42001: '预约时间范围不合法，请检查后重试',
  42002: '预约时间与已有订单冲突，请调整后重试',
  42003: '订单操作不合法，请刷新后重试',
  42004: '当前订单状态不允许执行该操作',
  42005: '当前订单类型不支持该操作',
  42006: '订单不存在',
  43001: '可用余额不足，请先充值',
  43002: '资金账户不存在，请联系管理员',
  50000: '服务器内部错误，请稍后重试'
}

class RequestError extends Error {
  constructor(message, options = {}) {
    super(message)
    this.name = 'RequestError'
    this.code = options.code || null
    this.httpStatus = options.httpStatus || null
    this.kind = options.kind || 'unknown'
    this.raw = options.raw
  }
}

function normalizeBizMessage(message, fallback = '服务异常，请稍后重试', code) {
  if (code && codeAliasMap[code]) {
    return codeAliasMap[code]
  }
  if (typeof message === 'string' && message.trim()) {
    const text = message.trim()
    if (
      text.toLowerCase() !== 'internal server error' &&
      text.toLowerCase() !== 'forbidden' &&
      text.toLowerCase() !== 'unauthorized'
    ) {
      return text
    }
  }
  return fallback
}

function shouldToast(config) {
  const mode = config && config.errorMode
  return mode !== 'silent' && mode !== 'inline'
}

const service = axios.create({
  baseURL: '/',
  timeout: 10000
})

service.interceptors.request.use((config) => {
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
      return Promise.reject(new RequestError('请勿重复提交', { kind: 'biz', code: 40000 }))
    }
    pendingWriteMap.set(requestKey, now)
    config.__requestKey = requestKey
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const requestKey = response.config && response.config.__requestKey
    if (requestKey) {
      pendingWriteMap.delete(requestKey)
    }
    const payload = response.data
    if (
      (response.config && response.config.rawResponse) ||
      (response.config && response.config.responseType === 'blob')
    ) {
      return payload
    }
    if (!payload || payload.code !== 200) {
      const code = payload && typeof payload.code !== 'undefined' ? payload.code : 50000
      const msg = normalizeBizMessage(payload && payload.msg, '请求失败', code)
      if (shouldToast(response.config)) {
        ElMessage.error(msg)
      }
      return Promise.reject(
        new RequestError(msg, {
          kind: 'biz',
          code,
          httpStatus: response.status,
          raw: payload
        })
      )
    }
    return payload.data
  },
  (error) => {
    const requestKey = error.config && error.config.__requestKey
    if (requestKey) {
      pendingWriteMap.delete(requestKey)
    }

    const status = error.response && error.response.status
    const payload = error.response && error.response.data
    const bizCode = payload && typeof payload.code !== 'undefined' ? payload.code : null

    if (status === 401 || bizCode === 401) {
      store.commit('clearAuth')
      const redirect = router.currentRoute.value && router.currentRoute.value.fullPath
      router.push({ path: '/login', query: redirect ? { redirect } : {} })
      return Promise.reject(
        new RequestError('登录状态已失效，请重新登录', {
          kind: 'auth',
          code: 401,
          httpStatus: 401,
          raw: payload || error
        })
      )
    }

    if (status === 403 || bizCode === 40300) {
      const message = normalizeBizMessage(payload && payload.msg, '当前账号无权限执行该操作', 40300)
      if (shouldToast(error.config)) {
        ElMessage.error(message)
      }
      return Promise.reject(
        new RequestError(message, {
          kind: 'biz',
          code: 40300,
          httpStatus: status || 403,
          raw: payload || error
        })
      )
    }

    if (payload && typeof payload.code !== 'undefined') {
      const message = normalizeBizMessage(payload.msg, '请求失败', payload.code)
      if (shouldToast(error.config)) {
        ElMessage.error(message)
      }
      return Promise.reject(
        new RequestError(message, {
          kind: 'biz',
          code: payload.code,
          httpStatus: status,
          raw: payload
        })
      )
    }

    const message = normalizeBizMessage(error.message, '网络异常，请检查连接后重试')
    if (shouldToast(error.config)) {
      ElMessage.error(message)
    }
    return Promise.reject(
      new RequestError(message, {
        kind: 'network',
        code: null,
        httpStatus: status || null,
        raw: error
      })
    )
  }
)

export default service
