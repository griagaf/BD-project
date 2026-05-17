import type { ReactNode } from "react"
import { useCurrentUserQuery } from "@/features/auth/api/authQueries"

type PermissionGuardProps = {
  children: ReactNode
  fallback?: ReactNode
  permissions?: string[]
  roles?: string[]
  mode?: "any" | "all"
}

export function PermissionGuard({ children, fallback = null, permissions = [], roles = [], mode = "any" }: PermissionGuardProps) {
  const { data: user } = useCurrentUserQuery()

  const hasPermission = permissions.length === 0 || (
    mode === "all"
      ? permissions.every((permission) => user?.permissions.includes(permission))
      : permissions.some((permission) => user?.permissions.includes(permission))
  )
  const effectiveRoles = user?.effectiveRoles ?? user?.roles ?? []
  const hasRole = roles.length === 0 || roles.some((role) => effectiveRoles.includes(role))

  if (!hasPermission || !hasRole) {
    return fallback
  }

  return children
}
