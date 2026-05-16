import type { ReactNode } from "react"
import { useCurrentUserQuery } from "@/features/auth/api/authQueries"

type PermissionGuardProps = {
  children: ReactNode
  fallback?: ReactNode
  permissions?: string[]
  roles?: string[]
}

export function PermissionGuard({ children, fallback = null, permissions = [], roles = [] }: PermissionGuardProps) {
  const { data: user } = useCurrentUserQuery()

  const hasPermission = permissions.length === 0 || permissions.some((permission) => user?.permissions.includes(permission))
  const hasRole = roles.length === 0 || roles.some((role) => user?.roles.includes(role))

  if (!hasPermission || !hasRole) {
    return fallback
  }

  return children
}
