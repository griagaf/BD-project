import { apiClient } from "@/shared/api/apiClient"
import type { CurrentUser } from "@/features/auth/model/authTypes"

export type ApiStatusResponse = {
  application?: string
  status: string
  message?: string
  timestamp?: string
}

export const authApi = {
  status: () => apiClient<ApiStatusResponse>("/api/system/status", { skipAuth: true }),
  me: () => apiClient<CurrentUser>("/api/auth/me"),
}
