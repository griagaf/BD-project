import { useQuery } from "@tanstack/react-query"
import { dashboardApi } from "@/features/dashboard/api/dashboardApi"

export function useDashboardQuery() {
  return useQuery({
    queryKey: ["dashboard"],
    queryFn: dashboardApi.dashboard,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  })
}

export function useAlertsQuery() {
  return useQuery({
    queryKey: ["alerts"],
    queryFn: dashboardApi.alerts,
    staleTime: 30_000,
    refetchOnWindowFocus: false,
  })
}
