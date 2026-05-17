import { Activity, Clock } from "lucide-react"
import { useTranslation } from "react-i18next"
import { useDashboardQuery } from "@/features/dashboard/api/dashboardQueries"
import { AlertsPanel } from "@/features/dashboard/ui/AlertsPanel"
import { ReadinessRadar } from "@/features/dashboard/ui/ReadinessRadar"
import { StatsCards } from "@/features/dashboard/ui/StatsCards"
import { Card } from "@/shared/ui/card"
import { PageHeader } from "@/shared/ui/page"
import { ErrorState, LoadingState } from "@/shared/ui/state"
import { objectTypeLabel } from "@/shared/i18n/labels"

export function DashboardPage() {
  const { t } = useTranslation("dashboard")
  const { data, isLoading, error } = useDashboardQuery()

  return (
    <div className="space-y-5">
      <PageHeader icon={Activity} eyebrow={t("page.eyebrow")} title={t("page.title")} description={t("page.description")} />

      {isLoading ? <LoadingState title={t("loading")} description={t("loadingDescription")} /> : null}
      {error ? <ErrorState title={t("error")} /> : null}
      {data ? (
        <>
          <StatsCards statistics={data.statistics} />
          <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_380px]">
            <div className="space-y-5">
              <ReadinessRadar readiness={data.readiness} />
              <Card>
                <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
                  <Clock className="h-4 w-4 shrink-0" />
                  {t("events.title")}
                </div>
                <div className="mt-4 space-y-2">
                  {data.latestEvents.map((event) => (
                    <div key={event.id} className="rounded-md border border-zinc-800 bg-zinc-900/40 px-3 py-2">
                      <div className="flex flex-col gap-1 sm:flex-row sm:items-center sm:justify-between">
                        <div className="break-words text-sm text-zinc-100">{event.message || `${event.eventType} ${objectTypeLabel(t, event.objectType)}`}</div>
                        <div className="shrink-0 text-xs text-zinc-600">{new Date(event.createdAt).toLocaleString()}</div>
                      </div>
                      <div className="mt-1 truncate text-xs text-zinc-500" title={`${event.actor} / ${objectTypeLabel(t, event.objectType)}:${event.objectId ?? "н/д"}`}>
                        {event.actor} / {objectTypeLabel(t, event.objectType)}:{event.objectId ?? "н/д"}
                      </div>
                    </div>
                  ))}
                  {!data.latestEvents.length ? <div className="text-sm text-zinc-500">{t("events.empty")}</div> : null}
                </div>
              </Card>
            </div>
            <AlertsPanel alerts={data.criticalAlerts} problemZones={data.problemZones} />
          </div>
        </>
      ) : null}
    </div>
  )
}
