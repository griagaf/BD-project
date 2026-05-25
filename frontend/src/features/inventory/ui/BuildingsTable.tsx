import { Link2, Pencil, Trash2, X } from "lucide-react"
import { useTranslation } from "react-i18next"
import type { BuildingRow } from "@/features/inventory/model/inventoryTypes"
import { StatusBadge } from "@/features/inventory/ui/StatusBadge"
import { Button } from "@/shared/ui/button"
import { EmptyState } from "@/shared/ui/state"
import { Table, TableShell, tableCellClass, tableHeadClass, tableRowClass } from "@/shared/ui/table"
import { cn } from "@/shared/lib/cn"

type BuildingsTableProps = {
  rows: BuildingRow[]
  canEdit: boolean
  onEdit: (row: BuildingRow) => void
  onDelete: (row: BuildingRow) => void
  onAssign: (row: BuildingRow) => void
  onRemoveAssignment: (row: BuildingRow, subdivisionId: number) => void
}

export function BuildingsTable({ rows, canEdit, onEdit, onDelete, onAssign, onRemoveAssignment }: BuildingsTableProps) {
  const { t } = useTranslation(["common", "buildings"])
  if (!rows.length) {
    return <EmptyState title={t("buildings:table.emptyTitle")} description={t("buildings:table.emptyDescription")} />
  }

  return (
    <TableShell>
        <Table>
          <thead className={tableHeadClass}>
            <tr>
              <th className={tableCellClass}>{t("buildings:table.building")}</th>
              <th className={tableCellClass}>{t("buildings:table.unit")}</th>
              <th className={tableCellClass}>{t("buildings:table.subdivisions")}</th>
              <th className={tableCellClass}>{t("buildings:table.assignmentMode")}</th>
              <th className={tableCellClass}>{t("table.status")}</th>
              <th className={cn(tableCellClass, "text-right")}>{t("table.actions")}</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id} className={tableRowClass}>
                <td className={cn(tableCellClass, "text-zinc-100")}>
                  <span className="block max-w-64 truncate" title={row.name}>{row.name}</span>
                </td>
                <td className={cn(tableCellClass, "text-zinc-400")}>
                  <span className="block max-w-64 truncate" title={row.unitName}>{row.unitName}</span>
                </td>
                <td className={cn(tableCellClass, "min-w-64 text-zinc-300")}>
                  {row.assignedSubdivisions?.length ? (
                    <div className="flex max-w-md flex-wrap gap-1.5">
                      {row.assignedSubdivisions.slice(0, 4).map((subdivision) => (
                        <span key={subdivision.id} className="inline-flex max-w-full items-center gap-1 rounded border border-zinc-800 bg-zinc-950 px-2 py-1 text-xs">
                          <span className="max-w-40 truncate" title={`${subdivision.unitName} / ${subdivision.type} / ${subdivision.name}`}>{subdivision.name}</span>
                          <button
                            type="button"
                            disabled={!canEdit}
                            className="text-zinc-500 hover:text-red-300 disabled:pointer-events-none disabled:opacity-40"
                            onClick={() => onRemoveAssignment(row, subdivision.id)}
                          >
                            <X className="h-3 w-3 shrink-0" />
                          </button>
                        </span>
                      ))}
                      {row.assignedSubdivisions.length > 4 ? (
                        <span className="rounded border border-zinc-800 bg-zinc-950 px-2 py-1 text-xs text-zinc-500">+{row.assignedSubdivisions.length - 4}</span>
                      ) : null}
                    </div>
                  ) : (
                    <span className="text-zinc-600">{t("buildings:table.noAssignments")}</span>
                  )}
                </td>
                <td className={tableCellClass}>
                  <span className={cn(
                    "inline-flex items-center rounded border px-2 py-1 text-xs",
                    row.assignable ? "border-emerald-500/30 bg-emerald-500/10 text-emerald-200" : "border-zinc-700 bg-zinc-900 text-zinc-400",
                  )}>
                    {row.assignable ? t("buildings:table.assignable") : t("buildings:table.notAssignable")}
                  </span>
                </td>
                <td className={tableCellClass}><StatusBadge status={row.status} /></td>
                <td className={tableCellClass}>
                  <div className="flex justify-end gap-2">
                    <Button type="button" variant="ghost" size="icon" disabled={!canEdit || !row.assignable} onClick={() => onAssign(row)}>
                      <Link2 className="h-4 w-4 shrink-0" />
                    </Button>
                    <Button type="button" variant="ghost" size="icon" disabled={!canEdit} onClick={() => onEdit(row)}>
                      <Pencil className="h-4 w-4 shrink-0" />
                    </Button>
                    <Button type="button" variant="ghost" size="icon" disabled={!canEdit} onClick={() => onDelete(row)}>
                      <Trash2 className="h-4 w-4 shrink-0" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </Table>
    </TableShell>
  )
}
