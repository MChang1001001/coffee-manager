import { request } from './request'
import type { PageResponse } from './coffee'

export interface BrewRecordQuery {
  page: number
  pageSize: number
}

export interface BrewRecord {
  id: number
  coffeeBeanId: number
  brewMethod: string
  beanAmountGrams: number | string | null
  waterAmountMl: number | string | null
  ratio: string | null
  waterTemperature: number | string | null
  grindSize: string | null
  brewTimeSeconds: number | null
  resultSummary: string | null
  resultNotes: string | null
  isRecommended: boolean | null
  createdAt: string | null
  updatedAt: string | null
}

export interface BrewRecordPayload {
  brewMethod: string
  beanAmountGrams: number | null
  waterAmountMl: number | null
  ratio: string | null
  waterTemperature: number | null
  grindSize: string | null
  brewTimeSeconds: number | null
  resultSummary: string | null
  resultNotes: string | null
  isRecommended: boolean
}

export interface BrewRecordIdResponse {
  id: number
}

const coffeeBeanPath = '/coffee-beans'
const brewRecordPath = '/brew-records'

export function listBrewRecords(coffeeBeanId: number, query: BrewRecordQuery) {
  return request<PageResponse<BrewRecord>>({
    url: `${coffeeBeanPath}/${coffeeBeanId}/brew-records`,
    method: 'GET',
    params: query,
  })
}

export function createBrewRecord(coffeeBeanId: number, payload: BrewRecordPayload) {
  return request<BrewRecordIdResponse>({
    url: `${coffeeBeanPath}/${coffeeBeanId}/brew-records`,
    method: 'POST',
    data: payload,
  })
}

export function getBrewRecord(id: number) {
  return request<BrewRecord>({
    url: `${brewRecordPath}/${id}`,
    method: 'GET',
  })
}

export function updateBrewRecord(id: number, payload: BrewRecordPayload) {
  return request<boolean>({
    url: `${brewRecordPath}/${id}`,
    method: 'PUT',
    data: payload,
  })
}

export function deleteBrewRecord(id: number) {
  return request<boolean>({
    url: `${brewRecordPath}/${id}`,
    method: 'DELETE',
  })
}
