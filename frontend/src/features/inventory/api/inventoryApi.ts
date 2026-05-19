import { apiClient } from "@/shared/api/apiClient"
import type {
  BuildingFilter,
  BuildingRow,
  BuildingStats,
  EquipmentTypePassport,
  EquipmentTypeRequest,
  InventoryCategoryRequest,
  InventoryCategory,
  InventoryFilter,
  InventoryRow,
  InventoryStats,
  InventoryType,
  PageResponse,
  WeaponTypePassport,
  WeaponTypeRequest,
} from "@/features/inventory/model/inventoryTypes"

function queryString(filters: Record<string, unknown>) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && `${value}`.trim() !== "") {
      params.set(key, `${value}`)
    }
  })
  const query = params.toString()
  return query ? `?${query}` : ""
}

function inventoryApi(resource: "equipment" | "weapons") {
  const unitSegment = resource === "equipment" ? "equipment" : "weapons"
  return {
    search: (filters: InventoryFilter) => apiClient<PageResponse<InventoryRow>>(`/api/${resource}${queryString(filters)}`),
    stats: () => apiClient<InventoryStats>(`/api/${resource}/statistics`),
    categories: () => apiClient<InventoryCategory[]>(`/api/${resource}/categories`),
    types: () => apiClient<InventoryType[]>(`/api/${resource}/types`),
    createCategory: (request: InventoryCategoryRequest) =>
      apiClient<InventoryCategory>(`/api/${resource}/categories`, {
        method: "POST",
        body: JSON.stringify(request),
      }),
    updateCategory: (id: number, request: InventoryCategoryRequest) =>
      apiClient<InventoryCategory>(`/api/${resource}/categories/${id}`, {
        method: "PUT",
        body: JSON.stringify(request),
      }),
    deleteCategory: (id: number) =>
      apiClient<void>(`/api/${resource}/categories/${id}`, {
        method: "DELETE",
      }),
    createType: (request: EquipmentTypeRequest | WeaponTypeRequest) =>
      apiClient<InventoryType>(`/api/${resource}/types`, {
        method: "POST",
        body: JSON.stringify(request),
      }),
    updateType: (id: number, request: EquipmentTypeRequest | WeaponTypeRequest) =>
      apiClient<InventoryType>(`/api/${resource}/types/${id}`, {
        method: "PUT",
        body: JSON.stringify(request),
      }),
    deleteType: (id: number) =>
      apiClient<void>(`/api/${resource}/types/${id}`, {
        method: "DELETE",
      }),
    typePassport: (id: number) =>
      apiClient<EquipmentTypePassport | WeaponTypePassport>(`/api/${resource}/types/${id}/passport`),
    update: (unitId: number, typeId: number, quantity: number) =>
      apiClient<InventoryRow>(`/api/units/${unitId}/${unitSegment}/${typeId}`, {
        method: "PUT",
        body: JSON.stringify({ quantity }),
      }),
    delete: (unitId: number, typeId: number) =>
      apiClient<void>(`/api/units/${unitId}/${unitSegment}/${typeId}`, {
        method: "DELETE",
      }),
  }
}

export const equipmentApi = inventoryApi("equipment")
export const weaponsApi = inventoryApi("weapons")

export const buildingsApi = {
  search: (filters: BuildingFilter) => apiClient<PageResponse<BuildingRow>>(`/api/buildings${queryString(filters)}`),
  stats: () => apiClient<BuildingStats>("/api/buildings/statistics"),
  create: (request: { name: string; unitId: number }) =>
    apiClient<BuildingRow>("/api/buildings", {
      method: "POST",
      body: JSON.stringify(request),
    }),
  update: (id: number, request: { name: string; unitId: number }) =>
    apiClient<BuildingRow>(`/api/buildings/${id}`, {
      method: "PUT",
      body: JSON.stringify(request),
    }),
  delete: (id: number) =>
    apiClient<void>(`/api/buildings/${id}`, {
      method: "DELETE",
    }),
}
