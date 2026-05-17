import { Pencil, Trash2 } from "lucide-react"
import type { InventoryRow } from "@/features/inventory/model/inventoryTypes"
import { Button } from "@/shared/ui/button"
import { StatusBadge } from "@/features/inventory/ui/StatusBadge"
import { EmptyState } from "@/shared/ui/state"
import { Table, TableShell, tableCellClass, tableHeadClass, tableRowClass } from "@/shared/ui/table"
import { cn } from "@/shared/lib/cn"

type InventoryTableProps = {
  rows: InventoryRow[]
  canEdit: boolean
  onEdit: (row: InventoryRow) => void
  onDelete: (row: InventoryRow) => void
}

export function InventoryTable({ rows, canEdit, onEdit, onDelete }: InventoryTableProps) {
  if (!rows.length) {
    return <EmptyState title="No inventory rows visible" description="Current filters and command scope returned no inventory records." />
  }

  return (
    <TableShell>
        <Table>
          <thead className={tableHeadClass}>
            <tr>
              <th className={tableCellClass}>Unit</th>
              <th className={tableCellClass}>Category</th>
              <th className={tableCellClass}>Type</th>
              <th className={tableCellClass}>Quantity</th>
              <th className={tableCellClass}>Status</th>
              <th className={cn(tableCellClass, "text-right")}>Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={`${row.unitId}:${row.typeId}`} className={tableRowClass}>
                <td className={cn(tableCellClass, "text-zinc-100")}>{row.unitName}</td>
                <td className={cn(tableCellClass, "text-zinc-400")}>{row.categoryName}</td>
                <td className={cn(tableCellClass, "text-zinc-300")}>{row.typeName}</td>
                <td className={cn(tableCellClass, "font-semibold text-zinc-100")}>{row.quantity}</td>
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
