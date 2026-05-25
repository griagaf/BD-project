import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { buildingsApi, equipmentApi, weaponsApi } from "@/features/inventory/api/inventoryApi"
import type { BuildingFilter, DynamicAttributeMetadata, EquipmentTypeRequest, InventoryCategoryRequest, InventoryFilter, WeaponTypeRequest } from "@/features/inventory/model/inventoryTypes"

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

export function useInventoryAttributeSchemaQuery(kind: "equipment" | "weapons", categoryId?: number | null) {
  return useQuery({
    queryKey: [kind, "attribute-schema", categoryId],
    queryFn: () => apiByKind[kind].attributeSchema(categoryId ?? 0),
    enabled: Boolean(categoryId),
    staleTime: 5 * 60_000,
  })
}

export function useInventoryAttributeTypesQuery(kind: "equipment" | "weapons") {
  return useQuery({
    queryKey: [kind, "attribute-types"],
    queryFn: () => apiByKind[kind].attributeTypes(),
    staleTime: 5 * 60_000,
  })
}

export function useCreateInventoryAttributeTypeMutation(kind: "equipment" | "weapons") {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: { name: string; dataType: DynamicAttributeMetadata["dataType"] }) => apiByKind[kind].createAttributeType(request),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: [kind, "attribute-types"] }),
  })
}

export function useAssignInventoryAttributeMutation(kind: "equipment" | "weapons") {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: { categoryId: number; attributeId: number; required?: boolean }) =>
      apiByKind[kind].assignAttribute(request.categoryId, { attributeId: request.attributeId, required: request.required }),
    onSuccess: (_data, variables) => {
      queryClient.invalidateQueries({ queryKey: [kind, "attribute-schema", variables.categoryId] })
      queryClient.invalidateQueries({ queryKey: [kind, "attribute-types"] })
    },
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
    mutationFn: (request: { name: string; unitId: number; assignable?: boolean }) => (id ? buildingsApi.update(id, request) : buildingsApi.create(request)),
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

export function useAssignBuildingSubdivisionMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: { buildingId: number; subdivisionId: number }) =>
      buildingsApi.assignSubdivision(request.buildingId, request.subdivisionId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["buildings"] }),
  })
}

export function useRemoveBuildingSubdivisionMutation() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (request: { buildingId: number; subdivisionId: number }) =>
      buildingsApi.removeSubdivision(request.buildingId, request.subdivisionId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ["buildings"] }),
  })
}
