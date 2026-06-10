import { request } from './request'

export interface PageResponse<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
  totalPages: number
}

export interface CoffeeBeanQuery {
  keyword?: string
  roastLevel?: string
  processMethod?: string
  origin?: string
  drinkStatus?: string
  status?: string
  page: number
  pageSize: number
}

export interface CoffeeBeanListItem {
  id: number
  name: string
  origin: string | null
  region: string | null
  variety: string | null
  processMethod: string | null
  roastLevel: string | null
  roaster: string | null
  roastDate: string | null
  bestFromDate: string | null
  bestToDate: string | null
  purchaseDate: string | null
  status: string | null
  coverImageUrl: string | null
  overallRating: number | string | null
  reviewCount: number | null
  brewCount: number | null
  summaryTitle: string | null
  createdAt: string | null
}

export interface CoffeeBeanPayload {
  name: string
  origin: string | null
  region: string | null
  farm: string | null
  variety: string | null
  processMethod: string | null
  roastLevel: string | null
  roaster: string | null
  roastDate: string | null
  bestFromDate: string | null
  bestToDate: string | null
  purchaseDate: string | null
  openDate: string | null
  finishDate: string | null
  netWeightGrams: number | null
  price: number | null
  currency: string | null
  status: string | null
  coverImageUrl: string | null
  notes: string | null
}

export interface CoffeeBeanDetail extends CoffeeBeanPayload {
  id: number
  overallRating: number | string | null
  reviewCount: number | null
  brewCount: number | null
  summaryTitle: string | null
  flavorSummary: string | null
  brewSuggestion: string | null
  repurchaseIntention: string | null
  summaryText: string | null
  summarySource: string | null
  summaryGeneratedAt: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface CoffeeSummaryPayload {
  summaryTitle: string | null
  flavorSummary: string | null
  brewSuggestion: string | null
  repurchaseIntention: string | null
  summaryText: string | null
  summarySource?: string | null
  summaryGeneratedAt?: string | null
}

export interface CoffeeSummaryDraft extends CoffeeSummaryPayload {
  summarySource: string | null
  summaryGeneratedAt: string | null
}

export interface CoffeeBeanIdResponse {
  id: number
}

export interface CoffeeEnumOption {
  label: string
  value: string
}

export interface CoffeeEnumOptions {
  roastLevels: CoffeeEnumOption[]
  processMethods: CoffeeEnumOption[]
  origins: CoffeeEnumOption[]
  varieties: CoffeeEnumOption[]
}

const coffeeBeanPath = '/coffee-beans'
const coffeeEnumsPath = '/enums/coffee'

export function getCoffeeEnums() {
  return request<CoffeeEnumOptions>({
    url: coffeeEnumsPath,
    method: 'GET',
  })
}

export function listCoffeeBeans(query: CoffeeBeanQuery) {
  return request<PageResponse<CoffeeBeanListItem>>({
    url: coffeeBeanPath,
    method: 'GET',
    params: cleanObject(query),
  })
}

export function getCoffeeBean(id: number) {
  return request<CoffeeBeanDetail>({
    url: `${coffeeBeanPath}/${id}`,
    method: 'GET',
  })
}

export function createCoffeeBean(payload: CoffeeBeanPayload) {
  return request<CoffeeBeanIdResponse>({
    url: coffeeBeanPath,
    method: 'POST',
    data: payload,
  })
}

export function updateCoffeeBean(id: number, payload: CoffeeBeanPayload) {
  return request<boolean>({
    url: `${coffeeBeanPath}/${id}`,
    method: 'PUT',
    data: payload,
  })
}

export function generateCoffeeAiSummary(id: number) {
  return request<CoffeeSummaryDraft>({
    url: `${coffeeBeanPath}/${id}/ai-summary`,
    method: 'POST',
  })
}

export function updateCoffeeSummary(id: number, payload: CoffeeSummaryPayload) {
  return request<boolean>({
    url: `${coffeeBeanPath}/${id}/summary`,
    method: 'PUT',
    data: payload,
  })
}

export function deleteCoffeeBean(id: number) {
  return request<boolean>({
    url: `${coffeeBeanPath}/${id}`,
    method: 'DELETE',
  })
}

function cleanObject(source: object) {
  const result: Record<string, unknown> = {}

  Object.entries(source).forEach(([key, value]) => {
    if (value !== '' && value !== null && value !== undefined) {
      result[key] = value
    }
  })

  return result
}
