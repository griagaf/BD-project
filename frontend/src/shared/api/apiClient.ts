import { useAuthStore } from "@/features/auth/model/authStore"
import { ApiError } from "@/shared/api/apiError"

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080"

type RequestOptions = RequestInit & {
  skipAuth?: boolean
}

export async function apiClient<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { accessToken, clearTokens } = useAuthStore.getState()

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(accessToken && !options.skipAuth ? { Authorization: `Bearer ${accessToken}` } : {}),
      ...options.headers,
    },
  })

  if (response.status === 401) {
    clearTokens()
    throw new ApiError(401, "UNAUTHORIZED", "Authentication is required")
  }

  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    throw ApiError.fromResponse(response.status, payload)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

