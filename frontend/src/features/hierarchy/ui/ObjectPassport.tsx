import { Activity, Boxes, Network, Users } from "lucide-react"
import type { ReactNode } from "react"
import type { ObjectPassport as ObjectPassportType, TreeNode } from "@/features/hierarchy/model/hierarchyTypes"
import { Card } from "@/shared/ui/card"
import { BreadcrumbNavigation } from "@/features/hierarchy/ui/BreadcrumbNavigation"
import { CommanderBadge } from "@/features/hierarchy/ui/CommanderBadge"

type ObjectPassportProps = {
  passport?: ObjectPassportType
  loading?: boolean
  onSelectBreadcrumb?: (node: TreeNode) => void
}

export function ObjectPassport({ passport, loading, onSelectBreadcrumb }: ObjectPassportProps) {
  if (loading) {
    return <Card className="min-h-[360px] text-sm text-zinc-500">Loading object passport</Card>
  }

  if (!passport) {
    return <Card className="min-h-[360px] text-sm text-zinc-500">Select hierarchy object</Card>
  }

  const metrics = passport.metrics

  return (
    <Card className="min-h-[360px] space-y-5">
      <div className="space-y-3">
        <BreadcrumbNavigation nodes={passport.breadcrumbs} onSelect={onSelectBreadcrumb} />
        <div>
          <div className="text-xs uppercase text-emerald-300">{passport.objectType.replaceAll("_", " ")}</div>
          <h2 className="mt-1 text-2xl font-semibold text-zinc-100">{passport.name}</h2>
          <p className="mt-1 text-sm text-zinc-500">{passport.subtitle ?? "Operational object"}</p>
        </div>
      </div>

      <CommanderBadge commander={passport.commander} />

      <div className="grid gap-3 md:grid-cols-4">
        <Metric icon={<Users className="size-4" />} label="Personnel" value={metrics.personnelCount} />
        <Metric icon={<Boxes className="size-4" />} label="Units" value={metrics.unitCount} />
        <Metric icon={<Network className="size-4" />} label="Subdivisions" value={metrics.subdivisionCount} />
        <Metric icon={<Activity className="size-4" />} label="Readiness" value={`${metrics.readinessScore}%`} />
      </div>

      <div className="grid gap-2 text-sm md:grid-cols-2">
        {Object.entries(passport.details).map(([key, value]) => (
          <div key={key} className="rounded-md border border-zinc-800 bg-zinc-900/40 px-3 py-2">
            <div className="text-xs uppercase text-zinc-600">{key}</div>
            <div className="mt-1 text-zinc-200">{String(value ?? "n/a")}</div>
          </div>
        ))}
      </div>
    </Card>
  )
}

function Metric({ icon, label, value }: { icon: ReactNode; label: string; value: ReactNode }) {
  return (
    <div className="rounded-md border border-zinc-800 bg-zinc-900/40 p-3">
      <div className="flex items-center gap-2 text-xs uppercase text-zinc-500">
        <span className="text-emerald-300">{icon}</span>
        {label}
      </div>
      <div className="mt-2 text-xl font-semibold text-zinc-100">{value}</div>
    </div>
  )
}
