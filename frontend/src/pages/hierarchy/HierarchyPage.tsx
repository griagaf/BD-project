import { Network } from "lucide-react"
import { useMemo, useState } from "react"
import { useTranslation } from "react-i18next"
import { useFocusTreeQuery, useHierarchyContextQuery, useHierarchyRootsQuery, useObjectPassportQuery } from "@/features/hierarchy/api/hierarchyQueries"
import type { HierarchySelection, TreeMode, TreeNode } from "@/features/hierarchy/model/hierarchyTypes"
import { ActionPanel } from "@/features/hierarchy/ui/ActionPanel"
import { HierarchySidebar } from "@/features/hierarchy/ui/HierarchySidebar"
import { ObjectPassport } from "@/features/hierarchy/ui/ObjectPassport"
import { Card } from "@/shared/ui/card"
import { PageHeader } from "@/shared/ui/page"

export function HierarchyPage() {
  const { t } = useTranslation("hierarchy")
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
      <PageHeader icon={Network} eyebrow={t("page.eyebrow")} title={t("page.title")} description={t("page.description")} />

      {isLoading ? (
        <Card className="text-sm text-zinc-500">{t("page.loading")}</Card>
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
