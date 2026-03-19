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
      source: 'Vue组件',
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
      event.preventDefault()
      return
    }
    if (shouldIgnoreGlobalNoise(event.reason)) {
      event.preventDefault()
      return
    }
    reportGlobalError(event.reason, { source: '未处理Promise异常' })
  })
}
