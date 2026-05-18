import { apiClient } from "@/shared/api/apiClient"
import type { Readiness, TacticalAlert, TacticalDashboard } from "@/features/dashboard/model/dashboardTypes"

export const dashboardApi = {
  dashboard: () => apiClient<TacticalDashboard>("/api/dashboard"),
  readiness: () => apiClient<Readiness>("/api/dashboard/readiness"),
  alerts: () => apiClient<TacticalAlert[]>("/api/alerts"),
}
