import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { buildingsApi, equipmentApi, weaponsApi } from "@/features/inventory/api/inventoryApi"
import type { BuildingFilter, EquipmentTypeRequest, InventoryCategoryRequest, InventoryFilter, WeaponTypeRequest } from "@/features/inventory/model/inventoryTypes"

const apiByKind = {
  equipment: equipmentApi,
  weapons: weaponsApi,
}

export function useInventoryQuery(kind: "equipment" | "weapons", filters: InventoryFilter) {
  return useQuery({
    queryKey: [kind, filters],
    queryFn: () => apiByKind[kind].search(filters),
    staleTime: 30_000,
  })
}

export function useInventoryStatsQuery(kind: "equipment" | "weapons") {
  return useQuery({
    queryKey: [kind, "statistics"],
    queryFn: () => apiByKind[kind].stats(),
    staleTime: 30_000,
  })
}

export function useInventoryDictionariesQuery(kind: "equipment" | "weapons") {
  return useQuery({
    queryKey: [kind, "dictionaries"],
    queryFn: async () => {
      const [categories, types] = await Promise.all([apiByKind[kind].categories(), apiByKind[kind].types()])
      return { categories, types }
    },
    staleTime: 5 * 60_000,
  })
}

export function useSaveInventoryCategoryMutation(kind: "equipment" | "weapons", id?: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: InventoryCategoryRequest) => (id ? apiByKind[kind].updateCategory(id, request) : apiByKind[kind].createCategory(request)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [kind] }),
  })
}

export function useSaveInventoryTypeMutation(kind: "equipment" | "weapons", id?: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: EquipmentTypeRequest | WeaponTypeRequest) => (id ? apiByKind[kind].updateType(id, request) : apiByKind[kind].createType(request)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [kind] }),
  })
}

export function useInventoryTypePassportQuery(kind: "equipment" | "weapons", id?: number | null) {
  return useQuery({
    queryKey: [kind, "type-passport", id],
    queryFn: () => apiByKind[kind].typePassport(id ?? 0),
    enabled: Boolean(id),
    staleTime: 30_000,
  })
}

export function useUpdateInventoryMutation(kind: "equipment" | "weapons") {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: { unitId: number; typeId: number; quantity: number }) =>
      apiByKind[kind].update(request.unitId, request.typeId, request.quantity),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [kind] }),
  })
}

export function useDeleteInventoryMutation(kind: "equipment" | "weapons") {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: { unitId: number; typeId: number }) => apiByKind[kind].delete(request.unitId, request.typeId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [kind] }),
  })
}

export function useBuildingsQuery(filters: BuildingFilter) {
  return useQuery({
    queryKey: ["buildings", filters],
    queryFn: () => buildingsApi.search(filters),
    staleTime: 30_000,
  })
}

export function useBuildingStatsQuery() {
  return useQuery({
    queryKey: ["buildings", "statistics"],
    queryFn: () => buildingsApi.stats(),
  })
}

export function useSaveBuildingMutation(id?: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: { name: string; unitId: number }) => (id ? buildingsApi.update(id, request) : buildingsApi.create(request)),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["buildings"] }),
  })
}

export function useDeleteBuildingMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (id: number) => buildingsApi.delete(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["buildings"] }),
  })
}
