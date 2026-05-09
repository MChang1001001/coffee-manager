import { request } from './request'
import type { PageResponse } from './coffee'

export interface CoffeeReviewQuery {
  page: number
  pageSize: number
}

export interface CoffeeReview {
  id: number
  coffeeBeanId: number
  overallRating: number | string | null
  aromaRating: number | string | null
  acidityRating: number | string | null
  sweetnessRating: number | string | null
  bitternessRating: number | string | null
  bodyRating: number | string | null
  aftertasteRating: number | string | null
  content: string | null
  createdAt: string | null
  updatedAt: string | null
}

export interface CoffeeReviewPayload {
  overallRating: number
  aromaRating: number | null
  acidityRating: number | null
  sweetnessRating: number | null
  bitternessRating: number | null
  bodyRating: number | null
  aftertasteRating: number | null
  content: string | null
}

export interface CoffeeReviewIdResponse {
  id: number
}

const coffeeBeanPath = '/coffee-beans'
const reviewPath = '/reviews'

export function listCoffeeReviews(coffeeBeanId: number, query: CoffeeReviewQuery) {
  return request<PageResponse<CoffeeReview>>({
    url: `${coffeeBeanPath}/${coffeeBeanId}/reviews`,
    method: 'GET',
    params: query,
  })
}

export function createCoffeeReview(coffeeBeanId: number, payload: CoffeeReviewPayload) {
  return request<CoffeeReviewIdResponse>({
    url: `${coffeeBeanPath}/${coffeeBeanId}/reviews`,
    method: 'POST',
    data: payload,
  })
}

export function getCoffeeReview(id: number) {
  return request<CoffeeReview>({
    url: `${reviewPath}/${id}`,
    method: 'GET',
  })
}

export function updateCoffeeReview(id: number, payload: CoffeeReviewPayload) {
  return request<boolean>({
    url: `${reviewPath}/${id}`,
    method: 'PUT',
    data: payload,
  })
}

export function deleteCoffeeReview(id: number) {
  return request<boolean>({
    url: `${reviewPath}/${id}`,
    method: 'DELETE',
  })
}
