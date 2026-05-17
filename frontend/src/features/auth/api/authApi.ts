import { apiClient } from "@/shared/api/apiClient"
import type { CurrentUser, LoginRequest, TokenResponse } from "@/features/auth/model/authTypes"

export type ApiStatusResponse = {
  application?: string
  status: string
  message?: string
  timestamp?: string
}

export const authApi = {
  status: () => apiClient<ApiStatusResponse>("/api/system/status", { skipAuth: true }),
  login: (request: LoginRequest) =>
    apiClient<TokenResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(request),
      skipAuth: true,
    }),
  me: () => apiClient<CurrentUser>("/api/auth/me"),
  simulationPreview: (request: { role: string; objectType: string; objectId: number }) =>
    apiClient<CurrentUser>("/api/auth/simulation/preview", {
      method: "POST",
      body: JSON.stringify(request),
    }),
}
