import { Crosshair, GitBranch, Radar } from "lucide-react"
import { useTranslation } from "react-i18next"
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
  const { t } = useTranslation("hierarchy")

  return (
    <Card className="space-y-4">
      <div>
        <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
          <Radar className="h-4 w-4 shrink-0" />
          {t("page.title")}
        </div>
        <h2 className="mt-1 text-lg font-semibold text-zinc-100">{t("tree.navigation")}</h2>
      </div>

      <div className="grid grid-cols-3 gap-2">
        <Button type="button" variant={mode === "STRATEGIC" ? "primary" : "secondary"} className="px-2" onClick={() => onModeChange("STRATEGIC")}>
          <Radar className="h-4 w-4 shrink-0" />
        </Button>
        <Button type="button" variant={mode === "FOCUS" ? "primary" : "secondary"} className="px-2" onClick={() => onModeChange("FOCUS")}>
          <Crosshair className="h-4 w-4 shrink-0" />
        </Button>
        <Button type="button" variant={mode === "CHAIN" ? "primary" : "secondary"} className="px-2" onClick={() => onModeChange("CHAIN")}>
          <GitBranch className="h-4 w-4 shrink-0" />
        </Button>
      </div>

      <StrategicTree roots={roots} selected={selected} onSelect={onSelect} />
    </Card>
  )
}
