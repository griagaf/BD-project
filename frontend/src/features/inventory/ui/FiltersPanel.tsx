import { Search } from "lucide-react"
import type { InventoryCategory, InventoryFilter, InventoryType } from "@/features/inventory/model/inventoryTypes"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"

type FiltersPanelProps = {
  filters: InventoryFilter
  categories?: InventoryCategory[]
  types?: InventoryType[]
  onChange: (filters: InventoryFilter) => void
}

export function FiltersPanel({ filters, categories = [], types = [], onChange }: FiltersPanelProps) {
  return (
    <Card className="grid gap-3 lg:grid-cols-[1fr_180px_180px_auto]">
      <label className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 size-4 -translate-y-1/2 text-zinc-600" />
        <input
          value={filters.search ?? ""}
          onChange={(event) => onChange({ ...filters, page: 0, search: event.target.value })}
          placeholder="Search by unit, type, category"
          className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 pl-9 pr-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
        />
      </label>
      <select
        value={filters.categoryId ?? ""}
        onChange={(event) => onChange({ ...filters, page: 0, categoryId: event.target.value ? Number(event.target.value) : undefined })}
        className="h-10 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
      >
        <option value="">All categories</option>
        {categories.map((category) => (
          <option key={category.id} value={category.id}>
            {category.name}
          </option>
        ))}
      </select>
      <select
        value={filters.typeId ?? ""}
        onChange={(event) => onChange({ ...filters, page: 0, typeId: event.target.value ? Number(event.target.value) : undefined })}
        className="h-10 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
      >
        <option value="">All types</option>
        {types.map((type) => (
          <option key={type.id} value={type.id}>
            {type.name}
          </option>
        ))}
      </select>
      <Button type="button" variant="secondary" onClick={() => onChange({ page: 0, size: filters.size ?? 10 })}>
        Reset
      </Button>
    </Card>
  )
}
