import { AlertTriangle } from "lucide-react"
import type { TacticalAlert } from "@/features/dashboard/model/dashboardTypes"
import { Card } from "@/shared/ui/card"
import { ActionButtons } from "@/features/dashboard/ui/ActionButtons"

type AlertCardsProps = {
  alerts: TacticalAlert[]
}

export function AlertCards({ alerts }: AlertCardsProps) {
  return (
    <div className="space-y-3">
      {alerts.map((alert) => (
        <Card key={alert.id} className={severityClasses(alert.severity).card}>
          <div className="flex gap-3">
            <div className={`flex size-10 shrink-0 items-center justify-center rounded-md ${severityClasses(alert.severity).icon}`}>
              <AlertTriangle className="size-5" />
            </div>
            <div className="min-w-0 flex-1">
              <div className="flex flex-wrap items-center gap-2">
                <h3 className="font-semibold text-zinc-100">{alert.title}</h3>
                <span className={`rounded-md border px-2 py-0.5 text-xs ${severityClasses(alert.severity).badge}`}>
                  {alert.severity}
                </span>
              </div>
              <p className="mt-1 text-sm text-zinc-400">{alert.message}</p>
              <div className="mt-3"><ActionButtons actions={alert.actions} /></div>
            </div>
          </div>
        </Card>
      ))}
      {!alerts.length ? <Card className="text-sm text-zinc-500">No critical alerts in current scope</Card> : null}
    </div>
  )
}

function severityClasses(severity: string) {
  if (severity === "CRITICAL") {
    return {
      card: "border-red-950/60 bg-red-950/10",
      icon: "bg-red-500/10 text-red-300",
      badge: "border-red-500/30 text-red-300",
    }
  }
  if (severity === "HIGH") {
    return {
      card: "border-amber-950/60 bg-amber-950/10",
      icon: "bg-amber-500/10 text-amber-300",
      badge: "border-amber-500/30 text-amber-300",
    }
  }
  if (severity === "MEDIUM") {
    return {
      card: "border-sky-950/60 bg-sky-950/10",
      icon: "bg-sky-500/10 text-sky-300",
      badge: "border-sky-500/30 text-sky-300",
    }
  }
  return {
    card: "border-emerald-950/60 bg-emerald-950/10",
    icon: "bg-emerald-500/10 text-emerald-300",
    badge: "border-emerald-500/30 text-emerald-300",
  }
}
