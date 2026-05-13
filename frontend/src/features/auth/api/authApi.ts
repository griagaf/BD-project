import { apiClient } from "@/shared/api/apiClient"

export type ApiStatusResponse = {
  application?: string
  status: string
  message?: string
  timestamp?: string
}

export const authApi = {
  status: () => apiClient<ApiStatusResponse>("/api/system/status", { skipAuth: true }),
}

