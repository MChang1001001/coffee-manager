import { clearAuthToken, getAuthToken, request, setAuthToken } from './request'

export interface UserProfile {
  id: number
  username: string
  nickname: string | null
  avatarUrl: string | null
}

export interface LoginData {
  token: string
  user: UserProfile
}

const DEV_LOGIN_CREDENTIALS = {
  username: 'admin',
  password: 'admin123456',
}

export async function loginWithDefaultAdmin() {
  const loginData = await request<LoginData>({
    url: '/auth/login',
    method: 'POST',
    data: DEV_LOGIN_CREDENTIALS,
  })

  setAuthToken(loginData.token)
  return loginData
}

export function fetchCurrentUser() {
  return request<UserProfile>({
    url: '/auth/me',
    method: 'GET',
  })
}

export async function ensureDevAuth() {
  if (getAuthToken()) {
    try {
      return await fetchCurrentUser()
    } catch {
      clearAuthToken()
    }
  }

  const loginData = await loginWithDefaultAdmin()
  return loginData.user
}
