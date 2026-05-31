<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ensureDevAuth } from '../api/auth'
import { getCoffeeBean } from '../api/coffee'
import type { CoffeeBeanDetail } from '../api/coffee'
import { getRequestErrorMessage } from '../api/request'

type CoffeeAction = 'edit' | 'review' | 'brew'

const route = useRoute()

const bean = ref<CoffeeBeanDetail | null>(null)
const loading = ref(false)
const error = ref('')
const coverImageFailed = ref(false)

const routeBeanId = computed(() => {
  const id = Number(route.params.id)
  return Number.isInteger(id) && id > 0 ? id : null
})

const originLine = computed(() => {
  if (!bean.value) {
    return '-'
  }

  return joinParts(bean.value.origin, bean.value.region) || '未记录产地'
})

const actionBeanId = computed(() => bean.value?.id ?? routeBeanId.value)

onMounted(() => {
  void loadBean()
})

watch(
  () => route.params.id,
  () => {
    void loadBean()
  },
)

async function loadBean() {
  if (!routeBeanId.value) {
    bean.value = null
    error.value = '咖啡豆 ID 不正确。'
    return
  }

  loading.value = true
  error.value = ''
  coverImageFailed.value = false

  try {
    await ensureDevAuth()
    bean.value = await getCoffeeBean(routeBeanId.value)
  } catch (caughtError) {
    bean.value = null
    error.value = getRequestErrorMessage(caughtError)
  } finally {
    loading.value = false
  }
}

function markCoverImageFailed() {
  coverImageFailed.value = true
}

function coffeeActionTo(action: CoffeeAction) {
  return {
    name: 'coffee',
    query: {
      action,
      beanId: actionBeanId.value,
    },
  }
}

function display(value: string | number | null | undefined) {
  return value === null || value === undefined || value === '' ? '-' : value
}

function dateDisplay(value: string | null | undefined) {
  return value?.trim() ? value : '未填写'
}

function bestPeriodText(detail: Pick<CoffeeBeanDetail, 'bestFromDate' | 'bestToDate'>) {
  if (!detail.bestFromDate && !detail.bestToDate) {
    return '未填写'
  }

  return `${dateDisplay(detail.bestFromDate)} ~ ${dateDisplay(detail.bestToDate)}`
}

function drinkStatusLabel(detail: Pick<CoffeeBeanDetail, 'bestFromDate' | 'bestToDate'>) {
  return drinkStatus(detail).label
}

function drinkStatusClass(detail: Pick<CoffeeBeanDetail, 'bestFromDate' | 'bestToDate'>) {
  return drinkStatus(detail).className
}

function drinkStatus(detail: Pick<CoffeeBeanDetail, 'bestFromDate' | 'bestToDate'>) {
  if (!detail.bestFromDate || !detail.bestToDate) {
    return { label: '未填写日期', className: 'missing' }
  }

  const today = todayDateString()

  if (today < detail.bestFromDate) {
    return { label: '养豆中', className: 'resting' }
  }

  if (today > detail.bestToDate) {
    return { label: '已过赏味期', className: 'expired' }
  }

  if (daysBetweenLocalDates(today, detail.bestToDate) <= 7) {
    return { label: '即将过赏味期', className: 'expiring' }
  }

  return { label: '赏味期中', className: 'ready' }
}

function todayDateString() {
  const today = new Date()
  return [
    today.getFullYear(),
    padDatePart(today.getMonth() + 1),
    padDatePart(today.getDate()),
  ].join('-')
}

function padDatePart(value: number) {
  return String(value).padStart(2, '0')
}

function daysBetweenLocalDates(from: string, to: string) {
  const fromDate = parseLocalDate(from)
  const toDate = parseLocalDate(to)

  if (!fromDate || !toDate) {
    return Number.POSITIVE_INFINITY
  }

  return Math.round((toDate.getTime() - fromDate.getTime()) / 86_400_000)
}

function parseLocalDate(value: string) {
  const [year, month, day] = value.split('-').map(Number)

  if (!year || !month || !day) {
    return null
  }

  return new Date(year, month - 1, day)
}

function normalizeStatus(value: string | null | undefined) {
  return value?.trim().toUpperCase() ?? ''
}

function statusLabel(value: string | null | undefined) {
  const normalized = normalizeStatus(value)

  if (normalized === 'UNOPENED') {
    return '未开封'
  }

  if (normalized === 'OPENED') {
    return '已开封'
  }

  if (normalized === 'FINISHED') {
    return '已喝完'
  }

  return display(value)
}

function statusClass(value: string | null | undefined) {
  const normalized = normalizeStatus(value)

  if (normalized === 'OPENED') {
    return 'opened'
  }

  if (normalized === 'FINISHED') {
    return 'finished'
  }

  return 'unopened'
}

function joinParts(...parts: Array<string | null | undefined>) {
  return parts.map((part) => part?.trim()).filter(Boolean).join(' / ')
}
</script>

<template>
  <main class="coffee-page coffee-detail-page">
    <section class="detail-sheet" aria-labelledby="coffee-detail-title">
      <div class="detail-topbar">
        <RouterLink class="button-link secondary compact-button" to="/coffee">返回列表</RouterLink>
        <p class="eyebrow">Coffee file</p>
      </div>

      <div v-if="loading" class="state-box detail-state" aria-live="polite">正在加载咖啡豆档案...</div>

      <div v-else-if="error" class="state-box detail-state error-state">
        <p>{{ error }}</p>
        <button type="button" class="secondary compact-button" @click="loadBean">重试</button>
      </div>

      <article v-else-if="bean" class="detail-file">
        <div class="detail-cover-frame">
          <img
            v-if="bean.coverImageUrl && !coverImageFailed"
            class="detail-cover"
            :src="bean.coverImageUrl"
            :alt="`${bean.name} 包装封面`"
            @error="markCoverImageFailed"
          />
          <div v-else class="detail-cover detail-cover-placeholder cover-placeholder">
            <span>No cover</span>
          </div>
        </div>

        <div class="detail-main">
          <header class="detail-heading">
            <div>
              <p class="mini-label">豆子档案卡</p>
              <h1 id="coffee-detail-title">{{ bean.name }}</h1>
              <p class="detail-origin">{{ originLine }}</p>
            </div>
            <div class="detail-status-stack">
              <span class="status-badge inline" :class="statusClass(bean.status)">
                {{ statusLabel(bean.status) }}
              </span>
              <span class="drink-status-badge" :class="drinkStatusClass(bean)">
                {{ drinkStatusLabel(bean) }}
              </span>
            </div>
          </header>

          <div class="detail-actions">
            <RouterLink class="button-link secondary compact-button" :to="coffeeActionTo('edit')">编辑</RouterLink>
            <RouterLink class="button-link secondary compact-button" :to="coffeeActionTo('review')">评价</RouterLink>
            <RouterLink class="button-link secondary compact-button" :to="coffeeActionTo('brew')">冲煮</RouterLink>
          </div>

          <dl class="detail-score-strip">
            <div>
              <dt>评分</dt>
              <dd>{{ display(bean.overallRating) }}</dd>
            </div>
            <div>
              <dt>评价数</dt>
              <dd>{{ display(bean.reviewCount) }}</dd>
            </div>
            <div>
              <dt>冲煮数</dt>
              <dd>{{ display(bean.brewCount) }}</dd>
            </div>
          </dl>

          <dl class="detail-info-grid">
            <div>
              <dt>产地</dt>
              <dd>{{ originLine }}</dd>
            </div>
            <div>
              <dt>烘焙度</dt>
              <dd>{{ display(bean.roastLevel) }}</dd>
            </div>
            <div>
              <dt>处理法</dt>
              <dd>{{ display(bean.processMethod) }}</dd>
            </div>
            <div>
              <dt>烘焙日期</dt>
              <dd>{{ dateDisplay(bean.roastDate) }}</dd>
            </div>
            <div>
              <dt>赏味开始</dt>
              <dd>{{ dateDisplay(bean.bestFromDate) }}</dd>
            </div>
            <div>
              <dt>赏味结束</dt>
              <dd>{{ dateDisplay(bean.bestToDate) }}</dd>
            </div>
            <div class="wide">
              <dt>赏味期</dt>
              <dd>{{ bestPeriodText(bean) }}</dd>
            </div>
            <div class="wide">
              <dt>风味 / 备注</dt>
              <dd>{{ display(bean.notes) }}</dd>
            </div>
          </dl>
        </div>
      </article>
    </section>
  </main>
</template>
