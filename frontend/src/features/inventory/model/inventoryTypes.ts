export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type InventoryRow = {
  unitId: number
  unitName: string
  typeId: number
  typeName: string
  categoryId: number
  categoryName: string
  quantity: number
  status: string
}

export type BuildingRow = {
  id: number
  name: string
  unitId: number
  unitName: string
  subdivisionsCount: number
  status: string
}

export type InventoryType = {
  id: number
  name: string
  categoryId: number
  categoryName: string
}

export type InventoryCategory = {
  id: number
  name: string
}

export type InventoryStats = {
  visibleUnits: number
  inventoryRows: number
  totalQuantity: number
  warningRows: number
  readinessScore: number
}

export type BuildingStats = {
  visibleUnits: number
  buildings: number
  assignedBuildings: number
  emptyBuildings: number
  overloadedBuildings: number
  readinessScore: number
}

export type InventoryFilter = {
  search?: string
  unitId?: number
  categoryId?: number
  typeId?: number
  page?: number
  size?: number
}

export type BuildingFilter = {
  search?: string
  unitId?: number
  page?: number
  size?: number
}
