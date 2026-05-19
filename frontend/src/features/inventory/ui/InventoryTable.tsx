import { Eye, Pencil, Trash2 } from "lucide-react"
import { useTranslation } from "react-i18next"
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
  onOpenType?: (typeId: number) => void
}

export function InventoryTable({ rows, canEdit, onEdit, onDelete, onOpenType }: InventoryTableProps) {
  const { t } = useTranslation(["common", "equipment"])
  if (!rows.length) {
    return <EmptyState title={t("equipment:table.emptyTitle")} description={t("equipment:table.emptyDescription")} />
  }

  return (
    <TableShell>
        <Table>
          <thead className={tableHeadClass}>
            <tr>
              <th className={tableCellClass}>{t("table.unit")}</th>
              <th className={tableCellClass}>{t("table.category")}</th>
              <th className={tableCellClass}>{t("table.type")}</th>
              <th className={tableCellClass}>{t("table.quantity")}</th>
              <th className={tableCellClass}>{t("table.status")}</th>
              <th className={cn(tableCellClass, "text-right")}>{t("table.actions")}</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={`${row.unitId}:${row.typeId}`} className={tableRowClass}>
                <td className={cn(tableCellClass, "text-zinc-100")}>
                  <span className="block max-w-64 truncate" title={row.unitName}>{row.unitName}</span>
                </td>
                <td className={cn(tableCellClass, "text-zinc-400")}>
                  <span className="block max-w-48 truncate" title={row.categoryName}>{row.categoryName}</span>
                </td>
                <td className={cn(tableCellClass, "text-zinc-300")}>
                  <span className="block max-w-56 truncate" title={row.typeName}>{row.typeName}</span>
                </td>
                <td className={cn(tableCellClass, "font-semibold text-zinc-100")}>{row.quantity}</td>
                <td className={tableCellClass}><StatusBadge status={row.status} /></td>
                <td className={tableCellClass}>
                  <div className="flex justify-end gap-2">
                    <Button type="button" variant="ghost" size="icon" disabled={!canEdit} onClick={() => onEdit(row)}>
                      <Pencil className="h-4 w-4 shrink-0" />
                    </Button>
                    <Button type="button" variant="ghost" size="icon" onClick={() => onOpenType?.(row.typeId)}>
                      <Eye className="h-4 w-4 shrink-0" />
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
