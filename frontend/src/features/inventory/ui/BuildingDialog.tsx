import { useEffect, useState } from "react"
import type { BuildingRow } from "@/features/inventory/model/inventoryTypes"
import { Button } from "@/shared/ui/button"

type BuildingDialogProps = {
  row: BuildingRow | null
  open: boolean
  saving: boolean
  onClose: () => void
  onSubmit: (request: { name: string; unitId: number }) => void
}

export function BuildingDialog({ row, open, saving, onClose, onSubmit }: BuildingDialogProps) {
  const [name, setName] = useState("")
  const [unitId, setUnitId] = useState(1)

  useEffect(() => {
    if (row) {
      setName(row.name)
      setUnitId(row.unitId)
    } else {
      setName("")
      setUnitId(1)
    }
  }, [row, open])

  if (!open) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
      <div className="w-full max-w-md rounded-md border border-zinc-800 bg-zinc-950 p-5 shadow-2xl">
        <div className="text-xs uppercase text-emerald-300">Building registry</div>
        <h2 className="mt-1 text-xl font-semibold text-zinc-100">{row ? "Edit building" : "Create building"}</h2>
        <div className="mt-5 space-y-4">
          <label className="block">
            <span className="text-xs uppercase text-zinc-500">Name</span>
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              className="mt-2 h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
            />
          </label>
          <label className="block">
            <span className="text-xs uppercase text-zinc-500">Unit ID</span>
            <input
              type="number"
              min={1}
              value={unitId}
              onChange={(event) => setUnitId(Number(event.target.value))}
              className="mt-2 h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
            />
          </label>
        </div>
        <div className="mt-5 flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onClose}>Cancel</Button>
          <Button type="button" disabled={saving || !name.trim()} onClick={() => onSubmit({ name, unitId })}>Save</Button>
        </div>
      </div>
    </div>
  )
}
