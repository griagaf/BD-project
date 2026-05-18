import { AlertTriangle } from "lucide-react"
import { useAlertsQuery } from "@/features/dashboard/api/dashboardQueries"
import { AlertCards } from "@/features/dashboard/ui/AlertCards"
import { Card } from "@/shared/ui/card"

export function AlertsPage() {
  const { data = [], isLoading, error } = useAlertsQuery()

  return (
    <div className="space-y-5">
      <div>
        <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
          <AlertTriangle className="size-4" />
          Alert Center
        </div>
        <h1 className="mt-1 text-2xl font-semibold text-zinc-100">Operational Alerts</h1>
        <p className="mt-1 text-sm text-zinc-500">Generated readiness and data-quality alerts for current command scope.</p>
      </div>
      {isLoading ? <Card className="text-sm text-zinc-500">Loading alerts</Card> : null}
      {error ? <Card className="border-red-950 bg-red-950/20 text-sm text-red-200">Unable to load alerts</Card> : null}
      <AlertCards alerts={data} />
    </div>
  )
}
