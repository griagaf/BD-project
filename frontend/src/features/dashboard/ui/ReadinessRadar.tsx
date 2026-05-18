import type { Readiness } from "@/features/dashboard/model/dashboardTypes"
import { Card } from "@/shared/ui/card"

type ReadinessRadarProps = {
  readiness: Readiness
}

export function ReadinessRadar({ readiness }: ReadinessRadarProps) {
  return (
    <Card className="space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <div className="text-xs uppercase text-emerald-300">Readiness Radar</div>
          <h2 className="mt-1 text-lg font-semibold text-zinc-100">Operational readiness</h2>
        </div>
        <div className="flex size-24 items-center justify-center rounded-full border border-emerald-500/30 bg-emerald-500/10 shadow-[0_0_32px_rgba(16,185,129,0.18)]">
          <span className="text-3xl font-semibold text-emerald-200">{readiness.overall}</span>
        </div>
      </div>
      <div className="space-y-3">
        {readiness.axes.map((axis) => (
          <div key={axis.key}>
            <div className="mb-1 flex justify-between text-xs">
              <span className="uppercase text-zinc-500">{axis.label}</span>
              <span className={axis.score >= 80 ? "text-emerald-300" : axis.score >= 55 ? "text-amber-300" : "text-red-300"}>{axis.score}%</span>
            </div>
            <div className="h-2 overflow-hidden rounded-full bg-zinc-900">
              <div
                className={`h-full rounded-full ${axis.score >= 80 ? "bg-emerald-400" : axis.score >= 55 ? "bg-amber-400" : "bg-red-400"}`}
                style={{ width: `${axis.score}%` }}
              />
            </div>
          </div>
        ))}
      </div>
    </Card>
  )
}
