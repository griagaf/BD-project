export type CurrentUser = {
  userId: number
  soldierId: number | null
  username: string
  displayName: string
  roles: string[]
  permissions: string[]
}

