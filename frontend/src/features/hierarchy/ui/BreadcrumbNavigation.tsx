import { ChevronRight } from "lucide-react"
import type { TreeNode } from "@/features/hierarchy/model/hierarchyTypes"

type BreadcrumbNavigationProps = {
  nodes: TreeNode[]
  onSelect?: (node: TreeNode) => void
}

export function BreadcrumbNavigation({ nodes, onSelect }: BreadcrumbNavigationProps) {
  if (!nodes.length) {
    return null
  }

  return (
    <div className="flex flex-wrap items-center gap-1 text-xs text-zinc-500">
      {nodes.map((node, index) => (
        <div key={node.id} className="flex items-center gap-1">
          <button
            type="button"
            className="rounded px-1.5 py-1 text-zinc-400 transition-colors hover:bg-zinc-900 hover:text-emerald-300"
            onClick={() => onSelect?.(node)}
          >
            {node.label}
          </button>
          {index < nodes.length - 1 ? <ChevronRight className="h-3 w-3 shrink-0 text-zinc-700" /> : null}
        </div>
      ))}
    </div>
  )
}
