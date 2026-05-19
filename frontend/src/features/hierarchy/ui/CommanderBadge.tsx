import { ShieldCheck } from "lucide-react"
import { useTranslation } from "react-i18next"
import type { Commander } from "@/features/hierarchy/model/hierarchyTypes"

type CommanderBadgeProps = {
  commander: Commander | null
}

export function CommanderBadge({ commander }: CommanderBadgeProps) {
  const { t } = useTranslation("hierarchy")

  return (
    <div className="flex items-center gap-3 rounded-md border border-zinc-800 bg-zinc-950/80 px-3 py-2">
      <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md bg-emerald-500/10 text-emerald-300">
        <ShieldCheck className="h-4 w-4 shrink-0" />
      </div>
      <div className="min-w-0">
        <div className="truncate text-sm font-medium text-zinc-100" title={commander?.fullName ?? t("passport.noCommander")}>{commander?.fullName ?? t("passport.noCommander")}</div>
        <div className="truncate text-xs text-zinc-500" title={commander?.rankName ?? t("passport.commanderSlot")}>{commander?.rankName ?? t("passport.commanderSlot")}</div>
      </div>
    </div>
  )
}
