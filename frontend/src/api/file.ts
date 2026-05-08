import { request } from './request'

export interface CoffeeCoverUploadResponse {
  url: string
  filename: string
  originalFilename: string
  contentType: string
  size: number
}

export function uploadCoffeeCover(file: File) {
  const formData = new FormData()
  formData.append('file', file)

  return request<CoffeeCoverUploadResponse>({
    url: '/files/coffee-cover',
    method: 'POST',
    data: formData,
  })
}
