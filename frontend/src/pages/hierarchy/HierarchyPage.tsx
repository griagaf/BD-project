import { Network } from "lucide-react"
import { useMemo, useState } from "react"
import { useFocusTreeQuery, useHierarchyContextQuery, useHierarchyRootsQuery, useObjectPassportQuery } from "@/features/hierarchy/api/hierarchyQueries"
import type { HierarchySelection, TreeMode, TreeNode } from "@/features/hierarchy/model/hierarchyTypes"
import { ActionPanel } from "@/features/hierarchy/ui/ActionPanel"
import { HierarchySidebar } from "@/features/hierarchy/ui/HierarchySidebar"
import { ObjectPassport } from "@/features/hierarchy/ui/ObjectPassport"
import { Card } from "@/shared/ui/card"

export function HierarchyPage() {
  const [mode, setMode] = useState<TreeMode>("STRATEGIC")
  const [selection, setSelection] = useState<HierarchySelection | null>(null)
  const { data: roots = [], isLoading } = useHierarchyRootsQuery(mode)
  const { data: focus } = useFocusTreeQuery(mode === "FOCUS" ? selection : null)
  const { data: passport, isLoading: passportLoading } = useObjectPassportQuery(selection)
  const { data: context } = useHierarchyContextQuery(selection)

  const visibleRoots = useMemo(() => {
    if (mode === "FOCUS" && focus?.parentPath.length) {
      return focus.parentPath
    }
    return roots
  }, [focus?.parentPath, mode, roots])

  function selectNode(node: TreeNode) {
    setSelection({ type: node.type, id: node.objectId })
    if (mode === "STRATEGIC") {
      setMode("FOCUS")
    }
  }

  return (
    <div className="space-y-5">
      <div>
        <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
          <Network className="size-4" />
          Tactical hierarchy
        </div>
        <h1 className="mt-1 text-2xl font-semibold text-zinc-100">Focus Tree</h1>
        <p className="mt-1 text-sm text-zinc-500">Lazy-loaded command structure with scope-aware passports.</p>
      </div>

      {isLoading ? (
        <Card className="text-sm text-zinc-500">Loading strategic structure</Card>
      ) : (
        <div className="grid gap-5 xl:grid-cols-[360px_minmax(0,1fr)_300px]">
          <HierarchySidebar
            mode={mode}
            roots={visibleRoots}
            selected={selection}
            onModeChange={setMode}
            onSelect={selectNode}
          />
          <ObjectPassport
            passport={passport}
            loading={passportLoading}
            onSelectBreadcrumb={(node) => setSelection({ type: node.type, id: node.objectId })}
          />
          <ActionPanel selection={selection} context={context} />
        </div>
      )}
    </div>
  )
}
