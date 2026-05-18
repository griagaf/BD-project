import { apiClient } from "@/shared/api/apiClient"
import { useAuthStore } from "@/features/auth/model/authStore"
import type { ExecuteQueryRequest, QueryResult, QueryTemplateMetadata } from "@/features/intelligence/model/intelligenceTypes"

export const intelligenceApi = {
  templates: () => apiClient<QueryTemplateMetadata[]>("/api/intelligence/templates"),
  execute: (code: string, request: ExecuteQueryRequest) =>
    apiClient<QueryResult>(`/api/intelligence/queries/${code}/execute`, {
      method: "POST",
      body: JSON.stringify(request),
    }),
  exportCsv: async (code: string, request: ExecuteQueryRequest) => {
    const { accessToken, simulationRole, simulationObjectType, simulationObjectId } = useAuthStore.getState()
    const response = await fetch(`${import.meta.env.VITE_API_URL ?? "http://localhost:8080"}/api/intelligence/queries/${code}/export`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
        ...(simulationRole ? { "X-Access-Simulation-Role": simulationRole } : {}),
        ...(simulationObjectType ? { "X-Access-Simulation-Object-Type": simulationObjectType } : {}),
        ...(simulationObjectId ? { "X-Access-Simulation-Object-Id": String(simulationObjectId) } : {}),
      },
      body: JSON.stringify(request),
    })
    if (!response.ok) {
      throw new Error("CSV export failed")
    }
    return response.blob()
  },
}
