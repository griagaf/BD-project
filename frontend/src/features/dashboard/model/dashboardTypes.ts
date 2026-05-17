export type DashboardStatistics = {
  formations: number
  units: number
  subdivisions: number
  personnel: number
  equipmentQuantity: number
  weaponQuantity: number
  buildings: number
  openAlerts: number
}

export type ReadinessAxis = {
  key: string
  label: string
  score: number
  status: string
}

export type Readiness = {
  overall: number
  axes: ReadinessAxis[]
}

export type AlertAction = {
  label: string
  route: string
  queryTemplate: string | null
}

export type TacticalAlert = {
  id: string
  type: string
  severity: "LOW" | "MEDIUM" | "HIGH" | "CRITICAL"
  title: string
  message: string
  objectType: string
  objectId: number
  details: Record<string, unknown>
  actions: AlertAction[]
}

export type ProblemZone = {
  type: string
  label: string
  severity: string
  count: number
}

export type AuditEvent = {
  id: number
  eventType: string
  objectType: string
  objectId: number | null
  actor: string
  message: string
  createdAt: string
}

export type TacticalDashboard = {
  statistics: DashboardStatistics
  readiness: Readiness
  problemZones: ProblemZone[]
  latestEvents: AuditEvent[]
  criticalAlerts: TacticalAlert[]
}
