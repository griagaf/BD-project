import { Eye, Pencil, Trash2 } from "lucide-react"
import { Link } from "react-router-dom"
import { useTranslation } from "react-i18next"
import type { Personnel } from "@/features/personnel/model/personnelTypes"
import { Button } from "@/shared/ui/button"
import { Badge } from "@/shared/ui/badge"
import { cn } from "@/shared/lib/cn"
import { EmptyState } from "@/shared/ui/state"
import { Table, TableShell, tableCellClass, tableHeadClass, tableRowClass } from "@/shared/ui/table"

type PersonnelTableProps = {
  rows: Personnel[]
  canEdit: boolean
  canDelete: boolean
  onEdit: (personnel: Personnel) => void
  onDelete: (id: number) => void
}

export function PersonnelTable({ rows, canEdit, canDelete, onEdit, onDelete }: PersonnelTableProps) {
  const { t } = useTranslation(["common", "personnel"])
  if (!rows.length) {
    return <EmptyState title={t("personnel:table.emptyTitle")} description={t("personnel:table.emptyDescription")} />
  }

  return (
    <TableShell>
      <Table>
        <thead className={tableHeadClass}>
          <tr>
            <th className={tableCellClass}>{t("personnel:table.personnel")}</th>
            <th className={tableCellClass}>{t("personnel:table.rank")}</th>
            <th className={tableCellClass}>{t("personnel:table.unit")}</th>
            <th className={tableCellClass}>{t("personnel:table.specialties")}</th>
            <th className={cn(tableCellClass, "w-36 text-right")}>{t("table.actions")}</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((personnel) => (
            <tr key={personnel.id} className={tableRowClass}>
              <td className={tableCellClass}>
                <div className="max-w-64 truncate font-medium text-zinc-100" title={personnel.fullName}>{personnel.fullName}</div>
                <div className="truncate text-xs text-zinc-500">{personnel.personalNumber}</div>
              </td>
              <td className={cn(tableCellClass, "text-zinc-300")}>{personnel.rank?.name ?? t("personnel:table.noRank")}</td>
              <td className={tableCellClass}>
                <div className="text-zinc-300">{personnel.unitName}</div>
                <div className="text-xs text-zinc-500">{personnel.subdivisionName}</div>
              </td>
              <td className={tableCellClass}>
                <div className="flex flex-wrap gap-1">
                  {personnel.specialties.map((specialty) => (
                    <Badge key={specialty.id}>
                      {specialty.name}
                    </Badge>
                  ))}
                </div>
              </td>
              <td className={tableCellClass}>
                <div className="flex justify-end gap-1">
                  <Link
                    to={`/personnel/${personnel.id}`}
                    className={cn(
                      "inline-flex size-9 items-center justify-center rounded-md text-zinc-400 hover:bg-zinc-900 hover:text-zinc-100",
                    )}
                    title={t("actions.open")}
                  >
                    <Eye className="size-4" />
                  </Link>
                  <Button
                    type="button"
                    variant="ghost"
                    className="size-9 px-0"
                    title={t("actions.edit")}
                    size="icon"
                    disabled={!canEdit}
                    onClick={() => onEdit(personnel)}
                  >
                    <Pencil className="size-4" />
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    className="size-9 px-0"
                    title={t("actions.delete")}
                    size="icon"
                    disabled={!canDelete}
                    onClick={() => onDelete(personnel.id)}
                  >
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
