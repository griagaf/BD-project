import { useEffect, useState } from "react"
import type { InventoryRow } from "@/features/inventory/model/inventoryTypes"
import { Button } from "@/shared/ui/button"

type InventoryDialogProps = {
  row: InventoryRow | null
  open: boolean
  saving: boolean
  onClose: () => void
  onSubmit: (quantity: number) => void
}

export function InventoryDialog({ row, open, saving, onClose, onSubmit }: InventoryDialogProps) {
  const [quantity, setQuantity] = useState(0)

  useEffect(() => {
    if (row) {
      setQuantity(row.quantity)
    }
  }, [row])

  if (!open || !row) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
      <div className="w-full max-w-md rounded-md border border-zinc-800 bg-zinc-950 p-5 shadow-2xl">
        <div className="text-xs uppercase text-emerald-300">Inventory update</div>
        <h2 className="mt-1 text-xl font-semibold text-zinc-100">{row.typeName}</h2>
        <p className="mt-1 text-sm text-zinc-500">{row.unitName}</p>
        <label className="mt-5 block">
          <span className="text-xs uppercase text-zinc-500">Quantity</span>
          <input
            type="number"
            min={0}
            value={quantity}
            onChange={(event) => setQuantity(Number(event.target.value))}
            className="mt-2 h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          />
        </label>
        <div className="mt-5 flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onClose}>Cancel</Button>
          <Button type="button" disabled={saving} onClick={() => onSubmit(quantity)}>Save</Button>
        </div>
      </div>
    </div>
  )
}
