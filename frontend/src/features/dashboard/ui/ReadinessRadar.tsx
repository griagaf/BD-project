import type { Readiness } from "@/features/dashboard/model/dashboardTypes"
import { Card } from "@/shared/ui/card"
import { useTranslation } from "react-i18next"
import { ChevronDown, Info } from "lucide-react"
import { useState } from "react"
import { cn } from "@/shared/lib/cn"

type ReadinessRadarProps = {
  readiness: Readiness
}

export function ReadinessRadar({ readiness }: ReadinessRadarProps) {
  const { t } = useTranslation("dashboard")
  const [criteriaOpen, setCriteriaOpen] = useState(false)
  const criteria = ["personnel", "equipment", "weapons", "specialists", "infrastructure", "alerts"]
  return (
    <Card className="space-y-5">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
        <div className="min-w-0">
          <div className="text-xs uppercase text-emerald-300">{t("readiness.eyebrow")}</div>
          <h2 className="mt-1 break-words text-lg font-semibold text-zinc-100">{t("readiness.title")}</h2>
        </div>
        <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-full border border-emerald-500/30 bg-emerald-500/10 shadow-[0_0_32px_rgba(16,185,129,0.18)]">
          <span className="text-3xl font-semibold text-emerald-200">{readiness.overall}</span>
        </div>
      </div>
      <div className="space-y-3">
        {readiness.axes.map((axis) => (
          <div key={axis.key}>
            <div className="mb-1 flex justify-between text-xs">
              <span className="truncate uppercase text-zinc-500" title={axis.label}>{axis.label}</span>
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
      <div className="rounded-md border border-zinc-800 bg-zinc-900/50">
        <button
          type="button"
          onClick={() => setCriteriaOpen((current) => !current)}
          className="flex w-full items-center justify-between gap-3 px-4 py-3 text-left"
        >
          <span className="flex min-w-0 items-center gap-2 text-sm font-medium text-zinc-100">
            <Info className="h-4 w-4 shrink-0 text-emerald-300" />
            <span className="truncate">{t("readiness.criteria.title")}</span>
          </span>
          <ChevronDown className={cn("h-4 w-4 shrink-0 text-zinc-500 transition", criteriaOpen && "rotate-180")} />
        </button>
        {criteriaOpen ? (
          <div className="grid gap-2 border-t border-zinc-800 p-4 md:grid-cols-2">
            {criteria.map((item) => (
              <div key={item} className="rounded border border-zinc-800 bg-zinc-950 px-3 py-2">
                <div className="truncate text-xs uppercase text-emerald-300" title={t(`readiness.criteria.${item}.title`)}>
                  {t(`readiness.criteria.${item}.title`)}
                </div>
                <p className="mt-1 break-words text-xs leading-5 text-zinc-400">
                  {t(`readiness.criteria.${item}.description`)}
                </p>
              </div>
            ))}
          </div>
        ) : null}
      </div>
    </Card>
  )
}
