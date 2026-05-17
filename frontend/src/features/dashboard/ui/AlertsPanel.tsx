import type { ProblemZone, TacticalAlert } from "@/features/dashboard/model/dashboardTypes"
import { useTranslation } from "react-i18next"
import { Card } from "@/shared/ui/card"
import { AlertCards } from "@/features/dashboard/ui/AlertCards"
import { statusLabel } from "@/shared/i18n/labels"

type AlertsPanelProps = {
  alerts: TacticalAlert[]
  problemZones?: ProblemZone[]
}

export function AlertsPanel({ alerts, problemZones = [] }: AlertsPanelProps) {
  const { t } = useTranslation(["common", "dashboard"])
  return (
    <div className="space-y-4">
      <Card className="space-y-3">
        <div className="text-xs uppercase text-emerald-300">{t("dashboard:problems.title")}</div>
        {problemZones.map((zone) => (
          <div key={zone.type} className="flex items-center justify-between rounded-md border border-zinc-800 bg-zinc-900/40 px-3 py-2">
            <div>
              <div className="break-words text-sm font-medium text-zinc-100">{zone.label}</div>
              <div className="text-xs text-zinc-500">{statusLabel(t, zone.severity)}</div>
            </div>
            <div className="text-xl font-semibold text-zinc-100">{zone.count}</div>
          </div>
        ))}
        {!problemZones.length ? <div className="text-sm text-zinc-500">{t("dashboard:problems.empty")}</div> : null}
      </Card>
      <AlertCards alerts={alerts} />
    </div>
  )
}
