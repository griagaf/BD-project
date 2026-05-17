import { useCurrentUserQuery } from "@/features/auth/api/authQueries"

export function RolePreview() {
  const { data: user } = useCurrentUserQuery()
  const assignment = user?.assignments?.[0]
  const role = user?.effectiveRoles?.[0] ?? user?.roles[0] ?? "NO_ROLE"

  return (
    <div>
      <div className="text-xs uppercase text-zinc-500">Active command scope</div>
      <div className="text-sm font-medium text-zinc-200">
        {role} / {assignment ? `${assignment.objectType}:${assignment.objectId}` : "unscoped"}
      </div>
    </div>
  )
}
