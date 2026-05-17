import { apiClient } from "@/shared/api/apiClient"
import type { PageResponse, Personnel, PersonnelFilter, PersonnelProfile, PersonnelRequest, Rank, Specialty } from "@/features/personnel/model/personnelTypes"

function queryString(filters: PersonnelFilter) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && `${value}`.trim() !== "") {
      params.set(key, `${value}`)
    }
  })
  const query = params.toString()
  return query ? `?${query}` : ""
}

export const personnelApi = {
  search: (filters: PersonnelFilter) => apiClient<PageResponse<Personnel>>(`/api/personnel${queryString(filters)}`),
  getById: (id: number) => apiClient<Personnel>(`/api/personnel/${id}`),
  profile: (id: number) => apiClient<PersonnelProfile>(`/api/personnel/${id}/profile`),
  create: (request: PersonnelRequest) =>
    apiClient<Personnel>("/api/personnel", {
      method: "POST",
      body: JSON.stringify(request),
    }),
  update: (id: number, request: PersonnelRequest) =>
    apiClient<Personnel>(`/api/personnel/${id}`, {
      method: "PUT",
      body: JSON.stringify(request),
    }),
  delete: (id: number) =>
    apiClient<void>(`/api/personnel/${id}`, {
      method: "DELETE",
    }),
  ranks: () => apiClient<Rank[]>("/api/personnel/ranks"),
  specialties: () => apiClient<Specialty[]>("/api/personnel/specialties"),
}
