import { useCurrentUserQuery } from "@/features/auth/api/authQueries"
import { useTranslation } from "react-i18next"
import { roleLabel } from "@/shared/i18n/labels"

export function RolePreview() {
  const { t } = useTranslation("common")
  const { data: user } = useCurrentUserQuery()
  const assignment = user?.assignments?.[0]
  const role = user?.effectiveRoles?.[0] ?? user?.roles[0] ?? "NO_ROLE"

  return (
    <div className="min-w-0">
      <div className="text-xs uppercase text-zinc-500">{t("session.scope")}</div>
      <div className="max-w-[44vw] truncate text-sm font-medium text-zinc-200 lg:max-w-[360px]" title={`${roleLabel(t, role)} / ${assignment ? `${assignment.objectType}:${assignment.objectId}` : t("session.unscoped")}`}>
        {roleLabel(t, role)} / {assignment ? `${assignment.objectType}:${assignment.objectId}` : t("session.unscoped")}
      </div>
    </div>
  )
}
