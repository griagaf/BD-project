import { Eye, RotateCcw } from "lucide-react"
import { useQueryClient } from "@tanstack/react-query"
import { useState } from "react"
import { useCurrentUserQuery } from "@/features/auth/api/authQueries"
import { useAuthStore } from "@/features/auth/model/authStore"
import { Button } from "@/shared/ui/button"

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
  { label: "District / 1", objectType: "DISTRICT", objectId: 1 },
  { label: "Army / 2", objectType: "ARMY", objectId: 2 },
  { label: "Brigade / 5", objectType: "BRIGADE", objectId: 5 },
  { label: "Unit / 1", objectType: "MILITARY_UNIT", objectId: 1 },
  { label: "Company / 10", objectType: "COMPANY", objectId: 10 },
  { label: "Platoon / 11", objectType: "PLATOON", objectId: 11 },
  { label: "Squad / 12", objectType: "SQUAD", objectId: 12 },
  { label: "Soldier / 9", objectType: "SELF", objectId: 9 },
]

export function ViewAsSelector() {
  const queryClient = useQueryClient()
  const { data: user } = useCurrentUserQuery()
  const simulationRole = useAuthStore((state) => state.simulationRole)
  const simulationObjectType = useAuthStore((state) => state.simulationObjectType)
  const simulationObjectId = useAuthStore((state) => state.simulationObjectId)
  const setSimulation = useAuthStore((state) => state.setSimulation)
  const clearSimulation = useAuthStore((state) => state.clearSimulation)
  const [role, setRole] = useState(simulationRole ?? "UNIT_COMMANDER")
  const [scopeKey, setScopeKey] = useState(`${simulationObjectType ?? "MILITARY_UNIT"}:${simulationObjectId ?? 1}`)

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
      <Eye className="size-4 text-emerald-300" />
      <select
        value={role}
        onChange={(event) => setRole(event.target.value)}
        className="h-8 rounded-md border border-zinc-800 bg-zinc-950 px-2 text-xs text-zinc-100 outline-none focus:border-emerald-500"
        aria-label="Simulation role"
      >
        {roles.map((item) => (
          <option key={item} value={item}>{item}</option>
        ))}
      </select>
      <select
        value={scopeKey}
        onChange={(event) => setScopeKey(event.target.value)}
        className="h-8 rounded-md border border-zinc-800 bg-zinc-950 px-2 text-xs text-zinc-100 outline-none focus:border-emerald-500"
        aria-label="Simulation scope"
      >
        {scopes.map((scope) => (
          <option key={`${scope.objectType}:${scope.objectId}`} value={`${scope.objectType}:${scope.objectId}`}>{scope.label}</option>
        ))}
      </select>
      <Button className="h-8 px-3 text-xs" onClick={applySimulation}>View as</Button>
      {simulationRole ? (
        <Button className="h-8 px-3 text-xs" variant="ghost" onClick={resetSimulation}>
          <RotateCcw className="size-3.5" />
          Reset
        </Button>
      ) : null}
    </div>
  )
}
