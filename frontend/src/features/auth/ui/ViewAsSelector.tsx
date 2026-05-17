import { Eye, RotateCcw } from "lucide-react"
import { useQueryClient } from "@tanstack/react-query"
import { useState } from "react"
import { useTranslation } from "react-i18next"
import { useCurrentUserQuery } from "@/features/auth/api/authQueries"
import { useAuthStore } from "@/features/auth/model/authStore"
import { Button } from "@/shared/ui/button"
import { roleLabel } from "@/shared/i18n/labels"

const roles = [
  "STAFF_ANALYST",
  "ARMY_COMMANDER",
  "FORMATION_COMMANDER",
  "UNIT_COMMANDER",
  "COMPANY_COMMANDER",
  "PLATOON_COMMANDER",
  "SQUAD_COMMANDER",
  "SOLDIER",
]

const scopes = [
  { labelKey: "district", objectType: "DISTRICT", objectId: 1 },
  { labelKey: "army", objectType: "ARMY", objectId: 11 },
  { labelKey: "formation", objectType: "CORPS", objectId: 1101 },
  { labelKey: "unit", objectType: "MILITARY_UNIT", objectId: 11101 },
  { labelKey: "company", objectType: "COMPANY", objectId: 1110101 },
  { labelKey: "platoon", objectType: "PLATOON", objectId: 11101011 },
  { labelKey: "squad", objectType: "SQUAD", objectId: 111010111 },
  { labelKey: "soldier", objectType: "SELF", objectId: 1110101111 },
]

export function ViewAsSelector() {
  const { t } = useTranslation(["common", "security"])
  const queryClient = useQueryClient()
  const { data: user } = useCurrentUserQuery()
  const simulationRole = useAuthStore((state) => state.simulationRole)
  const simulationObjectType = useAuthStore((state) => state.simulationObjectType)
  const simulationObjectId = useAuthStore((state) => state.simulationObjectId)
  const setSimulation = useAuthStore((state) => state.setSimulation)
  const clearSimulation = useAuthStore((state) => state.clearSimulation)
  const [role, setRole] = useState(simulationRole ?? "UNIT_COMMANDER")
  const [scopeKey, setScopeKey] = useState(`${simulationObjectType ?? "MILITARY_UNIT"}:${simulationObjectId ?? 11101}`)

  if (!user?.roles.includes("ADMIN_DISTRICT")) {
    return null
  }

  function applySimulation() {
    const scope = scopes.find((item) => `${item.objectType}:${item.objectId}` === scopeKey) ?? scopes[3]
    setSimulation({ role, objectType: scope.objectType, objectId: scope.objectId })
    queryClient.invalidateQueries()
  }

  function resetSimulation() {
    clearSimulation()
    queryClient.invalidateQueries()
  }

  return (
    <div className="flex flex-wrap items-center gap-2 rounded-md border border-zinc-800 bg-zinc-900/70 p-2">
      <Eye className="h-4 w-4 shrink-0 text-emerald-300" />
      <select
        value={role}
        onChange={(event) => setRole(event.target.value)}
        className="h-8 min-w-0 max-w-[210px] rounded-md border border-zinc-800 bg-zinc-950 px-2 text-xs text-zinc-100 outline-none focus:border-emerald-500"
        aria-label={t("security:simulation.roleLabel")}
      >
        {roles.map((item) => (
          <option key={item} value={item}>{roleLabel(t, item)}</option>
        ))}
      </select>
      <select
        value={scopeKey}
        onChange={(event) => setScopeKey(event.target.value)}
        className="h-8 min-w-0 max-w-[230px] rounded-md border border-zinc-800 bg-zinc-950 px-2 text-xs text-zinc-100 outline-none focus:border-emerald-500"
        aria-label={t("security:simulation.scopeLabel")}
      >
        {scopes.map((scope) => (
          <option key={`${scope.objectType}:${scope.objectId}`} value={`${scope.objectType}:${scope.objectId}`}>
            {t(`security:simulation.scopeLabels.${scope.labelKey}`)}
          </option>
        ))}
      </select>
      <Button size="sm" onClick={applySimulation}>{t("actions.viewAs")}</Button>
      {simulationRole ? (
        <Button size="sm" variant="ghost" onClick={resetSimulation}>
          <RotateCcw className="h-4 w-4 shrink-0" />
          {t("security:simulation.reset")}
        </Button>
      ) : null}
    </div>
  )
}
