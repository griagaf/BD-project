import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { personnelApi } from "@/features/personnel/api/personnelApi"
import type { PersonnelFilter, PersonnelRequest } from "@/features/personnel/model/personnelTypes"

export function usePersonnelQuery(filters: PersonnelFilter) {
  return useQuery({
    queryKey: ["personnel", filters],
    queryFn: () => personnelApi.search(filters),
    staleTime: 30_000,
  })
}

export function usePersonnelProfileQuery(id: number) {
  return useQuery({
    queryKey: ["personnel", id, "profile"],
    queryFn: () => personnelApi.profile(id),
    staleTime: 30_000,
  })
}

export function usePersonnelDictionariesQuery() {
  return useQuery({
    queryKey: ["personnel", "dictionaries"],
    queryFn: async () => {
      const [ranks, specialties] = await Promise.all([personnelApi.ranks(), personnelApi.specialties()])
      return { ranks, specialties }
    },
    staleTime: 5 * 60_000,
  })
}

export function useCreatePersonnelMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: PersonnelRequest) => personnelApi.create(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["personnel"] }),
  })
}

export function useUpdatePersonnelMutation(id: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: PersonnelRequest) => personnelApi.update(id, request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["personnel"] }),
  })
}

export function useDeletePersonnelMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => personnelApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["personnel"] }),
  })
}
