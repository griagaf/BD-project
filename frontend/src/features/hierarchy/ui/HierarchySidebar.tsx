import { Crosshair, GitBranch, Radar } from "lucide-react"
import type { HierarchySelection, TreeMode, TreeNode } from "@/features/hierarchy/model/hierarchyTypes"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"
import { StrategicTree } from "@/features/hierarchy/ui/StrategicTree"

type HierarchySidebarProps = {
  mode: TreeMode
  roots: TreeNode[]
  selected: HierarchySelection | null
  onModeChange: (mode: TreeMode) => void
  onSelect: (node: TreeNode) => void
}

export function HierarchySidebar({ mode, roots, selected, onModeChange, onSelect }: HierarchySidebarProps) {
  return (
    <Card className="space-y-4">
      <div>
        <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
          <Radar className="size-4" />
          Focus Tree
        </div>
        <h2 className="mt-1 text-lg font-semibold text-zinc-100">Structure Navigation</h2>
      </div>

      <div className="grid grid-cols-3 gap-2">
        <Button type="button" variant={mode === "STRATEGIC" ? "primary" : "secondary"} className="px-2" onClick={() => onModeChange("STRATEGIC")}>
          <Radar className="size-4" />
        </Button>
        <Button type="button" variant={mode === "FOCUS" ? "primary" : "secondary"} className="px-2" onClick={() => onModeChange("FOCUS")}>
          <Crosshair className="size-4" />
        </Button>
        <Button type="button" variant={mode === "CHAIN" ? "primary" : "secondary"} className="px-2" onClick={() => onModeChange("CHAIN")}>
          <GitBranch className="size-4" />
        </Button>
      </div>

      <StrategicTree roots={roots} selected={selected} onSelect={onSelect} />
    </Card>
  )
}
