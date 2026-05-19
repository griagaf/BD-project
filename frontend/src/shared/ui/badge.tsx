import type { HTMLAttributes } from "react"
import { cn } from "@/shared/lib/cn"

type BadgeProps = HTMLAttributes<HTMLSpanElement> & {
  variant?: "default" | "success" | "warning" | "danger" | "info" | "muted"
}

export function Badge({ className, variant = "default", ...props }: BadgeProps) {
  return (
    <span
      className={cn(
        "inline-flex items-center rounded border px-2 py-0.5 text-xs font-medium",
        variant === "default" && "border-emerald-500/30 bg-emerald-500/10 text-emerald-200",
        variant === "success" && "border-emerald-500/30 bg-emerald-500/10 text-emerald-200",
        variant === "warning" && "border-amber-500/30 bg-amber-500/10 text-amber-200",
        variant === "danger" && "border-red-500/30 bg-red-500/10 text-red-200",
        variant === "info" && "border-cyan-500/30 bg-cyan-500/10 text-cyan-200",
        variant === "muted" && "border-zinc-700 bg-zinc-900 text-zinc-400",
        className,
      )}
      {...props}
    />
  )
}
