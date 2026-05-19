import { ShieldAlert } from "lucide-react"
import { useTranslation } from "react-i18next"
import { useAuthStore } from "@/features/auth/model/authStore"
import { objectTypeLabel, roleLabel } from "@/shared/i18n/labels"

export function SimulationBanner() {
  const { t } = useTranslation(["security", "common"])
  const simulationRole = useAuthStore((state) => state.simulationRole)
  const simulationObjectType = useAuthStore((state) => state.simulationObjectType)
  const simulationObjectId = useAuthStore((state) => state.simulationObjectId)

  if (!simulationRole) {
    return null
  }

  return (
    <div className="border-b border-amber-500/20 bg-amber-500/10 px-6 py-2 text-sm text-amber-100">
      <div className="mx-auto flex w-full max-w-[1600px] items-center gap-2">
        <ShieldAlert className="h-4 w-4 shrink-0" />
        <span className="shrink-0 font-medium">{t("security:simulation.bannerTitle")}</span>
        <span className="min-w-0 truncate text-amber-200/80">
          {t("security:simulation.bannerDescription", { role: roleLabel(t, simulationRole), scope: `${objectTypeLabel(t, simulationObjectType)}:${simulationObjectId}` })}
        </span>
      </div>
    </div>
  )
}
