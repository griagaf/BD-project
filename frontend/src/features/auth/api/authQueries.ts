import { useQuery } from "@tanstack/react-query"
import { authApi } from "@/features/auth/api/authApi"

export function useApiStatusQuery() {
  return useQuery({
    queryKey: ["system", "status"],
    queryFn: authApi.status,
  })
}

