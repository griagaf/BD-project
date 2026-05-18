export type CurrentUser = {
  userId: number
  soldierId: number | null
  username: string
  displayName: string
  roles: string[]
  effectiveRoles?: string[]
  assignments?: Array<{
    assignmentId: number
    objectType: string
    objectId: number
    startsAt: string
    endsAt: string | null
    primary: boolean
  }>
  permissions: string[]
  accessSimulationActive?: boolean
}

export type AccessSimulationScope = {
  role: string
  objectType: string
  objectId: number
}

export type LoginRequest = {
  username: string
  password: string
}

export type TokenResponse = {
  accessToken: string
  refreshToken: string
  expiresIn: number
  user: CurrentUser
}
