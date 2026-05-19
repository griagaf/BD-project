import { apiClient } from "@/shared/api/apiClient"

export type LookupOption = {
  id: number
  label: string
  description?: string | null
  type?: string | null
  parentLabel?: string | null
  disabledReason?: string | null
}

function query(search = "", limit = 50) {
  const params = new URLSearchParams()
  if (search.trim()) {
    params.set("search", search.trim())
  }
  params.set("limit", String(limit))
  return params.toString()
}

export const lookupApi = {
  units: (search?: string) => apiClient<LookupOption[]>(`/api/lookups/units?${query(search)}`),
  formations: (search?: string, types?: string) => {
    const params = new URLSearchParams(query(search))
    if (types) {
      params.set("types", types)
    }
    return apiClient<LookupOption[]>(`/api/lookups/formations?${params.toString()}`)
  },
  subdivisions: (search?: string) => apiClient<LookupOption[]>(`/api/lookups/subdivisions?${query(search)}`),
  ranks: (search?: string) => apiClient<LookupOption[]>(`/api/lookups/ranks?${query(search)}`),
  specialties: (search?: string) => apiClient<LookupOption[]>(`/api/lookups/specialties?${query(search)}`),
  equipmentTypes: (search?: string) => apiClient<LookupOption[]>(`/api/lookups/equipment-types?${query(search)}`),
  weaponTypes: (search?: string) => apiClient<LookupOption[]>(`/api/lookups/weapon-types?${query(search)}`),
  buildings: (search?: string) => apiClient<LookupOption[]>(`/api/lookups/buildings?${query(search)}`),
  personnel: (search?: string) => apiClient<LookupOption[]>(`/api/lookups/personnel?${query(search)}`),
}
