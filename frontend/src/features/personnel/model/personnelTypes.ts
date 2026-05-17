export type Rank = {
  id: number
  name: string
  category: string
}

export type Specialty = {
  id: number
  name: string
}

export type Personnel = {
  id: number
  lastName: string
  firstName: string
  middleName: string | null
  fullName: string
  personalNumber: string
  birthDate: string
  serviceStart: string
  subdivisionId: number
  subdivisionName: string
  unitId: number
  unitName: string
  rank: Rank | null
  specialties: Specialty[]
}

export type PersonnelFilter = {
  search?: string
  unitId?: string
  subdivisionId?: string
  rank?: string
  specialtyId?: string
  page?: number
  size?: number
  sort?: string
}

export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type PersonnelRequest = {
  lastName: string
  firstName: string
  middleName?: string | null
  personalNumber: string
  birthDate: string
  serviceStart: string
  subdivisionId: number
  rankId?: number | null
  rankAssignmentDate?: string | null
  specialtyIds: number[]
}

export type ChainOfCommandNode = {
  objectType: string
  objectId: number
  objectName: string
  commanderPersonnelId: number | null
  commanderName: string | null
}

export type PersonnelProfile = {
  personnel: Personnel
  formationName: string | null
  assignmentPath: string
  profileGeneratedAt: string
  chainOfCommand: ChainOfCommandNode[]
}
