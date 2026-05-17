export type ReportObjectType = "DISTRICT" | "ARMY" | "FORMATION" | "BRIGADE" | "MILITARY_UNIT" | "COMPANY" | "PLATOON" | "SQUAD"

export type SmartMissionReportRequest = {
  objectType: ReportObjectType
  objectId: number
  includePersonnel: boolean
  includeResources: boolean
  includeAlerts: boolean
  includeRecommendations: boolean
}

export type ReportObject = {
  type: ReportObjectType
  id: number
  name: string
  parentName: string | null
  status: string | null
  location: string | null
  path: string[]
}

export type ReportCommander = {
  personnelId: number
  fullName: string
  rankName: string | null
  position: string
  objectName: string
}

export type ReportPersonnelSummary = {
  total: number
  officers: number
  enlisted: number
  commanders: number
  byRank: Record<string, number>
  bySubdivision: Record<string, number>
}

export type ResourceQuantity = {
  typeName: string
  categoryName: string
  quantity: number
}

export type ReportEquipmentSummary = {
  totalQuantity: number
  typesCount: number
  unitsWithoutEquipment: number
  topEquipment: ResourceQuantity[]
  missingEquipmentWarnings: string[]
}

export type ReportWeaponSummary = {
  totalQuantity: number
  typesCount: number
  unitsWithoutWeapons: number
  topWeapons: ResourceQuantity[]
  missingWeaponWarnings: string[]
}

export type BuildingUsage = {
  buildingId: number
  buildingName: string
  unitName: string
  subdivisionsCount: number
}

export type ReportBuildingSummary = {
  total: number
  unused: number
  overloaded: number
  problemBuildings: BuildingUsage[]
}

export type ReportSpecialtySummary = {
  totalSpecialties: number
  coveredSpecialties: number
  missingSpecialties: number
  missingSpecialtyNames: string[]
  topSpecialties: Record<string, number>
}

export type ReportAlert = {
  alertId: string
  type: string
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"
  title: string
  message: string
  objectType: string
  objectId: number
}

export type ReportReadiness = {
  overall: number
  personnel: number
  equipment: number
  weapons: number
  specialists: number
  infrastructure: number
  status: string
}

export type ReportRecommendation = {
  code: string
  severity: "INFO" | "MEDIUM" | "HIGH" | "CRITICAL"
  title: string
  description: string
  actionLabel: string
  actionRoute: string
}

export type SmartMissionReport = {
  reportId: string
  object: ReportObject
  generatedAt: string
  commanders: ReportCommander[]
  personnel: ReportPersonnelSummary
  equipment: ReportEquipmentSummary
  weapons: ReportWeaponSummary
  buildings: ReportBuildingSummary
  specialties: ReportSpecialtySummary
  alerts: ReportAlert[]
  readiness: ReportReadiness
  recommendations: ReportRecommendation[]
}
