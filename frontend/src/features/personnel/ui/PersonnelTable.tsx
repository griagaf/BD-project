import { Eye, Pencil, Trash2 } from "lucide-react"
import { Link } from "react-router-dom"
import type { Personnel } from "@/features/personnel/model/personnelTypes"
import { Button } from "@/shared/ui/button"
import { cn } from "@/shared/lib/cn"

type PersonnelTableProps = {
  rows: Personnel[]
  canEdit: boolean
  canDelete: boolean
  onEdit: (personnel: Personnel) => void
  onDelete: (id: number) => void
}

export function PersonnelTable({ rows, canEdit, canDelete, onEdit, onDelete }: PersonnelTableProps) {
  return (
    <div className="overflow-hidden rounded-md border border-zinc-800 bg-zinc-950">
      <table className="w-full border-collapse text-left text-sm">
        <thead className="bg-zinc-900 text-xs uppercase text-zinc-500">
          <tr>
            <th className="px-4 py-3">Personnel</th>
            <th className="px-4 py-3">Rank</th>
            <th className="px-4 py-3">Unit</th>
            <th className="px-4 py-3">Specialties</th>
            <th className="w-36 px-4 py-3 text-right">Actions</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((personnel) => (
            <tr key={personnel.id} className="border-t border-zinc-900">
              <td className="px-4 py-3">
                <div className="font-medium text-zinc-100">{personnel.fullName}</div>
                <div className="text-xs text-zinc-500">{personnel.personalNumber}</div>
              </td>
              <td className="px-4 py-3 text-zinc-300">{personnel.rank?.name ?? "No rank"}</td>
              <td className="px-4 py-3">
                <div className="text-zinc-300">{personnel.unitName}</div>
                <div className="text-xs text-zinc-500">{personnel.subdivisionName}</div>
              </td>
              <td className="px-4 py-3">
                <div className="flex flex-wrap gap-1">
                  {personnel.specialties.map((specialty) => (
                    <span key={specialty.id} className="rounded border border-emerald-500/30 px-2 py-0.5 text-xs text-emerald-300">
                      {specialty.name}
                    </span>
                  ))}
                </div>
              </td>
              <td className="px-4 py-3">
                <div className="flex justify-end gap-1">
                  <Link
                    to={`/personnel/${personnel.id}`}
                    className={cn(
                      "inline-flex size-9 items-center justify-center rounded-md text-zinc-400 hover:bg-zinc-900 hover:text-zinc-100",
                    )}
                    title="Open profile"
                  >
                    <Eye className="size-4" />
                  </Link>
                  <Button
                    type="button"
                    variant="ghost"
                    className="size-9 px-0"
                    title="Edit"
                    disabled={!canEdit}
                    onClick={() => onEdit(personnel)}
                  >
                    <Pencil className="size-4" />
                  </Button>
                  <Button
                    type="button"
                    variant="ghost"
                    className="size-9 px-0"
                    title="Delete"
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
      </table>
    </div>
  )
}
