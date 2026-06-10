<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ensureDevAuth } from '../api/auth'
import { listBrewRecords } from '../api/brew'
import type { BrewRecord } from '../api/brew'
import { generateCoffeeAiSummary, getCoffeeBean, updateCoffeeSummary } from '../api/coffee'
import type { CoffeeBeanDetail, CoffeeSummaryDraft, CoffeeSummaryPayload } from '../api/coffee'
import { listCoffeeReviews } from '../api/review'
import type { CoffeeReview } from '../api/review'
import { getFriendlyErrorMessage } from '../utils/errorMessage'

type CoffeeAction = 'edit' | 'review' | 'brew'
type SummarySource = 'MANUAL' | 'AI'

interface SummaryForm {
  summaryTitle: string
  flavorSummary: string
  brewSuggestion: string
  repurchaseIntention: string
  summaryText: string
  summarySource: SummarySource
}

const defaultSummaryForm: SummaryForm = {
  summaryTitle: '',
  flavorSummary: '',
  brewSuggestion: '',
  repurchaseIntention: '',
  summaryText: '',
  summarySource: 'MANUAL',
}

const repurchaseOptions = ['', '未决定', '会回购', '看情况', '不回购']

const roastLevelLabels: Record<string, string> = {
  LIGHT: '浅烘',
  MEDIUM_LIGHT: '中浅烘',
  MEDIUM: '中烘',
  MEDIUM_DARK: '中深烘',
  DARK: '深烘',
  UNKNOWN: '未知',
}

const route = useRoute()

const bean = ref<CoffeeBeanDetail | null>(null)
const loading = ref(false)
const error = ref('')
const coverImageFailed = ref(false)
const recentReviews = ref<CoffeeReview[]>([])
const recentBrewRecords = ref<BrewRecord[]>([])
const reviewSummaryLoading = ref(false)
const brewSummaryLoading = ref(false)
const reviewSummaryError = ref('')
const brewSummaryError = ref('')
const summaryForm = reactive<SummaryForm>({ ...defaultSummaryForm })
const isSummaryDialogOpen = ref(false)
const summaryGenerating = ref(false)
const summarySaving = ref(false)
const summaryActionError = ref('')
const summaryActionNotice = ref('')
const summaryFormError = ref('')

let beanFetchVersion = 0
let reviewSummaryFetchVersion = 0
let brewSummaryFetchVersion = 0

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
const hasRecentReviews = computed(() => recentReviews.value.length > 0)
const hasRecentBrewRecords = computed(() => recentBrewRecords.value.length > 0)
const hasSummary = computed(() =>
  Boolean(
    bean.value &&
      [
        bean.value.summaryTitle,
        bean.value.flavorSummary,
        bean.value.brewSuggestion,
        bean.value.repurchaseIntention,
        bean.value.summaryText,
      ].some(hasText),
  ),
)
const summaryGeneratedMeta = computed(() => {
  if (!bean.value?.summarySource && !bean.value?.summaryGeneratedAt) {
    return ''
  }

  const sourceLabel = bean.value.summarySource === 'AI' ? 'AI 辅助生成' : '手动整理'
  return bean.value.summaryGeneratedAt
    ? `${sourceLabel}于 ${timeDisplay(bean.value.summaryGeneratedAt)}`
    : sourceLabel
})

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
    beanFetchVersion += 1
    bean.value = null
    error.value = '咖啡豆 ID 不正确。'
    resetSummaryState()
    return
  }

  const beanId = routeBeanId.value
  const fetchVersion = ++beanFetchVersion

  loading.value = true
  error.value = ''
  coverImageFailed.value = false
  resetSummaryState()

  try {
    await ensureDevAuth()
    const detail = await getCoffeeBean(beanId)

    if (!isCurrentBeanFetch(beanId, fetchVersion)) {
      return
    }

    bean.value = detail
    void fetchReviewSummary(beanId)
    void fetchBrewSummary(beanId)
  } catch (caughtError) {
    if (!isCurrentBeanFetch(beanId, fetchVersion)) {
      return
    }

    bean.value = null
    error.value = getDetailErrorMessage(caughtError)
  } finally {
    if (isCurrentBeanFetch(beanId, fetchVersion)) {
      loading.value = false
    }
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

async function fetchReviewSummary(beanId: number | null = actionBeanId.value) {
  if (!beanId) {
    return
  }

  const fetchVersion = ++reviewSummaryFetchVersion
  reviewSummaryLoading.value = true
  reviewSummaryError.value = ''
  recentReviews.value = []

  try {
    const page = await listCoffeeReviews(beanId, {
      page: 1,
      pageSize: 3,
    })

    if (isCurrentReviewSummaryFetch(beanId, fetchVersion)) {
      recentReviews.value = page.items ?? []
    }
  } catch (caughtError) {
    if (isCurrentReviewSummaryFetch(beanId, fetchVersion)) {
      reviewSummaryError.value = getFriendlyErrorMessage(caughtError, '最近评价加载失败，请稍后重试。')
    }
  } finally {
    if (isCurrentReviewSummaryFetch(beanId, fetchVersion)) {
      reviewSummaryLoading.value = false
    }
  }
}

async function fetchBrewSummary(beanId: number | null = actionBeanId.value) {
  if (!beanId) {
    return
  }

  const fetchVersion = ++brewSummaryFetchVersion
  brewSummaryLoading.value = true
  brewSummaryError.value = ''
  recentBrewRecords.value = []

  try {
    const page = await listBrewRecords(beanId, {
      page: 1,
      pageSize: 3,
    })

    if (isCurrentBrewSummaryFetch(beanId, fetchVersion)) {
      recentBrewRecords.value = page.items ?? []
    }
  } catch (caughtError) {
    if (isCurrentBrewSummaryFetch(beanId, fetchVersion)) {
      brewSummaryError.value = getFriendlyErrorMessage(caughtError, '最近冲煮记录加载失败，请稍后重试。')
    }
  } finally {
    if (isCurrentBrewSummaryFetch(beanId, fetchVersion)) {
      brewSummaryLoading.value = false
    }
  }
}

function reloadReviewSummary() {
  void fetchReviewSummary()
}

function reloadBrewSummary() {
  void fetchBrewSummary()
}

async function generateSummaryDraft() {
  if (!actionBeanId.value) {
    return
  }

  const beanId = actionBeanId.value
  summaryGenerating.value = true
  summaryActionError.value = ''
  summaryActionNotice.value = ''
  summaryFormError.value = ''

  try {
    await ensureDevAuth()
    const draft = await generateCoffeeAiSummary(beanId)

    if (routeBeanId.value !== beanId) {
      return
    }

    fillSummaryForm(draft, 'AI')
    isSummaryDialogOpen.value = true
  } catch (caughtError) {
    if (routeBeanId.value === beanId) {
      summaryActionError.value = getFriendlyErrorMessage(caughtError, 'AI 总结生成失败，请稍后重试。')
    }
  } finally {
    if (routeBeanId.value === beanId) {
      summaryGenerating.value = false
    }
  }
}

function openSummaryDialog() {
  if (!bean.value) {
    return
  }

  fillSummaryForm(bean.value, normalizeSummarySource(bean.value.summarySource))
  summaryFormError.value = ''
  summaryActionError.value = ''
  summaryActionNotice.value = ''
  isSummaryDialogOpen.value = true
}

function closeSummaryDialog() {
  if (summarySaving.value) {
    return
  }

  isSummaryDialogOpen.value = false
  summaryFormError.value = ''
}

async function saveSummary() {
  if (!actionBeanId.value) {
    return
  }

  const beanId = actionBeanId.value
  summarySaving.value = true
  summaryFormError.value = ''

  try {
    await ensureDevAuth()
    await updateCoffeeSummary(beanId, toSummaryPayload())
    const detail = await getCoffeeBean(beanId)

    if (routeBeanId.value !== beanId) {
      return
    }

    bean.value = detail
    isSummaryDialogOpen.value = false
    summaryActionError.value = ''
    summaryActionNotice.value = '评测总结已保存。'
  } catch (caughtError) {
    if (routeBeanId.value === beanId) {
      summaryFormError.value = getFriendlyErrorMessage(caughtError, '总结保存失败，请稍后重试。')
    }
  } finally {
    if (routeBeanId.value === beanId) {
      summarySaving.value = false
    }
  }
}

function fillSummaryForm(source: CoffeeSummaryDraft | CoffeeBeanDetail, summarySource: SummarySource) {
  summaryForm.summaryTitle = source.summaryTitle ?? ''
  summaryForm.flavorSummary = source.flavorSummary ?? ''
  summaryForm.brewSuggestion = source.brewSuggestion ?? ''
  summaryForm.repurchaseIntention = source.repurchaseIntention ?? ''
  summaryForm.summaryText = source.summaryText ?? ''
  summaryForm.summarySource = summarySource
}

function resetSummaryForm() {
  Object.assign(summaryForm, defaultSummaryForm)
}

function toSummaryPayload(): CoffeeSummaryPayload {
  return {
    summaryTitle: emptyToNull(summaryForm.summaryTitle),
    flavorSummary: emptyToNull(summaryForm.flavorSummary),
    brewSuggestion: emptyToNull(summaryForm.brewSuggestion),
    repurchaseIntention: emptyToNull(summaryForm.repurchaseIntention),
    summaryText: emptyToNull(summaryForm.summaryText),
    summarySource: summaryForm.summarySource,
  }
}

function getDetailErrorMessage(caughtError: unknown) {
  const message = getFriendlyErrorMessage(caughtError, '咖啡豆档案加载失败，请稍后重试。')

  if (
    message.includes('咖啡豆不存在') ||
    message.includes('资源不存在') ||
    message.includes('数据不存在')
  ) {
    return '这包豆子的档案没有找到，可能已经被删除了。'
  }

  return message
}

function resetSummaryState() {
  reviewSummaryFetchVersion += 1
  brewSummaryFetchVersion += 1
  recentReviews.value = []
  recentBrewRecords.value = []
  reviewSummaryLoading.value = false
  brewSummaryLoading.value = false
  reviewSummaryError.value = ''
  brewSummaryError.value = ''
  summaryGenerating.value = false
  summarySaving.value = false
  summaryActionError.value = ''
  summaryActionNotice.value = ''
  summaryFormError.value = ''
  isSummaryDialogOpen.value = false
  resetSummaryForm()
}

function isCurrentBeanFetch(beanId: number, fetchVersion: number) {
  return fetchVersion === beanFetchVersion && routeBeanId.value === beanId
}

function isCurrentReviewSummaryFetch(beanId: number, fetchVersion: number) {
  return (
    fetchVersion === reviewSummaryFetchVersion &&
    routeBeanId.value === beanId &&
    bean.value?.id === beanId
  )
}

function isCurrentBrewSummaryFetch(beanId: number, fetchVersion: number) {
  return (
    fetchVersion === brewSummaryFetchVersion &&
    routeBeanId.value === beanId &&
    bean.value?.id === beanId
  )
}

function display(value: string | number | null | undefined) {
  return value === null || value === undefined || value === '' ? '-' : value
}

function hasText(value: string | null | undefined) {
  return Boolean(value?.trim())
}

function emptyToNull(value: string) {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

function normalizeSummarySource(value: string | null | undefined): SummarySource {
  return value?.trim().toUpperCase() === 'AI' ? 'AI' : 'MANUAL'
}

function roastLevelLabel(value: string | null | undefined) {
  const normalized = value?.trim()

  if (!normalized) {
    return '-'
  }

  return roastLevelLabels[normalized] ?? normalized
}

function dateDisplay(value: string | null | undefined) {
  return value?.trim() ? value : '未填写'
}

function timeDisplay(value: string | null | undefined) {
  const rawValue = value?.trim()

  if (!rawValue) {
    return '未记录时间'
  }

  return rawValue.replace('T', ' ').slice(0, 16)
}

function excerpt(value: string | null | undefined, fallback: string) {
  const rawValue = value?.trim()

  if (!rawValue) {
    return fallback
  }

  return rawValue.length > 86 ? `${rawValue.slice(0, 86)}...` : rawValue
}

function unitDisplay(value: string | number | null | undefined, unit: string) {
  const displayedValue = display(value)
  return displayedValue === '-' ? '-' : `${displayedValue}${unit}`
}

function brewNoteSummary(record: BrewRecord) {
  return excerpt(
    joinParts(record.resultSummary, record.resultNotes),
    '还没有复盘备注，下一次冲煮时再补上一笔。',
  )
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
              <dt>品种</dt>
              <dd>{{ display(bean.variety) }}</dd>
            </div>
            <div>
              <dt>烘焙度</dt>
              <dd>{{ roastLevelLabel(bean.roastLevel) }}</dd>
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

          <section class="detail-ai-summary" aria-labelledby="coffee-summary-title">
            <header class="detail-summary-header">
              <div>
                <p class="mini-label">AI tasting note</p>
                <h2 id="coffee-summary-title">评测总结</h2>
              </div>
              <div class="summary-action-group">
                <button
                  type="button"
                  class="secondary compact-button"
                  :disabled="summaryGenerating || summarySaving"
                  @click="generateSummaryDraft"
                >
                  {{ summaryGenerating ? '生成中' : 'AI 生成总结' }}
                </button>
                <button
                  type="button"
                  class="secondary compact-button"
                  :disabled="summaryGenerating || summarySaving"
                  @click="openSummaryDialog"
                >
                  编辑总结
                </button>
              </div>
            </header>

            <p v-if="summaryActionNotice" class="alert success summary-alert">{{ summaryActionNotice }}</p>
            <p v-if="summaryActionError" class="alert error summary-alert">{{ summaryActionError }}</p>

            <div v-if="!hasSummary" class="state-box summary-state summary-empty">
              还没有评测总结，点一下 AI 总结，让系统先帮你打个草稿吧。
            </div>
            <article v-else class="summary-detail-card">
              <header>
                <strong>{{ display(bean.summaryTitle) }}</strong>
                <span v-if="summaryGeneratedMeta">{{ summaryGeneratedMeta }}</span>
              </header>
              <dl class="summary-detail-grid">
                <div>
                  <dt>风味总结</dt>
                  <dd>{{ display(bean.flavorSummary) }}</dd>
                </div>
                <div>
                  <dt>冲煮建议</dt>
                  <dd>{{ display(bean.brewSuggestion) }}</dd>
                </div>
                <div>
                  <dt>回购意向</dt>
                  <dd>{{ display(bean.repurchaseIntention) }}</dd>
                </div>
                <div class="wide">
                  <dt>总结正文</dt>
                  <dd>{{ display(bean.summaryText) }}</dd>
                </div>
              </dl>
            </article>
          </section>

          <section class="detail-summary-board" aria-label="最近记录">
            <section class="detail-summary-note review-summary-note" aria-labelledby="recent-review-title">
              <header class="detail-summary-header">
                <div>
                  <p class="mini-label">Recent review</p>
                  <h2 id="recent-review-title">最近评价</h2>
                </div>
                <RouterLink class="button-link secondary compact-button" :to="coffeeActionTo('review')">
                  管理评价
                </RouterLink>
              </header>

              <div v-if="reviewSummaryLoading" class="state-box summary-state" aria-live="polite">
                正在翻最近的评价...
              </div>
              <div v-else-if="reviewSummaryError" class="state-box summary-state summary-error">
                <p>{{ reviewSummaryError }}</p>
                <button type="button" class="secondary compact-button" @click="reloadReviewSummary">重试</button>
              </div>
              <div v-else-if="!hasRecentReviews" class="state-box summary-state summary-empty">
                暂时没有评价，喝完这一杯再写点感受。
              </div>
              <div v-else class="summary-list">
                <article v-for="review in recentReviews" :key="review.id" class="summary-card">
                  <header class="summary-card-header">
                    <strong>综合 {{ display(review.overallRating) }}</strong>
                    <span>{{ timeDisplay(review.createdAt) }}</span>
                  </header>
                  <p>{{ excerpt(review.content, '这条评价还没有写内容。') }}</p>
                </article>
              </div>
            </section>

            <section class="detail-summary-note brew-summary-note" aria-labelledby="recent-brew-title">
              <header class="detail-summary-header">
                <div>
                  <p class="mini-label">Recent brew</p>
                  <h2 id="recent-brew-title">最近冲煮</h2>
                </div>
                <RouterLink class="button-link secondary compact-button" :to="coffeeActionTo('brew')">
                  管理冲煮
                </RouterLink>
              </header>

              <div v-if="brewSummaryLoading" class="state-box summary-state" aria-live="polite">
                正在翻最近的冲煮记录...
              </div>
              <div v-else-if="brewSummaryError" class="state-box summary-state summary-error">
                <p>{{ brewSummaryError }}</p>
                <button type="button" class="secondary compact-button" @click="reloadBrewSummary">重试</button>
              </div>
              <div v-else-if="!hasRecentBrewRecords" class="state-box summary-state summary-empty">
                还没有冲煮记录，下一次开冲时记一笔。
              </div>
              <div v-else class="summary-list">
                <article v-for="record in recentBrewRecords" :key="record.id" class="summary-card">
                  <header class="summary-card-header">
                    <strong>{{ display(record.brewMethod) }}</strong>
                    <span>{{ timeDisplay(record.createdAt) }}</span>
                  </header>
                  <dl class="summary-chip-list">
                    <div>
                      <dt>粉</dt>
                      <dd>{{ unitDisplay(record.beanAmountGrams, 'g') }}</dd>
                    </div>
                    <div>
                      <dt>水</dt>
                      <dd>{{ unitDisplay(record.waterAmountMl, 'ml') }}</dd>
                    </div>
                    <div>
                      <dt>温</dt>
                      <dd>{{ unitDisplay(record.waterTemperature, '°C') }}</dd>
                    </div>
                  </dl>
                  <p>{{ brewNoteSummary(record) }}</p>
                </article>
              </div>
            </section>
          </section>
        </div>
      </article>
    </section>

    <div v-if="isSummaryDialogOpen" class="dialog-backdrop" role="presentation">
      <section class="dialog-panel summary-form-dialog-panel" role="dialog" aria-modal="true" aria-labelledby="summary-dialog-title">
        <header class="dialog-header">
          <div>
            <h2 id="summary-dialog-title">编辑评测总结</h2>
            <p>{{ summaryForm.summarySource === 'AI' ? 'AI 草稿已填入，可先修改再保存。' : '整理这包豆子的个人评测笔记。' }}</p>
          </div>
          <button type="button" class="icon-button" :disabled="summarySaving" aria-label="关闭" @click="closeSummaryDialog">
            ×
          </button>
        </header>

        <form class="summary-form" @submit.prevent="saveSummary">
          <p v-if="summaryFormError" class="alert error form-alert">{{ summaryFormError }}</p>

          <label class="field">
            <span>一句话总结</span>
            <input v-model="summaryForm.summaryTitle" type="text" maxlength="128" placeholder="例如：清甜花香，适合手冲慢慢喝" />
          </label>

          <label class="field">
            <span>风味总结</span>
            <textarea v-model="summaryForm.flavorSummary" rows="4" placeholder="记录主要风味、口感和变化"></textarea>
          </label>

          <label class="field">
            <span>冲煮建议</span>
            <textarea v-model="summaryForm.brewSuggestion" rows="4" placeholder="记录推荐参数、研磨、水温或下次调整方向"></textarea>
          </label>

          <label class="field">
            <span>回购意向</span>
            <select v-model="summaryForm.repurchaseIntention">
              <option v-for="option in repurchaseOptions" :key="option || 'EMPTY'" :value="option">
                {{ option || '暂不选择' }}
              </option>
            </select>
          </label>

          <label class="field">
            <span>总结正文</span>
            <textarea v-model="summaryForm.summaryText" rows="6" placeholder="写下完整评测总结"></textarea>
          </label>

          <div class="form-actions">
            <button type="submit" :disabled="summarySaving">{{ summarySaving ? '保存中' : '保存总结' }}</button>
            <button type="button" class="secondary" :disabled="summarySaving" @click="closeSummaryDialog">取消</button>
          </div>
        </form>
      </section>
    </div>
  </main>
</template>
