<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ensureDevAuth } from '../api/auth'
import type { UserProfile } from '../api/auth'
import {
  createCoffeeBean,
  deleteCoffeeBean,
  getCoffeeBean,
  listCoffeeBeans,
  updateCoffeeBean,
} from '../api/coffee'
import { uploadCoffeeCover } from '../api/file'
import {
  createCoffeeReview,
  deleteCoffeeReview,
  listCoffeeReviews,
  updateCoffeeReview,
} from '../api/review'
import type {
  CoffeeBeanDetail,
  CoffeeBeanListItem,
  CoffeeBeanPayload,
  CoffeeBeanQuery,
  PageResponse,
} from '../api/coffee'
import type { CoffeeReview, CoffeeReviewPayload } from '../api/review'
import { getRequestErrorMessage } from '../api/request'

interface CoffeeBeanForm {
  name: string
  origin: string
  region: string
  farm: string
  variety: string
  processMethod: string
  roastLevel: string
  roaster: string
  roastDate: string
  purchaseDate: string
  openDate: string
  finishDate: string
  netWeightGrams: string | number
  price: string | number
  currency: string
  status: string
  coverImageUrl: string
  notes: string
}

interface ReviewForm {
  overallRating: string | number
  aromaRating: string | number
  acidityRating: string | number
  sweetnessRating: string | number
  bitternessRating: string | number
  bodyRating: string | number
  aftertasteRating: string | number
  content: string
}

type RatingFieldKey = Exclude<keyof ReviewForm, 'content'>

const defaultForm: CoffeeBeanForm = {
  name: '',
  origin: '',
  region: '',
  farm: '',
  variety: '',
  processMethod: '',
  roastLevel: '',
  roaster: '',
  roastDate: '',
  purchaseDate: '',
  openDate: '',
  finishDate: '',
  netWeightGrams: '',
  price: '',
  currency: 'CNY',
  status: 'UNOPENED',
  coverImageUrl: '',
  notes: '',
}

const defaultReviewForm: ReviewForm = {
  overallRating: '',
  aromaRating: '',
  acidityRating: '',
  sweetnessRating: '',
  bitternessRating: '',
  bodyRating: '',
  aftertasteRating: '',
  content: '',
}

const reviewRatingFields: Array<{ key: RatingFieldKey; label: string; required?: boolean }> = [
  { key: 'overallRating', label: '综合评分 *', required: true },
  { key: 'aromaRating', label: '香气评分' },
  { key: 'acidityRating', label: '酸度评分' },
  { key: 'sweetnessRating', label: '甜感评分' },
  { key: 'bitternessRating', label: '苦感评分' },
  { key: 'bodyRating', label: '醇厚度评分' },
  { key: 'aftertasteRating', label: '余韵评分' },
]

const reviewDimensionFields = reviewRatingFields.filter((field) => field.key !== 'overallRating')

const currentUser = ref<UserProfile | null>(null)
const beans = ref<CoffeeBeanListItem[]>([])
const loading = ref(false)
const saving = ref(false)
const coverUploading = ref(false)
const deletingId = ref<number | null>(null)
const editingLoadingId = ref<number | null>(null)
const error = ref('')
const formError = ref('')
const coverUploadError = ref('')
const coverUploadNotice = ref('')
const notice = ref('')
const formMode = ref<'create' | 'edit'>('create')
const editingId = ref<number | null>(null)
const isDialogOpen = ref(false)
const previewCover = ref<{ name: string; url: string } | null>(null)
const selectedReviewBean = ref<CoffeeBeanListItem | null>(null)
const reviews = ref<CoffeeReview[]>([])
const reviewLoading = ref(false)
const reviewSaving = ref(false)
const reviewDeletingId = ref<number | null>(null)
const reviewError = ref('')
const reviewFormError = ref('')
const reviewNotice = ref('')
const reviewMode = ref<'create' | 'edit'>('create')
const editingReviewId = ref<number | null>(null)
const isReviewDialogOpen = ref(false)

const filters = reactive({
  keyword: '',
  roastLevel: '',
  processMethod: '',
  origin: '',
  page: 1,
  pageSize: 20,
})

const pageState = reactive({
  total: 0,
  totalPages: 0,
})

const form = reactive<CoffeeBeanForm>({ ...defaultForm })
const reviewQuery = reactive({
  page: 1,
  pageSize: 20,
})
const reviewPageState = reactive({
  total: 0,
  totalPages: 0,
})
const reviewForm = reactive<ReviewForm>({ ...defaultReviewForm })

const formTitle = computed(() => (formMode.value === 'edit' ? '编辑咖啡豆' : '新增咖啡豆'))
const formDescription = computed(() => {
  if (formMode.value === 'edit' && editingId.value !== null) {
    return `ID: ${editingId.value}`
  }

  return '保存后自动刷新列表'
})
const submitText = computed(() => {
  if (saving.value) {
    return '保存中'
  }

  if (coverUploading.value) {
    return '封面上传中'
  }

  return formMode.value === 'edit' ? '保存修改' : '新增咖啡豆'
})
const hasRows = computed(() => beans.value.length > 0)
const hasReviews = computed(() => reviews.value.length > 0)
const effectiveTotalPages = computed(() => {
  if (pageState.total <= 0) {
    return 0
  }

  return Math.max(1, pageState.totalPages)
})
const canGoPrevious = computed(() => filters.page > 1 && !loading.value)
const canGoNext = computed(() => filters.page < effectiveTotalPages.value && !loading.value)
const reviewDialogTitle = computed(() => {
  if (!selectedReviewBean.value) {
    return '咖啡豆评价'
  }

  return `评价：${selectedReviewBean.value.name}`
})
const reviewFormTitle = computed(() => (reviewMode.value === 'edit' ? '编辑评价' : '新增评价'))
const reviewSubmitText = computed(() => {
  if (reviewSaving.value) {
    return '保存中'
  }

  return reviewMode.value === 'edit' ? '保存修改' : '新增评价'
})
const effectiveReviewTotalPages = computed(() => {
  if (reviewPageState.total <= 0) {
    return 0
  }

  return Math.max(1, reviewPageState.totalPages)
})
const canReviewGoPrevious = computed(() => reviewQuery.page > 1 && !reviewLoading.value)
const canReviewGoNext = computed(
  () => reviewQuery.page < effectiveReviewTotalPages.value && !reviewLoading.value,
)

onMounted(() => {
  void bootPage()
})

async function bootPage() {
  loading.value = true
  error.value = ''

  try {
    currentUser.value = await ensureDevAuth()
  } catch (caughtError) {
    error.value = `临时登录失败：${getRequestErrorMessage(caughtError)}`
    loading.value = false
    return
  }

  await fetchBeans()
}

async function fetchBeans() {
  loading.value = true
  error.value = ''

  try {
    const page = await listCoffeeBeans(buildQuery())

    if (filters.page > 1 && page.items.length === 0 && page.total > 0) {
      filters.page = Math.max(1, page.totalPages)
      applyPage(await listCoffeeBeans(buildQuery()))
      return
    }

    applyPage(page)
  } catch (caughtError) {
    error.value = getRequestErrorMessage(caughtError)
  } finally {
    loading.value = false
  }
}

function applyPage(page: PageResponse<CoffeeBeanListItem>) {
  beans.value = page.items ?? []
  filters.page = page.page
  filters.pageSize = page.pageSize
  pageState.total = page.total
  pageState.totalPages = page.totalPages
}

function buildQuery(): CoffeeBeanQuery {
  return {
    keyword: emptyToUndefined(filters.keyword),
    roastLevel: emptyToUndefined(filters.roastLevel),
    processMethod: emptyToUndefined(filters.processMethod),
    origin: emptyToUndefined(filters.origin),
    page: filters.page,
    pageSize: filters.pageSize,
  }
}

function applyFilters() {
  notice.value = ''
  filters.page = 1
  void fetchBeans()
}

function resetFilters() {
  notice.value = ''
  filters.keyword = ''
  filters.roastLevel = ''
  filters.processMethod = ''
  filters.origin = ''
  filters.page = 1
  filters.pageSize = 20
  void fetchBeans()
}

function changePage(page: number) {
  if (page < 1 || (effectiveTotalPages.value > 0 && page > effectiveTotalPages.value)) {
    return
  }

  filters.page = page
  void fetchBeans()
}

function changePageSize() {
  filters.page = 1
  void fetchBeans()
}

async function openReviewDialog(bean: CoffeeBeanListItem) {
  error.value = ''
  notice.value = ''
  selectedReviewBean.value = bean
  reviewQuery.page = 1
  resetReviewState()
  isReviewDialogOpen.value = true
  await fetchReviews()
}

async function fetchReviews() {
  if (!selectedReviewBean.value) {
    return
  }

  reviewLoading.value = true
  reviewError.value = ''

  try {
    const page = await listCoffeeReviews(selectedReviewBean.value.id, {
      page: reviewQuery.page,
      pageSize: reviewQuery.pageSize,
    })

    if (reviewQuery.page > 1 && page.items.length === 0 && page.total > 0) {
      reviewQuery.page = Math.max(1, page.totalPages)
      applyReviewPage(
        await listCoffeeReviews(selectedReviewBean.value.id, {
          page: reviewQuery.page,
          pageSize: reviewQuery.pageSize,
        }),
      )
      return
    }

    applyReviewPage(page)
  } catch (caughtError) {
    reviewError.value = getRequestErrorMessage(caughtError)
  } finally {
    reviewLoading.value = false
  }
}

function applyReviewPage(page: PageResponse<CoffeeReview>) {
  reviews.value = page.items ?? []
  reviewQuery.page = page.page
  reviewQuery.pageSize = page.pageSize
  reviewPageState.total = page.total
  reviewPageState.totalPages = page.totalPages
}

function changeReviewPage(page: number) {
  if (page < 1 || (effectiveReviewTotalPages.value > 0 && page > effectiveReviewTotalPages.value)) {
    return
  }

  reviewQuery.page = page
  void fetchReviews()
}

function startCreateReview() {
  resetReviewForm()
  reviewFormError.value = ''
  reviewNotice.value = ''
}

function startEditReview(review: CoffeeReview) {
  reviewMode.value = 'edit'
  editingReviewId.value = review.id
  reviewFormError.value = ''
  reviewNotice.value = ''
  fillReviewForm(review)
}

async function submitReviewForm() {
  if (!selectedReviewBean.value) {
    reviewFormError.value = '请先选择咖啡豆。'
    return
  }

  reviewFormError.value = ''
  reviewNotice.value = ''

  let payload: CoffeeReviewPayload

  try {
    payload = toReviewPayload()
  } catch (caughtError) {
    reviewFormError.value = getRequestErrorMessage(caughtError)
    return
  }

  reviewSaving.value = true

  try {
    if (reviewMode.value === 'edit' && editingReviewId.value !== null) {
      await updateCoffeeReview(editingReviewId.value, payload)
      reviewNotice.value = '评价已更新。'
    } else {
      await createCoffeeReview(selectedReviewBean.value.id, payload)
      reviewNotice.value = '评价已新增。'
    }

    resetReviewForm()
    await fetchReviews()
  } catch (caughtError) {
    reviewFormError.value = getRequestErrorMessage(caughtError)
  } finally {
    reviewSaving.value = false
  }
}

async function confirmDeleteReview(review: CoffeeReview) {
  const confirmed = window.confirm('确认删除这条评价吗？')

  if (!confirmed) {
    return
  }

  reviewError.value = ''
  reviewFormError.value = ''
  reviewNotice.value = ''
  reviewDeletingId.value = review.id

  try {
    await deleteCoffeeReview(review.id)

    if (editingReviewId.value === review.id) {
      resetReviewForm()
    }

    reviewNotice.value = '评价已删除。'
    await fetchReviews()
  } catch (caughtError) {
    reviewError.value = getRequestErrorMessage(caughtError)
  } finally {
    reviewDeletingId.value = null
  }
}

function closeReviewDialog() {
  if (reviewSaving.value || reviewDeletingId.value !== null) {
    return
  }

  isReviewDialogOpen.value = false
  selectedReviewBean.value = null
  reviews.value = []
  reviewPageState.total = 0
  reviewPageState.totalPages = 0
  resetReviewState()
}

function startCreate() {
  error.value = ''
  formError.value = ''
  resetCoverUploadState()
  notice.value = ''
  resetForm()
  isDialogOpen.value = true
}

async function startEdit(bean: CoffeeBeanListItem) {
  error.value = ''
  formError.value = ''
  resetCoverUploadState()
  notice.value = ''
  editingLoadingId.value = bean.id

  try {
    const detail = await getCoffeeBean(bean.id)
    editingId.value = bean.id
    formMode.value = 'edit'
    fillForm(detail)
    isDialogOpen.value = true
  } catch (caughtError) {
    error.value = getRequestErrorMessage(caughtError)
  } finally {
    editingLoadingId.value = null
  }
}

async function submitForm() {
  formError.value = ''
  notice.value = ''

  if (coverUploading.value) {
    formError.value = '封面仍在上传中，请上传完成后再保存。'
    return
  }

  let payload: CoffeeBeanPayload

  try {
    payload = toPayload()
  } catch (caughtError) {
    formError.value = getRequestErrorMessage(caughtError)
    return
  }

  saving.value = true

  try {
    const successMessage = formMode.value === 'edit' ? '咖啡豆已更新。' : '咖啡豆已新增。'

    if (formMode.value === 'edit' && editingId.value !== null) {
      await updateCoffeeBean(editingId.value, payload)
    } else {
      await createCoffeeBean(payload)
    }

    isDialogOpen.value = false
    resetForm()
    notice.value = successMessage
    await fetchBeans()
  } catch (caughtError) {
    formError.value = getRequestErrorMessage(caughtError)
  } finally {
    saving.value = false
  }
}

async function confirmDelete(bean: CoffeeBeanListItem) {
  const confirmed = window.confirm(`确认删除「${bean.name}」吗？`)

  if (!confirmed) {
    return
  }

  error.value = ''
  notice.value = ''
  deletingId.value = bean.id

  try {
    await deleteCoffeeBean(bean.id)

    if (editingId.value === bean.id) {
      closeDialog()
    }

    if (selectedReviewBean.value?.id === bean.id) {
      closeReviewDialog()
    }

    notice.value = '咖啡豆已删除。'
    await fetchBeans()
  } catch (caughtError) {
    error.value = getRequestErrorMessage(caughtError)
  } finally {
    deletingId.value = null
  }
}

function closeDialog() {
  if (saving.value || coverUploading.value) {
    return
  }

  isDialogOpen.value = false
  formError.value = ''
  resetForm()
}

function fillForm(detail: CoffeeBeanDetail) {
  Object.assign(form, {
    name: valueToString(detail.name),
    origin: valueToString(detail.origin),
    region: valueToString(detail.region),
    farm: valueToString(detail.farm),
    variety: valueToString(detail.variety),
    processMethod: valueToString(detail.processMethod),
    roastLevel: valueToString(detail.roastLevel),
    roaster: valueToString(detail.roaster),
    roastDate: valueToString(detail.roastDate),
    purchaseDate: valueToString(detail.purchaseDate),
    openDate: valueToString(detail.openDate),
    finishDate: valueToString(detail.finishDate),
    netWeightGrams: valueToString(detail.netWeightGrams),
    price: valueToString(detail.price),
    currency: valueToString(detail.currency || 'CNY'),
    status: valueToString(detail.status || 'UNOPENED'),
    coverImageUrl: valueToString(detail.coverImageUrl),
    notes: valueToString(detail.notes),
  })
}

function resetForm() {
  Object.assign(form, defaultForm)
  editingId.value = null
  formMode.value = 'create'
  resetCoverUploadState()
}

async function handleCoverFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]

  if (!file) {
    return
  }

  coverUploadError.value = ''
  coverUploadNotice.value = ''
  formError.value = ''

  if (file.type && !file.type.startsWith('image/')) {
    coverUploadError.value = '请选择图片文件。'
    input.value = ''
    return
  }

  coverUploading.value = true

  try {
    const uploaded = await uploadCoffeeCover(file)

    if (!uploaded.url) {
      throw new Error('上传成功，但后端未返回封面 URL。')
    }

    form.coverImageUrl = uploaded.url
    coverUploadNotice.value = '封面上传成功，已写入封面 URL。'
  } catch (caughtError) {
    coverUploadError.value = getRequestErrorMessage(caughtError)
  } finally {
    coverUploading.value = false
    input.value = ''
  }
}

function clearCoverImage() {
  form.coverImageUrl = ''
  resetCoverUploadState()
}

function resetCoverUploadState() {
  coverUploadError.value = ''
  coverUploadNotice.value = ''
}

function openCoverPreview(bean: CoffeeBeanListItem) {
  if (!bean.coverImageUrl) {
    return
  }

  previewCover.value = {
    name: bean.name,
    url: bean.coverImageUrl,
  }
}

function closeCoverPreview() {
  previewCover.value = null
}

function resetReviewState() {
  reviews.value = []
  reviewPageState.total = 0
  reviewPageState.totalPages = 0
  reviewError.value = ''
  reviewFormError.value = ''
  reviewNotice.value = ''
  resetReviewForm()
}

function resetReviewForm() {
  Object.assign(reviewForm, defaultReviewForm)
  editingReviewId.value = null
  reviewMode.value = 'create'
}

function fillReviewForm(review: CoffeeReview) {
  Object.assign(reviewForm, {
    overallRating: valueToString(review.overallRating),
    aromaRating: valueToString(review.aromaRating),
    acidityRating: valueToString(review.acidityRating),
    sweetnessRating: valueToString(review.sweetnessRating),
    bitternessRating: valueToString(review.bitternessRating),
    bodyRating: valueToString(review.bodyRating),
    aftertasteRating: valueToString(review.aftertasteRating),
    content: valueToString(review.content),
  })
}

function toPayload(): CoffeeBeanPayload {
  const name = form.name.trim()

  if (!name) {
    throw new Error('咖啡豆名称不能为空。')
  }

  return {
    name,
    origin: emptyToNull(form.origin),
    region: emptyToNull(form.region),
    farm: emptyToNull(form.farm),
    variety: emptyToNull(form.variety),
    processMethod: emptyToNull(form.processMethod),
    roastLevel: emptyToNull(form.roastLevel),
    roaster: emptyToNull(form.roaster),
    roastDate: emptyToNull(form.roastDate),
    purchaseDate: emptyToNull(form.purchaseDate),
    openDate: emptyToNull(form.openDate),
    finishDate: emptyToNull(form.finishDate),
    netWeightGrams: positiveNumberOrNull(form.netWeightGrams, '净含量'),
    price: nonNegativeNumberOrNull(form.price, '价格'),
    currency: emptyToNull(form.currency),
    status: emptyToNull(form.status),
    coverImageUrl: emptyToNull(form.coverImageUrl),
    notes: emptyToNull(form.notes),
  }
}

function toReviewPayload(): CoffeeReviewPayload {
  return {
    overallRating: requiredRatingNumber(reviewForm.overallRating, '综合评分'),
    aromaRating: optionalRatingNumber(reviewForm.aromaRating, '香气评分'),
    acidityRating: optionalRatingNumber(reviewForm.acidityRating, '酸度评分'),
    sweetnessRating: optionalRatingNumber(reviewForm.sweetnessRating, '甜感评分'),
    bitternessRating: optionalRatingNumber(reviewForm.bitternessRating, '苦感评分'),
    bodyRating: optionalRatingNumber(reviewForm.bodyRating, '醇厚度评分'),
    aftertasteRating: optionalRatingNumber(reviewForm.aftertasteRating, '余韵评分'),
    content: emptyToNull(reviewForm.content),
  }
}

function emptyToNull(value: string) {
  const trimmed = value.trim()
  return trimmed === '' ? null : trimmed
}

function emptyToUndefined(value: string) {
  const trimmed = value.trim()
  return trimmed === '' ? undefined : trimmed
}

function valueToString(value: string | number | null | undefined) {
  return value === null || value === undefined ? '' : String(value)
}

function positiveNumberOrNull(value: string | number, label: string) {
  const parsed = parseOptionalNumber(value, label)

  if (parsed !== null && parsed <= 0) {
    throw new Error(`${label}必须大于 0。`)
  }

  return parsed
}

function nonNegativeNumberOrNull(value: string | number, label: string) {
  const parsed = parseOptionalNumber(value, label)

  if (parsed !== null && parsed < 0) {
    throw new Error(`${label}不能小于 0。`)
  }

  return parsed
}

function parseOptionalNumber(value: string | number, label: string) {
  const trimmed = String(value).trim()

  if (!trimmed) {
    return null
  }

  const parsed = Number(trimmed)

  if (!Number.isFinite(parsed)) {
    throw new Error(`${label}必须是数字。`)
  }

  return parsed
}

function requiredRatingNumber(value: string | number, label: string) {
  const parsed = parseOptionalNumber(value, label)

  if (parsed === null) {
    throw new Error(`${label}不能为空。`)
  }

  assertRatingRange(parsed, label)
  return parsed
}

function optionalRatingNumber(value: string | number, label: string) {
  const parsed = parseOptionalNumber(value, label)

  if (parsed === null) {
    return null
  }

  assertRatingRange(parsed, label)
  return parsed
}

function assertRatingRange(value: number, label: string) {
  if (value < 0 || value > 5) {
    throw new Error(`${label}必须在 0 到 5 之间。`)
  }

  if (!Number.isInteger(value * 2)) {
    throw new Error(`${label}必须以 0.5 为步进。`)
  }
}

function display(value: string | number | null | undefined) {
  return value === null || value === undefined || value === '' ? '-' : value
}
</script>

<template>
  <main class="coffee-page">
    <section class="page-hero">
      <div>
        <p class="eyebrow">Coffee Beans</p>
        <h1>咖啡豆档案</h1>
        <p class="subtitle">基础信息、包装链接和入库状态。</p>
      </div>
      <div class="hero-actions">
        <button type="button" @click="startCreate">新增咖啡豆</button>
        <div class="user-status">
          <span>当前用户</span>
          <strong>{{ currentUser?.nickname || currentUser?.username || '连接中' }}</strong>
        </div>
      </div>
    </section>

    <section class="filter-bar" aria-label="咖啡豆筛选">
      <form class="filter-form" @submit.prevent="applyFilters">
        <label class="field">
          <span>关键词</span>
          <input v-model="filters.keyword" type="search" placeholder="名称 / 烘焙商 / 产地" />
        </label>

        <label class="field">
          <span>烘焙度</span>
          <input v-model="filters.roastLevel" type="text" placeholder="LIGHT / MEDIUM" />
        </label>

        <label class="field">
          <span>处理法</span>
          <input v-model="filters.processMethod" type="text" placeholder="WASHED / NATURAL" />
        </label>

        <label class="field">
          <span>产地</span>
          <input v-model="filters.origin" type="text" placeholder="Ethiopia" />
        </label>

        <div class="filter-actions">
          <button type="submit" :disabled="loading">查询</button>
          <button type="button" class="secondary" :disabled="loading" @click="resetFilters">
            重置
          </button>
          <button type="button" class="secondary" :disabled="loading" @click="fetchBeans">
            刷新
          </button>
        </div>
      </form>
    </section>

    <section class="list-panel">
      <div class="section-heading">
        <div>
          <h2>咖啡豆列表</h2>
          <p>{{ pageState.total }} 条记录</p>
        </div>

        <label class="page-size">
          <span>每页</span>
          <select v-model.number="filters.pageSize" :disabled="loading" @change="changePageSize">
            <option :value="10">10</option>
            <option :value="20">20</option>
            <option :value="50">50</option>
          </select>
        </label>
      </div>

      <p v-if="error" class="alert error">{{ error }}</p>
      <p v-if="notice" class="alert success">{{ notice }}</p>

      <div v-if="loading" class="state-box">正在加载咖啡豆列表...</div>
      <div v-else-if="!hasRows" class="state-box empty">暂无咖啡豆数据。</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>名称</th>
              <th>产地</th>
              <th>处理法</th>
              <th>烘焙度</th>
              <th>烘焙日期</th>
              <th>状态</th>
              <th>封面</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="bean in beans" :key="bean.id">
              <td>
                <strong>{{ bean.name }}</strong>
                <span>{{ display(bean.roaster) }}</span>
              </td>
              <td>
                {{ display(bean.origin) }}
                <span>{{ display(bean.region) }}</span>
              </td>
              <td>{{ display(bean.processMethod) }}</td>
              <td>{{ display(bean.roastLevel) }}</td>
              <td>{{ display(bean.roastDate) }}</td>
              <td>{{ display(bean.status) }}</td>
              <td>
                <button
                  v-if="bean.coverImageUrl"
                  type="button"
                  class="cover-thumb-button"
                  :aria-label="`查看 ${bean.name} 封面大图`"
                  @click="openCoverPreview(bean)"
                >
                  <img class="cover-thumb" :src="bean.coverImageUrl" :alt="`${bean.name} 封面`" />
                </button>
                <span v-else>-</span>
              </td>
              <td>
                <div class="row-actions">
                  <button
                    type="button"
                    class="secondary"
                    :disabled="saving || deletingId !== null || editingLoadingId !== null"
                    @click="openReviewDialog(bean)"
                  >
                    评价
                  </button>
                  <button
                    type="button"
                    class="secondary"
                    :disabled="saving || deletingId !== null || editingLoadingId !== null"
                    @click="startEdit(bean)"
                  >
                    {{ editingLoadingId === bean.id ? '加载中' : '编辑' }}
                  </button>
                  <button
                    type="button"
                    class="danger"
                    :disabled="deletingId === bean.id"
                    @click="confirmDelete(bean)"
                  >
                    {{ deletingId === bean.id ? '删除中' : '删除' }}
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div v-if="pageState.total > 0" class="pagination">
        <button type="button" class="secondary" :disabled="!canGoPrevious" @click="changePage(filters.page - 1)">
          上一页
        </button>
        <span>第 {{ filters.page }} / {{ effectiveTotalPages }} 页</span>
        <button type="button" class="secondary" :disabled="!canGoNext" @click="changePage(filters.page + 1)">
          下一页
        </button>
      </div>
    </section>

    <div v-if="isDialogOpen" class="dialog-backdrop" role="presentation" @click.self="closeDialog">
      <section
        class="dialog-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="coffee-dialog-title"
        aria-describedby="coffee-dialog-description"
      >
        <header class="dialog-header">
          <div>
            <h2 id="coffee-dialog-title">{{ formTitle }}</h2>
            <p id="coffee-dialog-description">{{ formDescription }}</p>
          </div>
          <button
            type="button"
            class="icon-button"
            :disabled="saving || coverUploading"
            aria-label="关闭"
            @click="closeDialog"
          >
            ×
          </button>
        </header>

        <form class="bean-form" @submit.prevent="submitForm">
          <p v-if="formError" class="alert error form-alert">{{ formError }}</p>

          <label class="field wide">
            <span>名称 *</span>
            <input v-model.trim="form.name" type="text" maxlength="128" required />
          </label>

          <label class="field">
            <span>产地</span>
            <input v-model.trim="form.origin" type="text" maxlength="128" />
          </label>

          <label class="field">
            <span>产区</span>
            <input v-model.trim="form.region" type="text" maxlength="128" />
          </label>

          <label class="field">
            <span>庄园 / 农场</span>
            <input v-model.trim="form.farm" type="text" maxlength="128" />
          </label>

          <label class="field">
            <span>品种</span>
            <input v-model.trim="form.variety" type="text" maxlength="128" />
          </label>

          <label class="field">
            <span>处理法</span>
            <input v-model.trim="form.processMethod" type="text" maxlength="64" />
          </label>

          <label class="field">
            <span>烘焙度</span>
            <input v-model.trim="form.roastLevel" type="text" maxlength="64" />
          </label>

          <label class="field">
            <span>烘焙商</span>
            <input v-model.trim="form.roaster" type="text" maxlength="128" />
          </label>

          <label class="field">
            <span>净含量 g</span>
            <input v-model.trim="form.netWeightGrams" type="number" min="0.01" step="0.01" />
          </label>

          <label class="field">
            <span>价格</span>
            <input v-model.trim="form.price" type="number" min="0" step="0.01" />
          </label>

          <label class="field">
            <span>币种</span>
            <input v-model.trim="form.currency" type="text" maxlength="16" />
          </label>

          <label class="field">
            <span>状态</span>
            <select v-model="form.status">
              <option value="UNOPENED">UNOPENED</option>
              <option value="OPENED">OPENED</option>
              <option value="FINISHED">FINISHED</option>
            </select>
          </label>

          <label class="field">
            <span>烘焙日期</span>
            <input v-model="form.roastDate" type="date" />
          </label>

          <label class="field">
            <span>购买日期</span>
            <input v-model="form.purchaseDate" type="date" />
          </label>

          <label class="field">
            <span>开封日期</span>
            <input v-model="form.openDate" type="date" />
          </label>

          <label class="field">
            <span>喝完日期</span>
            <input v-model="form.finishDate" type="date" />
          </label>

          <div class="cover-upload wide">
            <div class="cover-upload-header">
              <span>封面图片</span>
              <button
                v-if="form.coverImageUrl"
                type="button"
                class="secondary compact-button"
                :disabled="saving || coverUploading"
                @click="clearCoverImage"
              >
                清空
              </button>
            </div>

            <div class="cover-upload-body">
              <div class="cover-preview" :class="{ empty: !form.coverImageUrl }">
                <img v-if="form.coverImageUrl" :src="form.coverImageUrl" alt="咖啡豆封面预览" />
                <span v-else>暂无封面</span>
              </div>

              <div class="cover-upload-controls">
                <input
                  type="file"
                  accept="image/*"
                  :disabled="saving || coverUploading"
                  @change="handleCoverFileChange"
                />
                <p v-if="coverUploading" class="upload-status">封面上传中...</p>
                <p v-else class="upload-hint">上传成功后会自动写入封面 URL。</p>
                <p v-if="coverUploadError" class="inline-error">{{ coverUploadError }}</p>
                <p v-if="coverUploadNotice" class="inline-success">{{ coverUploadNotice }}</p>
              </div>
            </div>

            <label class="field cover-url-field">
              <span>封面 URL</span>
              <input v-model.trim="form.coverImageUrl" type="text" maxlength="500" />
            </label>
          </div>

          <label class="field wide">
            <span>备注</span>
            <textarea v-model.trim="form.notes" rows="4"></textarea>
          </label>

          <div class="form-actions">
            <button type="button" class="secondary" :disabled="saving || coverUploading" @click="closeDialog">
              取消
            </button>
            <button type="submit" :disabled="saving || coverUploading">{{ submitText }}</button>
          </div>
        </form>
      </section>
    </div>

    <div
      v-if="isReviewDialogOpen"
      class="dialog-backdrop"
      role="presentation"
      @click.self="closeReviewDialog"
    >
      <section
        class="dialog-panel review-dialog-panel"
        role="dialog"
        aria-modal="true"
        aria-labelledby="review-dialog-title"
        aria-describedby="review-dialog-description"
      >
        <header class="dialog-header">
          <div>
            <h2 id="review-dialog-title">{{ reviewDialogTitle }}</h2>
            <p id="review-dialog-description">{{ reviewPageState.total }} 条评价</p>
          </div>
          <button
            type="button"
            class="icon-button"
            :disabled="reviewSaving || reviewDeletingId !== null"
            aria-label="关闭评价"
            @click="closeReviewDialog"
          >
            ×
          </button>
        </header>

        <div class="review-layout">
          <section class="review-form-panel">
            <h3>{{ reviewFormTitle }}</h3>

            <form class="review-form" @submit.prevent="submitReviewForm">
              <p v-if="reviewFormError" class="alert error form-alert">{{ reviewFormError }}</p>

              <div class="review-rating-fields">
                <label v-for="field in reviewRatingFields" :key="field.key" class="field">
                  <span>{{ field.label }}</span>
                  <input
                    v-model.trim="reviewForm[field.key]"
                    type="number"
                    min="0"
                    max="5"
                    step="0.5"
                    :required="field.required"
                  />
                </label>
              </div>

              <label class="field">
                <span>文本评价</span>
                <textarea v-model.trim="reviewForm.content" rows="5" maxlength="2000"></textarea>
              </label>

              <div class="form-actions">
                <button type="button" class="secondary" :disabled="reviewSaving" @click="startCreateReview">
                  {{ reviewMode === 'edit' ? '取消编辑' : '清空' }}
                </button>
                <button type="submit" :disabled="reviewSaving">{{ reviewSubmitText }}</button>
              </div>
            </form>
          </section>

          <section class="review-list-panel">
            <div class="review-list-header">
              <h3>评价列表</h3>
              <button type="button" class="secondary compact-button" :disabled="reviewLoading" @click="fetchReviews">
                刷新
              </button>
            </div>

            <p v-if="reviewError" class="alert error">{{ reviewError }}</p>
            <p v-if="reviewNotice" class="alert success">{{ reviewNotice }}</p>

            <div v-if="reviewLoading" class="state-box review-state">正在加载评价...</div>
            <div v-else-if="!hasReviews" class="state-box empty review-state">暂无评价。</div>
            <div v-else class="review-list">
              <article v-for="review in reviews" :key="review.id" class="review-card">
                <header class="review-card-header">
                  <div>
                    <strong>综合 {{ display(review.overallRating) }}</strong>
                    <span>{{ display(review.createdAt) }}</span>
                  </div>
                  <div class="row-actions">
                    <button
                      type="button"
                      class="secondary compact-button"
                      :disabled="reviewSaving || reviewDeletingId !== null"
                      @click="startEditReview(review)"
                    >
                      编辑
                    </button>
                    <button
                      type="button"
                      class="danger compact-button"
                      :disabled="reviewDeletingId === review.id"
                      @click="confirmDeleteReview(review)"
                    >
                      {{ reviewDeletingId === review.id ? '删除中' : '删除' }}
                    </button>
                  </div>
                </header>

                <dl class="review-rating-list">
                  <div v-for="field in reviewDimensionFields" :key="field.key">
                    <dt>{{ field.label.replace('评分', '') }}</dt>
                    <dd>{{ display(review[field.key]) }}</dd>
                  </div>
                </dl>

                <p class="review-content">{{ display(review.content) }}</p>
              </article>
            </div>

            <div v-if="reviewPageState.total > 0" class="pagination review-pagination">
              <button
                type="button"
                class="secondary"
                :disabled="!canReviewGoPrevious"
                @click="changeReviewPage(reviewQuery.page - 1)"
              >
                上一页
              </button>
              <span>第 {{ reviewQuery.page }} / {{ effectiveReviewTotalPages }} 页</span>
              <button
                type="button"
                class="secondary"
                :disabled="!canReviewGoNext"
                @click="changeReviewPage(reviewQuery.page + 1)"
              >
                下一页
              </button>
            </div>
          </section>
        </div>
      </section>
    </div>

    <div
      v-if="previewCover"
      class="image-viewer-backdrop"
      role="presentation"
      @click.self="closeCoverPreview"
    >
      <section class="image-viewer-panel" role="dialog" aria-modal="true" aria-labelledby="cover-viewer-title">
        <header class="image-viewer-header">
          <div>
            <h2 id="cover-viewer-title">{{ previewCover.name }}</h2>
            <p>{{ previewCover.url }}</p>
          </div>
          <button type="button" class="icon-button" aria-label="关闭大图" @click="closeCoverPreview">×</button>
        </header>

        <img class="image-viewer-img" :src="previewCover.url" :alt="`${previewCover.name} 封面大图`" />
      </section>
    </div>
  </main>
</template>
