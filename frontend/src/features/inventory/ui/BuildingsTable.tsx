import { Pencil, Trash2 } from "lucide-react"
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
}

export function BuildingsTable({ rows, canEdit, onEdit, onDelete }: BuildingsTableProps) {
  if (!rows.length) {
    return <EmptyState title="No buildings visible" description="Current filters and command scope returned no building records." />
  }

  return (
    <TableShell>
        <Table>
          <thead className={tableHeadClass}>
            <tr>
              <th className={tableCellClass}>Building</th>
              <th className={tableCellClass}>Unit</th>
              <th className={tableCellClass}>Subdivisions</th>
              <th className={tableCellClass}>Status</th>
              <th className={cn(tableCellClass, "text-right")}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id} className={tableRowClass}>
                <td className={cn(tableCellClass, "text-zinc-100")}>{row.name}</td>
                <td className={cn(tableCellClass, "text-zinc-400")}>{row.unitName}</td>
                <td className={cn(tableCellClass, "font-semibold text-zinc-100")}>{row.subdivisionsCount}</td>
                <td className={tableCellClass}><StatusBadge status={row.status} /></td>
                <td className={tableCellClass}>
                  <div className="flex justify-end gap-2">
                    <Button type="button" variant="ghost" size="icon" disabled={!canEdit} onClick={() => onEdit(row)}>
                      <Pencil className="size-4" />
                    </Button>
                    <Button type="button" variant="ghost" size="icon" disabled={!canEdit} onClick={() => onDelete(row)}>
                      <Trash2 className="size-4" />
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
