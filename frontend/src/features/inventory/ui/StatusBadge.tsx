import { AlertTriangle, CheckCircle2, Gauge } from "lucide-react"
import { useTranslation } from "react-i18next"
import { cn } from "@/shared/lib/cn"
import { statusLabel } from "@/shared/i18n/labels"

type StatusBadgeProps = {
  status: string
}

export function StatusBadge({ status }: StatusBadgeProps) {
  const { t } = useTranslation("common")
  const normalized = status.toUpperCase()
  const warning = normalized === "WARNING" || normalized === "OVERLOADED"
  const low = normalized === "LOW"
  const neutral = normalized === "DEPLOYMENT_NOT_APPLICABLE"
  const Icon = warning ? AlertTriangle : low ? Gauge : CheckCircle2

  return (
    <span
      className={cn(
        "inline-flex items-center gap-1 rounded-md border px-2 py-1 text-xs font-medium",
        warning && "border-amber-500/30 bg-amber-500/10 text-amber-300",
        low && "border-sky-500/30 bg-sky-500/10 text-sky-300",
        neutral && "border-zinc-700 bg-zinc-900 text-zinc-400",
        !warning && !low && !neutral && "border-emerald-500/30 bg-emerald-500/10 text-emerald-300",
      )}
    >
      <Icon className="h-4 w-4 shrink-0" />
      <span className="truncate">{statusLabel(t, status)}</span>
    </span>
  )
}
