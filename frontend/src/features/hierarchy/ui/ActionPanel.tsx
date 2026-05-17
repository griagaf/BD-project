import { ExternalLink, FileText, ShieldPlus } from "lucide-react"
import { useNavigate } from "react-router-dom"
import type { HierarchyContext, HierarchySelection } from "@/features/hierarchy/model/hierarchyTypes"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"

type ActionPanelProps = {
  selection: HierarchySelection | null
  context?: HierarchyContext
}

export function ActionPanel({ selection, context }: ActionPanelProps) {
  const navigate = useNavigate()
  const unitPassportEnabled = selection?.type === "MILITARY_UNIT"

  return (
    <div className="space-y-4">
      <Card className="space-y-3">
        <div>
          <div className="text-xs uppercase text-emerald-300">Actions</div>
          <h3 className="mt-1 text-lg font-semibold text-zinc-100">Command Panel</h3>
        </div>
        <div className="space-y-2">
          <Button type="button" variant="secondary" className="w-full justify-start" disabled={!selection}>
            <ShieldPlus className="size-4" />
            Assign commander
          </Button>
          <Button
            type="button"
            variant="secondary"
            className="w-full justify-start"
            disabled={!unitPassportEnabled}
            onClick={() => selection && navigate(`/units/${selection.id}`)}
          >
            <ExternalLink className="size-4" />
            Unit passport
          </Button>
          <Button type="button" variant="ghost" className="w-full justify-start" disabled={!selection}>
            <FileText className="size-4" />
            Tactical report
          </Button>
        </div>
      </Card>

      <Card className="space-y-3">
        <div className="text-xs uppercase text-emerald-300">Statistics</div>
        <div className="grid grid-cols-2 gap-2 text-sm">
          <Stat label="Personnel" value={context?.statistics.personnelCount ?? 0} />
          <Stat label="Readiness" value={`${context?.statistics.readinessScore ?? 0}%`} />
          <Stat label="Units" value={context?.statistics.unitCount ?? 0} />
          <Stat label="Subdivisions" value={context?.statistics.subdivisionCount ?? 0} />
        </div>
      </Card>
    </div>
  )
}

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-md border border-zinc-800 bg-zinc-900/40 p-3">
      <div className="text-xs text-zinc-500">{label}</div>
      <div className="mt-1 text-lg font-semibold text-zinc-100">{value}</div>
    </div>
  )
}
