import { AnimatePresence, motion } from "framer-motion"
import { ChevronDown, ChevronRight, Crosshair, Network } from "lucide-react"
import { useState } from "react"
import { useHierarchyChildrenQuery } from "@/features/hierarchy/api/hierarchyQueries"
import type { HierarchySelection, TreeNode } from "@/features/hierarchy/model/hierarchyTypes"
import { cn } from "@/shared/lib/cn"

type FocusTreeProps = {
  roots: TreeNode[]
  selected: HierarchySelection | null
  onSelect: (node: TreeNode) => void
}

export function FocusTree({ roots, selected, onSelect }: FocusTreeProps) {
  if (!roots.length) {
    return <div className="rounded-md border border-zinc-800 p-4 text-sm text-zinc-500">No hierarchy nodes visible</div>
  }

  return (
    <div className="space-y-2">
      {roots.map((node) => (
        <TreeNodeRow key={node.id} node={node} depth={0} selected={selected} onSelect={onSelect} />
      ))}
    </div>
  )
}

function TreeNodeRow({
  node,
  depth,
  selected,
  onSelect,
}: {
  node: TreeNode
  depth: number
  selected: HierarchySelection | null
  onSelect: (node: TreeNode) => void
}) {
  const [expanded, setExpanded] = useState(depth === 0)
  const { data: children = [], isLoading } = useHierarchyChildrenQuery({ type: node.type, id: node.objectId }, expanded && node.hasChildren)
  const active = selected?.type === node.type && selected.id === node.objectId

  return (
    <div>
      <div
        className={cn(
          "group flex items-center gap-2 rounded-md border px-2 py-2 text-left transition-colors",
          active ? "border-emerald-500/60 bg-emerald-500/10" : "border-zinc-800 bg-zinc-950 hover:bg-zinc-900",
        )}
        style={{ marginLeft: depth * 14 }}
      >
        <button
          type="button"
          className="flex size-6 items-center justify-center rounded text-zinc-500 hover:bg-zinc-800 hover:text-zinc-100"
          disabled={!node.hasChildren}
          onClick={() => setExpanded((value) => !value)}
        >
          {node.hasChildren ? expanded ? <ChevronDown className="size-4" /> : <ChevronRight className="size-4" /> : <Crosshair className="size-3" />}
        </button>
        <button type="button" className="min-w-0 flex-1 text-left" onClick={() => onSelect(node)}>
          <div className="truncate text-sm font-medium text-zinc-100">{node.label}</div>
          <div className="truncate text-xs text-zinc-500">{node.subtitle ?? node.type}</div>
        </button>
        <div className="flex items-center gap-1 text-xs text-zinc-600">
          <Network className="size-3" />
          {node.childrenCount}
        </div>
      </div>

      <AnimatePresence initial={false}>
        {expanded && node.hasChildren ? (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: "auto", opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.16 }}
            className="overflow-hidden pt-2"
          >
            {isLoading ? (
              <div className="py-2 text-xs text-zinc-600" style={{ marginLeft: (depth + 1) * 14 }}>
                Loading children
              </div>
            ) : (
              children.map((child) => (
                <TreeNodeRow key={child.id} node={child} depth={depth + 1} selected={selected} onSelect={onSelect} />
              ))
            )}
          </motion.div>
        ) : null}
      </AnimatePresence>
    </div>
  )
}
