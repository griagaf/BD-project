import { useMutation, useQuery } from "@tanstack/react-query"
import { authApi } from "@/features/auth/api/authApi"
import { useAuthStore } from "@/features/auth/model/authStore"
import type { LoginRequest } from "@/features/auth/model/authTypes"

export function useApiStatusQuery() {
  return useQuery({
    queryKey: ["system", "status"],
    queryFn: authApi.status,
  })
}

export function useLoginMutation() {
  return useMutation({
    mutationFn: (request: LoginRequest) => authApi.login(request),
  })
}

export function useCurrentUserQuery() {
  const simulationRole = useAuthStore((state) => state.simulationRole)
  const simulationObjectType = useAuthStore((state) => state.simulationObjectType)
  const simulationObjectId = useAuthStore((state) => state.simulationObjectId)

  return useQuery({
    queryKey: ["auth", "me", simulationRole, simulationObjectType, simulationObjectId],
    queryFn: authApi.me,
  })
}
