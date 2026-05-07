<script setup lang="ts">
import { onMounted, ref } from 'vue'
import axios from 'axios'

const message = ref('loading...')
const error = ref('')

onMounted(async () => {
  try {
    const response = await axios.get<string>('/api/hello')
    message.value = response.data
  } catch {
    error.value = '后端连接失败，请确认 Spring Boot 已在 8080 端口启动。'
    message.value = ''
  }
})
</script>

<template>
  <main class="page">
    <section class="panel">
      <p class="label">Coffee Manager Connectivity Check</p>
      <h1>{{ message || error }}</h1>
    </section>
  </main>
</template>
