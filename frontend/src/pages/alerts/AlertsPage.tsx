import { AlertTriangle } from "lucide-react"
import { useTranslation } from "react-i18next"
import { useAlertsQuery } from "@/features/dashboard/api/dashboardQueries"
import { AlertCards } from "@/features/dashboard/ui/AlertCards"
import { PageHeader } from "@/shared/ui/page"
import { EmptyState, ErrorState, LoadingState } from "@/shared/ui/state"

export function AlertsPage() {
  const { t } = useTranslation("alerts")
  const { data = [], isLoading, error } = useAlertsQuery()

  return (
    <div className="space-y-5">
      <PageHeader icon={AlertTriangle} eyebrow={t("page.eyebrow")} title={t("page.title")} description={t("page.description")} />
      {isLoading ? <LoadingState title={t("states.loading")} /> : null}
      {error ? <ErrorState title={t("error")} /> : null}
      {!isLoading && !error && !data.length ? <EmptyState title={t("states.emptyTitle")} description={t("states.emptyDescription")} /> : null}
      {data.length ? <AlertCards alerts={data} /> : null}
    </div>
  )
}
