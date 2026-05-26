import { apiClient } from "@/shared/api/apiClient"

export type LookupOption = {
  id: number
  label: string
  description?: string | null
  type?: string | null
  parentLabel?: string | null
  disabledReason?: string | null
}

function query(search = "", limit = 50, extra?: Record<string, string | number | null | undefined>) {
  const params = new URLSearchParams()
  if (search.trim()) {
    params.set("search", search.trim())
  }
  params.set("limit", String(limit))
  Object.entries(extra ?? {}).forEach(([key, value]) => {
    if (value !== undefined && value !== null && `${value}`.trim() !== "") {
      params.set(key, `${value}`)
    }
  })
  return params.toString()
}

export const lookupApi = {
  units: (search?: string, extra?: { formationId?: number | null; limit?: number }) =>
    apiClient<LookupOption[]>(`/api/lookups/units?${query(search, extra?.limit ?? 50, { formationId: extra?.formationId })}`),
  formations: (search?: string, types?: string, extra?: { parentId?: number | null; limit?: number }) => {
    const params = new URLSearchParams(query(search, extra?.limit ?? 50, { parentId: extra?.parentId }))
    if (types) {
      params.set("types", types)
    }
    return apiClient<LookupOption[]>(`/api/lookups/formations?${params.toString()}`)
  },
  subdivisions: (search?: string, extra?: { unitId?: number | null; parentId?: number | null; type?: string; limit?: number }) =>
    apiClient<LookupOption[]>(`/api/lookups/subdivisions?${query(search, extra?.limit ?? 50, {
      unitId: extra?.unitId,
      parentId: extra?.parentId,
      type: extra?.type,
    })}`),
  ranks: (search?: string, limit = 50, extra?: { category?: string | null }) =>
    apiClient<LookupOption[]>(`/api/lookups/ranks?${query(search, limit, { category: extra?.category })}`),
  specialties: (search?: string, limit = 50) => apiClient<LookupOption[]>(`/api/lookups/specialties?${query(search, limit)}`),
  equipmentTypes: (search?: string, limit = 50) => apiClient<LookupOption[]>(`/api/lookups/equipment-types?${query(search, limit)}`),
  weaponTypes: (search?: string, limit = 50) => apiClient<LookupOption[]>(`/api/lookups/weapon-types?${query(search, limit)}`),
  buildings: (search?: string, limit = 50) => apiClient<LookupOption[]>(`/api/lookups/buildings?${query(search, limit)}`),
  personnel: (search?: string, extra?: { unitId?: number | null; subdivisionId?: number | null; limit?: number }) =>
    apiClient<LookupOption[]>(`/api/lookups/personnel?${query(search, extra?.limit ?? 50, {
      unitId: extra?.unitId,
      subdivisionId: extra?.subdivisionId,
    })}`),
}
