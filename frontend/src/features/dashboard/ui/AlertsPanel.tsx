import type { ProblemZone, TacticalAlert } from "@/features/dashboard/model/dashboardTypes"
import { Card } from "@/shared/ui/card"
import { AlertCards } from "@/features/dashboard/ui/AlertCards"

type AlertsPanelProps = {
  alerts: TacticalAlert[]
  problemZones?: ProblemZone[]
}

export function AlertsPanel({ alerts, problemZones = [] }: AlertsPanelProps) {
  return (
    <div className="space-y-4">
      <Card className="space-y-3">
        <div className="text-xs uppercase text-emerald-300">Problem zones</div>
        {problemZones.map((zone) => (
          <div key={zone.type} className="flex items-center justify-between rounded-md border border-zinc-800 bg-zinc-900/40 px-3 py-2">
            <div>
              <div className="text-sm font-medium text-zinc-100">{zone.label}</div>
              <div className="text-xs text-zinc-500">{zone.severity}</div>
            </div>
            <div className="text-xl font-semibold text-zinc-100">{zone.count}</div>
          </div>
        ))}
        {!problemZones.length ? <div className="text-sm text-zinc-500">No problem zones detected</div> : null}
      </Card>
      <AlertCards alerts={alerts} />
    </div>
  )
}
