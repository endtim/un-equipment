import { reactive } from 'vue'

const defaultState = {
  hasError: false,
  title: '页面出现异常',
  message: '系统发生了未预期错误，请稍后重试。',
  detail: '',
  source: '',
  time: ''
}

const errorState = reactive({ ...defaultState })

function stringifyError(error) {
  if (!error) {
    return ''
  }
  if (typeof error === 'string') {
    return error
  }
  if (error instanceof Error) {
    return error.message || String(error)
  }
  if (typeof error === 'object' && typeof error.message === 'string') {
    return error.message
  }
  try {
    return JSON.stringify(error)
  } catch (e) {
    return String(error)
  }
}

function isHandledRequestError(error) {
  // 请求层已对业务/鉴权/网络错误做统一提示，这类异常不再上报全局错误页，避免重复弹错。
  return (
    !!error &&
    (error.name === 'RequestError' ||
      error.kind === 'biz' ||
      error.kind === 'network' ||
      error.kind === 'auth')
  )
}

function shouldIgnoreGlobalNoise(error) {
  const message = stringifyError(error)
  if (!message) {
    return false
  }
  const normalized = String(message).toLowerCase()
  return (
    // 忽略浏览器已知噪音，避免无业务影响的 ResizeObserver 警告污染全局错误中心。
    normalized.includes('resizeobserver loop completed with undelivered notifications') ||
    normalized.includes('resizeobserver loop limit exceeded')
  )
}

export function reportGlobalError(error, context = {}) {
  const message = stringifyError(error) || defaultState.message
  errorState.hasError = true
  errorState.title = context.title || defaultState.title
  errorState.message = message
  errorState.detail = context.detail || (error && error.stack ? error.stack : '')
  errorState.source = context.source || '运行时'
  errorState.time = new Date().toLocaleString()
  // eslint-disable-next-line no-console
  console.error('[GlobalError]', context, error)
}

export function clearGlobalError() {
  Object.assign(errorState, defaultState)
}

export function useGlobalErrorState() {
  return errorState
}

export function installGlobalErrorHandlers({ app, router }) {
  app.config.errorHandler = (error, instance, info) => {
    if (isHandledRequestError(error)) {
      return
    }
    if (shouldIgnoreGlobalNoise(error)) {
      return
    }
    reportGlobalError(error, {
      source: '组件渲染',
      detail: info
    })
  }

  if (router && typeof router.onError === 'function') {
    router.onError((error) => {
      reportGlobalError(error, { source: '路由' })
    })
  }

  window.addEventListener('error', (event) => {
    if (shouldIgnoreGlobalNoise(event.error || event.message)) {
      return
    }
    reportGlobalError(event.error || event.message, { source: '窗口错误' })
  })

  window.addEventListener('unhandledrejection', (event) => {
    if (isHandledRequestError(event.reason)) {
      // 已被请求层消费的 Promise 异常直接阻断默认控制台报错。
      event.preventDefault()
      return
    }
    if (shouldIgnoreGlobalNoise(event.reason)) {
      event.preventDefault()
      return
    }
    reportGlobalError(event.reason, { source: '未处理异步异常' })
  })
}
