<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios, { AxiosError } from 'axios'

interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

interface HealthData {
  status: string
  database: string
}

interface UserProfile {
  id: number
  username: string
  nickname: string | null
  avatarUrl: string | null
}

interface LoginData {
  token: string
  user: UserProfile
}

const checking = ref(true)
const backendStatus = ref('checking')
const databaseStatus = ref('checking')
const authStatus = ref('checking')
const currentUser = ref<UserProfile | null>(null)
const tokenPreview = ref('')
const error = ref('')

onMounted(async () => {
  await checkBackend()
})

async function checkBackend() {
  checking.value = true
  error.value = ''
  currentUser.value = null
  tokenPreview.value = ''

  try {
    const healthResponse = await axios.get<ApiResponse<HealthData>>('/api/health')
    const health = unwrapResponse(healthResponse.data, '健康检查')
    backendStatus.value = health.status
    databaseStatus.value = health.database

    const loginResponse = await axios.post<ApiResponse<LoginData>>('/api/auth/login', {
      username: 'admin',
      password: 'admin123456',
    })
    const login = unwrapResponse(loginResponse.data, '登录认证')
    authStatus.value = 'ok'
    tokenPreview.value = `${login.token.slice(0, 20)}...`

    const meResponse = await axios.get<ApiResponse<UserProfile>>('/api/auth/me', {
      headers: {
        Authorization: `Bearer ${login.token}`,
      },
    })
    currentUser.value = unwrapResponse(meResponse.data, '当前用户')
  } catch (caughtError) {
    backendStatus.value = 'error'
    error.value = getErrorMessage(caughtError)
  } finally {
    checking.value = false
  }
}

function unwrapResponse<T>(response: ApiResponse<T>, step: string): T {
  if (response.code !== 0) {
    throw new Error(`${step}失败：${response.message}`)
  }
  return response.data
}

function getErrorMessage(caughtError: unknown) {
  if (caughtError instanceof AxiosError) {
    if (caughtError.response) {
      return `后端响应异常：${caughtError.response.status}`
    }
    return '后端连接失败，请确认 Spring Boot 已在 8080 端口启动。'
  }
  if (caughtError instanceof Error) {
    return caughtError.message
  }
  return '未知错误'
}
</script>

<template>
  <main class="page">
    <section class="panel">
      <div class="heading">
        <p class="label">Coffee Manager</p>
        <h1>后端骨架联调</h1>
      </div>

      <div class="status-list">
        <div class="status-row">
          <span>服务</span>
          <strong :class="{ ok: backendStatus === 'ok' }">{{ backendStatus }}</strong>
        </div>
        <div class="status-row">
          <span>数据库</span>
          <strong :class="{ ok: databaseStatus === 'ok' }">{{ databaseStatus }}</strong>
        </div>
        <div class="status-row">
          <span>JWT 登录</span>
          <strong :class="{ ok: authStatus === 'ok' }">{{ authStatus }}</strong>
        </div>
      </div>

      <div v-if="currentUser" class="user-card">
        <span>当前用户</span>
        <strong>{{ currentUser.nickname || currentUser.username }}</strong>
        <small>{{ currentUser.username }} · {{ tokenPreview }}</small>
      </div>

      <p v-if="error" class="error">{{ error }}</p>

      <button type="button" :disabled="checking" @click="checkBackend">
        {{ checking ? '检查中' : '重新检查' }}
      </button>
    </section>
  </main>
</template>
