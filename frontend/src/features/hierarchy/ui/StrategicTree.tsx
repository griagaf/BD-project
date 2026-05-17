import type { HierarchySelection, TreeNode } from "@/features/hierarchy/model/hierarchyTypes"
import { FocusTree } from "@/features/hierarchy/ui/FocusTree"

type StrategicTreeProps = {
  roots: TreeNode[]
  selected: HierarchySelection | null
  onSelect: (node: TreeNode) => void
}

export function StrategicTree(props: StrategicTreeProps) {
  return <FocusTree {...props} />
}
