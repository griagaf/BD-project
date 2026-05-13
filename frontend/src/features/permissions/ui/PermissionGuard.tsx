import type { ReactNode } from "react"

type PermissionGuardProps = {
  children: ReactNode
  fallback?: ReactNode
}

export function PermissionGuard({ children }: PermissionGuardProps) {
  return children
}

