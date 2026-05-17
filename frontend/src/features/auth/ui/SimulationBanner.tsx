import { ShieldAlert } from "lucide-react"
import { useAuthStore } from "@/features/auth/model/authStore"

export function SimulationBanner() {
  const simulationRole = useAuthStore((state) => state.simulationRole)
  const simulationObjectType = useAuthStore((state) => state.simulationObjectType)
  const simulationObjectId = useAuthStore((state) => state.simulationObjectId)

  if (!simulationRole) {
    return null
  }

  return (
    <div className="border-b border-amber-500/20 bg-amber-500/10 px-6 py-2 text-sm text-amber-100">
      <div className="mx-auto flex w-full max-w-[1600px] items-center gap-2">
        <ShieldAlert className="size-4" />
        <span className="font-medium">Access Simulation Mode</span>
        <span className="text-amber-200/80">
          viewing as {simulationRole} in {simulationObjectType}:{simulationObjectId}
        </span>
      </div>
    </div>
  )
}
