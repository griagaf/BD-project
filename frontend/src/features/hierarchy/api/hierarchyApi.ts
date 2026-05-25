import { apiClient } from "@/shared/api/apiClient"
import type { FocusTreeResponse, FormationRequest, HierarchyContext, ObjectPassport, SubdivisionRequest, TreeMode, TreeNode, Unit, UnitRequest } from "@/features/hierarchy/model/hierarchyTypes"

export const hierarchyApi = {
  roots: (mode: TreeMode) => apiClient<TreeNode[]>(`/api/hierarchy/roots?mode=${mode}`),
  children: (nodeType: string, nodeId: number) =>
    apiClient<TreeNode[]>(`/api/hierarchy/nodes/${nodeType}/${nodeId}/children`),
  focus: (nodeType: string, nodeId: number) =>
    apiClient<FocusTreeResponse>(`/api/hierarchy/focus?nodeType=${nodeType}&nodeId=${nodeId}`),
  personnelChain: (personnelId: number) => apiClient<FocusTreeResponse>(`/api/hierarchy/personnel/${personnelId}/chain`),
  passport: (nodeType: string, nodeId: number) =>
    apiClient<ObjectPassport>(`/api/hierarchy/nodes/${nodeType}/${nodeId}/passport`),
  context: (nodeType: string, nodeId: number) =>
    apiClient<HierarchyContext>(`/api/hierarchy/nodes/${nodeType}/${nodeId}/context`),
  unitPassport: (unitId: number) => apiClient<ObjectPassport>(`/api/units/${unitId}/passport`),
  units: () => apiClient<Unit[]>("/api/units"),
  createFormation: (request: FormationRequest) =>
    apiClient("/api/formations", {
      method: "POST",
      body: JSON.stringify(request),
    }),
  createUnit: (request: UnitRequest) =>
    apiClient("/api/units", {
      method: "POST",
      body: JSON.stringify(request),
    }),
  createSubdivision: (request: SubdivisionRequest) =>
    apiClient("/api/subdivisions", {
      method: "POST",
      body: JSON.stringify(request),
    }),
  assignCommander: (nodeType: string, nodeId: number, commanderId: number) => {
    const resource = nodeType === "MILITARY_UNIT"
      ? "units"
      : nodeType === "COMPANY" || nodeType === "PLATOON" || nodeType === "SQUAD" || nodeType === "BATTALION"
        ? "subdivisions"
        : "formations"
    return apiClient(`/api/${resource}/${nodeId}/commander`, {
      method: "PUT",
      body: JSON.stringify({ commanderId }),
    })
  },
}
