import axios from 'axios'

const DEFAULT_FALLBACK_MESSAGE = '操作失败，请稍后重试。'
const NETWORK_ERROR_MESSAGE = '网络连接失败，请确认后端服务是否已启动。'
const AUTH_ERROR_MESSAGE = '登录状态异常，请重新打开页面后重试。'
const NOT_FOUND_MESSAGE = '要查看的数据不存在，可能已经被删除。'
const SERVER_ERROR_MESSAGE = '服务暂时出错，请稍后重试。'
const TIMEOUT_ERROR_MESSAGE = '请求超时，请稍后重试。'

export function getFriendlyErrorMessage(
  caughtError: unknown,
  fallbackMessage = DEFAULT_FALLBACK_MESSAGE,
) {
  const safeFallback = fallbackMessage.trim() || DEFAULT_FALLBACK_MESSAGE

  if (axios.isAxiosError(caughtError)) {
    const responseMessage = extractResponseMessage(caughtError.response?.data)

    if (responseMessage && hasChineseText(responseMessage)) {
      return responseMessage
    }

    const status = caughtError.response?.status

    if (status === 401 || status === 403) {
      return AUTH_ERROR_MESSAGE
    }

    if (status === 404) {
      return NOT_FOUND_MESSAGE
    }

    if (status && status >= 500) {
      return SERVER_ERROR_MESSAGE
    }

    if (isTimeoutError(caughtError)) {
      return TIMEOUT_ERROR_MESSAGE
    }

    if (caughtError.response) {
      return safeFallback
    }

    if (caughtError.message === 'Network Error' || caughtError.request) {
      return NETWORK_ERROR_MESSAGE
    }

    return safeFallback
  }

  if (caughtError instanceof Error) {
    return normalizeNonAxiosMessage(caughtError.message, safeFallback)
  }

  if (typeof caughtError === 'string') {
    return normalizeNonAxiosMessage(caughtError, safeFallback)
  }

  return safeFallback
}

function normalizeNonAxiosMessage(message: string, fallbackMessage: string) {
  const trimmed = message.trim()

  if (!trimmed) {
    return fallbackMessage
  }

  if (hasChineseText(trimmed)) {
    return trimmed
  }

  if (trimmed === 'Network Error') {
    return NETWORK_ERROR_MESSAGE
  }

  if (/timeout|exceeded/i.test(trimmed)) {
    return TIMEOUT_ERROR_MESSAGE
  }

  return fallbackMessage
}

function extractResponseMessage(data: unknown) {
  if (!data || typeof data !== 'object') {
    return ''
  }

  const message = (data as { message?: unknown }).message
  return typeof message === 'string' ? message.trim() : ''
}

function hasChineseText(value: string) {
  return /[\u4e00-\u9fff]/.test(value)
}

function isTimeoutError(caughtError: unknown) {
  return (
    axios.isAxiosError(caughtError) &&
    (caughtError.code === 'ECONNABORTED' || /timeout/i.test(caughtError.message))
  )
}
