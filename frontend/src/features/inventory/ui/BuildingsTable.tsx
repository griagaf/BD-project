import { Pencil, Trash2 } from "lucide-react"
import type { BuildingRow } from "@/features/inventory/model/inventoryTypes"
import { StatusBadge } from "@/features/inventory/ui/StatusBadge"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"

type BuildingsTableProps = {
  rows: BuildingRow[]
  canEdit: boolean
  onEdit: (row: BuildingRow) => void
  onDelete: (row: BuildingRow) => void
}

export function BuildingsTable({ rows, canEdit, onEdit, onDelete }: BuildingsTableProps) {
  return (
    <Card className="overflow-hidden p-0">
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-zinc-800 bg-zinc-900/60 text-xs uppercase text-zinc-500">
            <tr>
              <th className="px-4 py-3">Building</th>
              <th className="px-4 py-3">Unit</th>
              <th className="px-4 py-3">Subdivisions</th>
              <th className="px-4 py-3">Status</th>
              <th className="px-4 py-3 text-right">Actions</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={row.id} className="border-b border-zinc-900">
                <td className="px-4 py-3 text-zinc-100">{row.name}</td>
                <td className="px-4 py-3 text-zinc-400">{row.unitName}</td>
                <td className="px-4 py-3 font-semibold text-zinc-100">{row.subdivisionsCount}</td>
                <td className="px-4 py-3"><StatusBadge status={row.status} /></td>
                <td className="px-4 py-3">
                  <div className="flex justify-end gap-2">
                    <Button type="button" variant="ghost" className="h-8 px-2" disabled={!canEdit} onClick={() => onEdit(row)}>
                      <Pencil className="size-4" />
                    </Button>
                    <Button type="button" variant="ghost" className="h-8 px-2" disabled={!canEdit} onClick={() => onDelete(row)}>
                      <Trash2 className="size-4" />
                    </Button>
                  </div>
                </td>
              </tr>
            ))}
            {!rows.length ? (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-zinc-500">No buildings visible</td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </Card>
  )
}
