import { AlertTriangle } from "lucide-react"
import { useAlertsQuery } from "@/features/dashboard/api/dashboardQueries"
import { AlertCards } from "@/features/dashboard/ui/AlertCards"
import { PageHeader } from "@/shared/ui/page"
import { EmptyState, ErrorState, LoadingState } from "@/shared/ui/state"

export function AlertsPage() {
  const { data = [], isLoading, error } = useAlertsQuery()

  return (
    <div className="space-y-5">
      <PageHeader icon={AlertTriangle} eyebrow="Alert Center" title="Operational Alerts" description="Generated readiness and data-quality alerts for current command scope." />
      {isLoading ? <LoadingState title="Loading alerts" /> : null}
      {error ? <ErrorState title="Unable to load alerts" /> : null}
      {!isLoading && !error && !data.length ? <EmptyState title="No active alerts" description="Current scope has no generated readiness or data-quality alerts." /> : null}
      {data.length ? <AlertCards alerts={data} /> : null}
    </div>
  )
}
