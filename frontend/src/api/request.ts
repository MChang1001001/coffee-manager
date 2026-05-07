import axios from 'axios'
import type { AxiosRequestConfig } from 'axios'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

const TOKEN_STORAGE_KEY = 'coffee_manager_token'

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = getAuthToken()

  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }

  return config
})

export function getAuthToken() {
  return window.localStorage.getItem(TOKEN_STORAGE_KEY)
}

export function setAuthToken(token: string) {
  window.localStorage.setItem(TOKEN_STORAGE_KEY, token)
}

export function clearAuthToken() {
  window.localStorage.removeItem(TOKEN_STORAGE_KEY)
}

export async function request<T>(config: AxiosRequestConfig) {
  try {
    const response = await http.request<ApiResponse<T>>(config)
    const body = response.data

    if (!body || body.code !== 0) {
      throw new Error(body?.message || '请求失败')
    }

    return body.data
  } catch (caughtError) {
    throw new Error(getRequestErrorMessage(caughtError))
  }
}

export function getRequestErrorMessage(caughtError: unknown) {
  if (axios.isAxiosError<ApiResponse<unknown>>(caughtError)) {
    const responseMessage = caughtError.response?.data?.message

    if (responseMessage) {
      return responseMessage
    }

    if (caughtError.response) {
      return `后端响应异常：${caughtError.response.status}`
    }

    if (caughtError.request) {
      return '无法连接后端服务，请确认 Spring Boot 已启动并且 Vite 代理可用。'
    }

    return caughtError.message
  }

  if (caughtError instanceof Error) {
    return caughtError.message
  }

  return '未知错误'
}
