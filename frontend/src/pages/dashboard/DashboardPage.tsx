import { Activity, Clock } from "lucide-react"
import { useDashboardQuery } from "@/features/dashboard/api/dashboardQueries"
import { AlertsPanel } from "@/features/dashboard/ui/AlertsPanel"
import { ReadinessRadar } from "@/features/dashboard/ui/ReadinessRadar"
import { StatsCards } from "@/features/dashboard/ui/StatsCards"
import { Card } from "@/shared/ui/card"
import { PageHeader } from "@/shared/ui/page"
import { ErrorState, LoadingState } from "@/shared/ui/state"

export function DashboardPage() {
  const { data, isLoading, error } = useDashboardQuery()

  return (
    <div className="space-y-5">
      <PageHeader icon={Activity} eyebrow="Operational overview" title="Tactical Dashboard" description="Scoped command summary, alerts and readiness posture." />

      {isLoading ? <LoadingState title="Loading tactical dashboard" description="Compiling scoped command metrics" /> : null}
      {error ? <ErrorState title="Unable to load dashboard" /> : null}
      {data ? (
        <>
          <StatsCards statistics={data.statistics} />
          <div className="grid gap-5 xl:grid-cols-[minmax(0,1fr)_380px]">
            <div className="space-y-5">
              <ReadinessRadar readiness={data.readiness} />
              <Card>
                <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
                  <Clock className="size-4" />
                  Latest events
                </div>
                <div className="mt-4 space-y-2">
                  {data.latestEvents.map((event) => (
                    <div key={event.id} className="rounded-md border border-zinc-800 bg-zinc-900/40 px-3 py-2">
                      <div className="flex items-center justify-between gap-3">
                        <div className="text-sm text-zinc-100">{event.message || `${event.eventType} ${event.objectType}`}</div>
                        <div className="text-xs text-zinc-600">{new Date(event.createdAt).toLocaleString()}</div>
                      </div>
                      <div className="mt-1 text-xs text-zinc-500">{event.actor} / {event.objectType}:{event.objectId ?? "n/a"}</div>
                    </div>
                  ))}
                  {!data.latestEvents.length ? <div className="text-sm text-zinc-500">No audit events in current scope</div> : null}
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
