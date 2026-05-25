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
  assignable: boolean
  subdivisionsCount: number
  status: string
  assignedSubdivisions: BuildingAssignment[]
}

export type BuildingAssignment = {
  id: number
  name: string
  type: string
  unitId: number
  unitName: string
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

export type InventoryCategoryRequest = {
  name: string
}

export type AttributeValue = {
  id: number
  name: string
  dataType: string
  displayValue: string
}

export type DynamicAttributeMetadata = {
  id: number
  name: string
  dataType: "text" | "number" | "date" | "boolean"
  required: boolean
}

export type DynamicAttributeValueRequest = {
  attributeId: number
  value: string
}

export type EquipmentTypeRequest = {
  name: string
  categoryId: number
  purpose?: string | null
  crewSize?: number | null
  weightTons?: number | null
  maxSpeedKmh?: number | null
  operationalRangeKm?: number | null
  adoptionYear?: number | null
  manufacturer?: string | null
  description?: string | null
  attributes?: DynamicAttributeValueRequest[]
}

export type WeaponTypeRequest = {
  name: string
  categoryId: number
  purpose?: string | null
  caliber?: string | null
  effectiveRangeM?: number | null
  adoptionYear?: number | null
  manufacturer?: string | null
  description?: string | null
  attributes?: DynamicAttributeValueRequest[]
}

export type EquipmentTypePassport = InventoryType & {
  purpose?: string | null
  crewSize?: number | null
  weightTons?: number | null
  maxSpeedKmh?: number | null
  operationalRangeKm?: number | null
  adoptionYear?: number | null
  manufacturer?: string | null
  description?: string | null
  totalQuantity: number
  unitsCount: number
  attributes: AttributeValue[]
  distribution: InventoryRow[]
}

export type WeaponTypePassport = InventoryType & {
  purpose?: string | null
  caliber?: string | null
  effectiveRangeM?: number | null
  adoptionYear?: number | null
  manufacturer?: string | null
  description?: string | null
  totalQuantity: number
  unitsCount: number
  attributes: AttributeValue[]
  distribution: InventoryRow[]
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
