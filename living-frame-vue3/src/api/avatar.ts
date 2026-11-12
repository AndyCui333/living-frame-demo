import { http } from './request'

export function uploadAvatar(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return http.post<string>('/system/files/avatar', formData)
}
