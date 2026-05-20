export type TreeMode = "STRATEGIC" | "FOCUS" | "CHAIN"

export type Commander = {
  personnelId: number
  fullName: string
  rankName: string | null
}

export type TreeNodeMetrics = {
  personnelCount: number
  unitCount: number
  subdivisionCount: number
  equipmentCount: number
  weaponCount: number
  readinessScore: number
}

export type TreeNode = {
  id: string
  type: string
  objectId: number
  label: string
  subtitle: string | null
  status: string | null
  level: number
  hasChildren: boolean
  childrenLoaded: boolean
  childrenCount: number
  commander: Commander | null
  metrics: TreeNodeMetrics
  children: TreeNode[]
}

export type FocusTreeResponse = {
  mode: TreeMode
  selectedNode: TreeNode | null
  parentPath: TreeNode[]
  siblings: TreeNode[]
  children: TreeNode[]
}

export type ObjectPassport = {
  objectType: string
  objectId: number
  name: string
  subtitle: string | null
  status: string | null
  commander: Commander | null
  metrics: TreeNodeMetrics
  breadcrumbs: TreeNode[]
  details: Record<string, unknown>
}

export type ActionItem = {
  code: string
  label: string
  path: string
  enabled: boolean
}

export type HierarchyContext = {
  actions: ActionItem[]
  alerts: string[]
  statistics: TreeNodeMetrics
}

export type Unit = {
  id: number
  name: string
  formationId: number
  formationName: string
  locationId: number | null
  locationName: string | null
  commander: Commander | null
}

export type HierarchySelection = {
  type: string
  id: number
}

export type SubdivisionRequest = {
  name: string
  type: string
  unitId: number
  parentId?: number | null
  commanderId?: number | null
}
