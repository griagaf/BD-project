import { AlertTriangle, CheckCircle2, Info } from "lucide-react"
import type { ReportRecommendation } from "@/features/report/model/reportTypes"
import { cn } from "@/shared/lib/cn"

const severityClass: Record<ReportRecommendation["severity"], string> = {
  INFO: "border-emerald-500/25 bg-emerald-500/10 text-emerald-200",
  MEDIUM: "border-amber-500/25 bg-amber-500/10 text-amber-200",
  HIGH: "border-orange-500/25 bg-orange-500/10 text-orange-200",
  CRITICAL: "border-red-500/25 bg-red-500/10 text-red-200",
}

export function RecommendationCards({ recommendations }: { recommendations: ReportRecommendation[] }) {
  return (
    <div className="grid gap-3 lg:grid-cols-2">
      {recommendations.map((recommendation) => {
        const Icon = recommendation.severity === "INFO" ? CheckCircle2 : recommendation.severity === "MEDIUM" ? Info : AlertTriangle
        return (
          <a
            key={recommendation.code}
            href={recommendation.actionRoute}
            className={cn("rounded-md border p-4 transition-colors hover:bg-zinc-900/80", severityClass[recommendation.severity])}
          >
            <div className="flex items-start gap-3">
              <Icon className="mt-0.5 size-4 shrink-0" />
              <div>
                <div className="text-sm font-semibold">{recommendation.title}</div>
                <p className="mt-1 text-sm text-zinc-400">{recommendation.description}</p>
                <div className="mt-3 text-xs uppercase text-zinc-500">{recommendation.actionLabel}</div>
              </div>
            </div>
          </a>
        )
      })}
    </div>
  )
}
