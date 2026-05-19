import { Activity, RadioTower } from "lucide-react"
import { useTranslation } from "react-i18next"
import type { SmartMissionReport } from "@/features/report/model/reportTypes"
import { Card } from "@/shared/ui/card"
import { RecommendationCards } from "@/features/report/ui/RecommendationCards"
import { ReportSections } from "@/features/report/ui/ReportSections"
import { objectTypeLabel, statusLabel } from "@/shared/i18n/labels"

export function ReportViewer({ report }: { report: SmartMissionReport }) {
  const { t } = useTranslation(["reports", "common"])
  const readinessAxes = [
    [t("readiness.personnel"), report.readiness.personnel],
    [t("readiness.equipment"), report.readiness.equipment],
    [t("readiness.weapons"), report.readiness.weapons],
    [t("readiness.specialists"), report.readiness.specialists],
    [t("readiness.infrastructure"), report.readiness.infrastructure],
  ] as const

  return (
    <Card className="space-y-5 border-emerald-500/20 shadow-[0_0_40px_rgba(16,185,129,0.08)]">
      <header className="flex flex-col gap-4 border-b border-zinc-800 pb-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
            <RadioTower className="h-4 w-4 shrink-0" />
            {t("page.title")}
          </div>
          <h2 className="mt-2 break-words text-2xl font-semibold text-zinc-100">{report.object.name}</h2>
          <div className="mt-2 break-words text-sm text-zinc-500">
            {objectTypeLabel(t, report.object.type)} / {report.object.parentName ?? t("object.noParent")} / {new Date(report.generatedAt).toLocaleString()}
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            {report.object.path.map((node) => (
              <span key={node} className="rounded border border-zinc-800 bg-zinc-900 px-2 py-1 text-xs text-zinc-400">{node}</span>
            ))}
          </div>
        </div>
        <div className="flex h-28 w-28 shrink-0 items-center justify-center rounded-full border border-emerald-500/30 bg-emerald-500/10 shadow-[0_0_36px_rgba(16,185,129,0.18)]">
          <div className="text-center">
            <div className="text-3xl font-semibold text-emerald-200">{report.readiness.overall}</div>
            <div className="text-xs uppercase text-zinc-500">{statusLabel(t, report.readiness.status)}</div>
          </div>
        </div>
      </header>

      <section className="grid gap-3 lg:grid-cols-5">
        {readinessAxes.map(([label, value]) => (
          <div key={label} className="rounded-md border border-zinc-800 bg-zinc-900/50 p-3">
            <div className="flex min-w-0 items-center gap-2 text-xs uppercase text-zinc-500">
              <Activity className="h-4 w-4 shrink-0 text-emerald-300" />
              <span className="truncate" title={label}>{label}</span>
            </div>
            <div className="mt-3 h-2 overflow-hidden rounded-full bg-zinc-950">
              <div className={value >= 80 ? "h-full bg-emerald-400" : value >= 60 ? "h-full bg-amber-400" : "h-full bg-red-400"} style={{ width: `${value}%` }} />
            </div>
            <div className="mt-2 text-lg font-semibold text-zinc-100">{value}%</div>
          </div>
        ))}
      </section>

      <RecommendationCards recommendations={report.recommendations} />
      <ReportSections report={report} />
    </Card>
  )
}
