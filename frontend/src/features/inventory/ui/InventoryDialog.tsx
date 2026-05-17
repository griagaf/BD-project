import { useEffect, useState } from "react"
import { useTranslation } from "react-i18next"
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
  const { t } = useTranslation("common")
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
        <div className="text-xs uppercase text-emerald-300">{t("dialog.inventoryUpdate")}</div>
        <h2 className="mt-1 break-words text-xl font-semibold text-zinc-100">{row.typeName}</h2>
        <p className="mt-1 break-words text-sm text-zinc-500">{row.unitName}</p>
        <label className="mt-5 block">
          <span className="text-xs uppercase text-zinc-500">{t("table.quantity")}</span>
          <input
            type="number"
            min={0}
            value={quantity}
            onChange={(event) => setQuantity(Number(event.target.value))}
            className="mt-2 h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          />
        </label>
        <div className="mt-5 flex justify-end gap-2">
          <Button type="button" variant="ghost" onClick={onClose}>{t("actions.cancel")}</Button>
          <Button type="button" disabled={saving} onClick={() => onSubmit(quantity)}>{t("actions.save")}</Button>
        </div>
      </div>
    </div>
  )
}
