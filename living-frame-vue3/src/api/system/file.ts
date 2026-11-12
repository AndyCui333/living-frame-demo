import { http } from '../request'
import type { MinioFileConfig, MinioObject } from '@/types/api'

export function getFileConfig(): Promise<MinioFileConfig> {
  return http.get<MinioFileConfig>('/system/files/config')
}

export function getBuckets(): Promise<string[]> {
  return http.get<string[]>('/system/files/buckets')
}

export function createBucket(bucketName: string): Promise<void> {
  return http.post<void>('/system/files/buckets', { bucketName })
}

export function deleteBucket(bucket: string): Promise<void> {
  return http.delete<void>(`/system/files/buckets/${encodeURIComponent(bucket)}`)
}

export function listFiles(bucket: string, prefix = ''): Promise<MinioObject[]> {
  return http.get<MinioObject[]>('/system/files/objects', { params: { bucket, prefix } })
}

export function uploadFile(bucket: string, prefix: string, file: File): Promise<string> {
  const formData = new FormData()
  formData.append('bucket', bucket)
  formData.append('prefix', prefix)
  formData.append('file', file)
  return http.post<string>('/system/files/upload', formData, { timeout: 300_000 })
}

export function getDownloadUrl(bucket: string, objectName: string): Promise<string> {
  return http
    .get<{ url: string }>('/system/files/download', { params: { bucket, objectName } })
    .then((result) => result.url)
}

export function deleteFile(bucket: string, objectName: string): Promise<void> {
  return http.delete<void>('/system/files/objects', { params: { bucket, objectName } })
}
