import { apiClient } from "@/shared/api/apiClient"
import type { FocusTreeResponse, HierarchyContext, ObjectPassport, TreeMode, TreeNode, Unit } from "@/features/hierarchy/model/hierarchyTypes"

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
}
