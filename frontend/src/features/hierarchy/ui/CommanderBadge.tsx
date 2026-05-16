import { ShieldCheck } from "lucide-react"
import type { Commander } from "@/features/hierarchy/model/hierarchyTypes"

type CommanderBadgeProps = {
  commander: Commander | null
}

export function CommanderBadge({ commander }: CommanderBadgeProps) {
  return (
    <div className="flex items-center gap-3 rounded-md border border-zinc-800 bg-zinc-950/80 px-3 py-2">
      <div className="flex size-9 items-center justify-center rounded-md bg-emerald-500/10 text-emerald-300">
        <ShieldCheck className="size-4" />
      </div>
      <div className="min-w-0">
        <div className="truncate text-sm font-medium text-zinc-100">{commander?.fullName ?? "No commander assigned"}</div>
        <div className="truncate text-xs text-zinc-500">{commander?.rankName ?? "Commander slot"}</div>
      </div>
    </div>
  )
}
