import { Activity, RadioTower } from "lucide-react"
import type { SmartMissionReport } from "@/features/report/model/reportTypes"
import { Card } from "@/shared/ui/card"
import { RecommendationCards } from "@/features/report/ui/RecommendationCards"
import { ReportSections } from "@/features/report/ui/ReportSections"

export function ReportViewer({ report }: { report: SmartMissionReport }) {
  const readinessAxes = [
    ["Personnel", report.readiness.personnel],
    ["Equipment", report.readiness.equipment],
    ["Weapons", report.readiness.weapons],
    ["Specialists", report.readiness.specialists],
    ["Infrastructure", report.readiness.infrastructure],
  ] as const

  return (
    <Card className="space-y-5 border-emerald-500/20 shadow-[0_0_40px_rgba(16,185,129,0.08)]">
      <header className="flex flex-col gap-4 border-b border-zinc-800 pb-5 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
            <RadioTower className="size-4" />
            Smart Mission Report
          </div>
          <h2 className="mt-2 text-2xl font-semibold text-zinc-100">{report.object.name}</h2>
          <div className="mt-2 text-sm text-zinc-500">
            {report.object.type}:{report.object.id} / {report.object.parentName ?? "No parent"} / {new Date(report.generatedAt).toLocaleString()}
          </div>
          <div className="mt-3 flex flex-wrap gap-2">
            {report.object.path.map((node) => (
              <span key={node} className="rounded border border-zinc-800 bg-zinc-900 px-2 py-1 text-xs text-zinc-400">{node}</span>
            ))}
          </div>
        </div>
        <div className="flex size-28 shrink-0 items-center justify-center rounded-full border border-emerald-500/30 bg-emerald-500/10 shadow-[0_0_36px_rgba(16,185,129,0.18)]">
          <div className="text-center">
            <div className="text-3xl font-semibold text-emerald-200">{report.readiness.overall}</div>
            <div className="text-xs uppercase text-zinc-500">{report.readiness.status}</div>
          </div>
        </div>
      </header>

      <section className="grid gap-3 lg:grid-cols-5">
        {readinessAxes.map(([label, value]) => (
          <div key={label} className="rounded-md border border-zinc-800 bg-zinc-900/50 p-3">
            <div className="flex items-center gap-2 text-xs uppercase text-zinc-500">
              <Activity className="size-3.5 text-emerald-300" />
              {label}
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
