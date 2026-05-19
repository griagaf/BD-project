import { Pencil, Trash2 } from "lucide-react"
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
}

export function BuildingsTable({ rows, canEdit, onEdit, onDelete }: BuildingsTableProps) {
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
                <td className={cn(tableCellClass, "font-semibold text-zinc-100")}>{row.subdivisionsCount}</td>
                <td className={tableCellClass}><StatusBadge status={row.status} /></td>
                <td className={tableCellClass}>
                  <div className="flex justify-end gap-2">
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
