import { useAuthStore } from "@/features/auth/model/authStore"
import type { SmartMissionReport, SmartMissionReportRequest } from "@/features/report/model/reportTypes"
import { apiClient } from "@/shared/api/apiClient"

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080"

export const reportApi = {
  generate: (request: SmartMissionReportRequest) =>
    apiClient<SmartMissionReport>("/api/reports/smart-mission/generate", {
      method: "POST",
      body: JSON.stringify(request),
    }),
  exportCsv: async (request: SmartMissionReportRequest) => {
    const { accessToken } = useAuthStore.getState()
    const response = await fetch(`${API_URL}/api/reports/smart-mission/export.csv`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(accessToken ? { Authorization: `Bearer ${accessToken}` } : {}),
      },
      body: JSON.stringify(request),
    })

    if (!response.ok) {
      throw new Error("Smart mission report CSV export failed")
    }

    return response.blob()
  },
}
