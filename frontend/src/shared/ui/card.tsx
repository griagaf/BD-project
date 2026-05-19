import type { HTMLAttributes } from "react"
import { cn } from "@/shared/lib/cn"

type CardProps = HTMLAttributes<HTMLDivElement> & {
  interactive?: boolean
}

export function Card({ className, interactive = false, ...props }: CardProps) {
  return (
    <div
      className={cn(
        "rounded-md border border-zinc-800/90 bg-zinc-950/80 p-5 shadow-[inset_0_1px_0_rgba(255,255,255,0.03)] backdrop-blur",
        interactive && "transition-all hover:border-emerald-500/30 hover:bg-zinc-900/60 hover:shadow-[0_0_32px_rgba(16,185,129,0.08)]",
        className,
      )}
      {...props}
    />
  )
}
