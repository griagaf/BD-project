import { useQuery } from "@tanstack/react-query"
import { dashboardApi } from "@/features/dashboard/api/dashboardApi"

export function useDashboardQuery() {
  return useQuery({
    queryKey: ["dashboard"],
    queryFn: dashboardApi.dashboard,
  })
}

export function useAlertsQuery() {
  return useQuery({
    queryKey: ["alerts"],
    queryFn: dashboardApi.alerts,
  })
}
