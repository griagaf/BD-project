import { useEffect, useState } from "react"
import { useTranslation } from "react-i18next"
import { lookupApi, type LookupOption } from "@/shared/api/lookupApi"
import type { InventoryRow } from "@/features/inventory/model/inventoryTypes"
import { Button } from "@/shared/ui/button"
import { SearchableSelect } from "@/shared/ui/searchable-select"

type InventoryDialogProps = {
  row: InventoryRow | null
  open: boolean
  saving: boolean
  unitOptions: LookupOption[]
  typeOptions: LookupOption[]
  initialUnitId?: number | null
  initialTypeId?: number | null
  onClose: () => void
  onSubmit: (request: { unitId: number; typeId: number; quantity: number }) => void
}

export function InventoryDialog({ row, open, saving, unitOptions, typeOptions, initialUnitId, initialTypeId, onClose, onSubmit }: InventoryDialogProps) {
  const { t } = useTranslation("common")
  const [unitId, setUnitId] = useState<number | null>(null)
  const [typeId, setTypeId] = useState<number | null>(null)
  const [quantity, setQuantity] = useState(0)

  useEffect(() => {
    if (row) {
      setUnitId(row.unitId)
      setTypeId(row.typeId)
      setQuantity(row.quantity)
    } else if (open) {
      setUnitId(initialUnitId ?? null)
      setTypeId(initialTypeId ?? null)
      setQuantity(1)
    }
  }, [initialTypeId, initialUnitId, open, row])

  if (!open) {
    return null
  }

  const canSubmit = Boolean(unitId && typeId && quantity >= 0)

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
      <div className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-md border border-zinc-800 bg-zinc-950 p-5 shadow-2xl">
        <div className="text-xs uppercase text-emerald-300">{row ? t("dialog.inventoryUpdate") : t("dialog.inventoryAdd")}</div>
        <h2 className="mt-1 break-words text-xl font-semibold text-zinc-100">{row?.typeName ?? t("dialog.inventoryPosition")}</h2>
        <p className="mt-1 break-words text-sm text-zinc-500">{row?.unitName ?? t("dialog.inventoryPositionDescription")}</p>
        <div className="mt-5 grid gap-4">
          <SearchableSelect
            label={t("fields.unit")}
            value={unitId}
            options={unitOptions}
            disabled={Boolean(row)}
            placeholder={t("placeholders.selectUnit")}
            searchPlaceholder={t("placeholders.searchUnit")}
            loadOptions={(search) => lookupApi.units(search, { limit: 500 })}
            onChange={setUnitId}
          />
          <SearchableSelect
            label={t("fields.resourceType")}
            value={typeId}
            options={typeOptions}
            disabled={Boolean(row)}
            placeholder={t("placeholders.selectResourceType")}
            searchPlaceholder={t("placeholders.searchResourceType")}
            onChange={setTypeId}
          />
        </div>
        <label className="mt-5 block">
          <span className="text-xs uppercase text-zinc-500">{t("table.quantity")}</span>
          <input
            type="number"
            min={0}
            placeholder={t("placeholders.quantity")}
            value={quantity}
            onChange={(event) => setQuantity(Number(event.target.value))}
            className="mt-2 h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          />
        </label>
        <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
          <Button type="button" variant="ghost" onClick={onClose}>{t("actions.cancel")}</Button>
          <Button
            type="button"
            disabled={saving || !canSubmit}
            onClick={() => {
              if (unitId && typeId) {
                onSubmit({ unitId, typeId, quantity })
              }
            }}
          >
            {t("actions.save")}
          </Button>
        </div>
      </div>
    </div>
  )
}
