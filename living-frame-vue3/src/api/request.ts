import axios, { type AxiosRequestConfig } from 'axios'
import JSONbigFactory from 'json-bigint'
import { ElMessage } from 'element-plus'
import { getToken, removeToken } from '@/utils/storage'
import type { ApiResponse } from '@/types/api'

const JSONbig = JSONbigFactory({ storeAsString: true })

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 60_000,
  transformResponse: [
    (data: unknown) => {
      if (typeof data !== 'string' || !data) {
        return data
      }
      try {
        return JSONbig.parse(data)
      } catch {
        return JSON.parse(data)
      }
    },
  ],
})

service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

service.interceptors.response.use(
  (response) => {
    const body = response.data as ApiResponse<unknown>
    if (body && typeof body.code === 'number' && body.code !== 200) {
      ElMessage.error(body.message || '请求失败')
      return Promise.reject(new Error(body.message || '请求失败'))
    }
    return response
  },
  (error) => {
    const status = error?.response?.status as number | undefined
    const body = error?.response?.data as ApiResponse<unknown> | undefined
    if (status === 401) {
      removeToken()
      ElMessage.error('登录已过期，请重新登录')
      if (!window.location.pathname.startsWith('/login')) {
        window.location.replace('/login')
      }
    } else {
      ElMessage.error(body?.message || error?.message || '网络请求失败')
    }
    return Promise.reject(error)
  },
)

export const http = {
  get<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.get<ApiResponse<T>>(url, config).then((response) => response.data.data)
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.post<ApiResponse<T>>(url, data, config).then((response) => response.data.data)
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return service.put<ApiResponse<T>>(url, data, config).then((response) => response.data.data)
  },
  delete<T>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return service.delete<ApiResponse<T>>(url, config).then((response) => response.data.data)
  },
}
