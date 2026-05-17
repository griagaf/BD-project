import { Search, SlidersHorizontal } from "lucide-react"
import type { PersonnelFilter, Specialty } from "@/features/personnel/model/personnelTypes"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"

type PersonnelFiltersProps = {
  filters: PersonnelFilter
  specialties: Specialty[]
  onChange: (filters: PersonnelFilter) => void
}

export function PersonnelFilters({ filters, specialties, onChange }: PersonnelFiltersProps) {
  return (
    <Card className="grid gap-3 md:grid-cols-[1.4fr_0.7fr_0.7fr_0.8fr_auto] md:items-end">
      <label className="space-y-2">
        <span className="text-xs uppercase text-zinc-500">Search</span>
        <div className="flex items-center gap-2 rounded-md border border-zinc-800 bg-zinc-900 px-3">
          <Search className="size-4 text-zinc-500" />
          <input
            className="h-10 flex-1 bg-transparent text-sm outline-none placeholder:text-zinc-600"
            value={filters.search ?? ""}
            onChange={(event) => onChange({ ...filters, search: event.target.value, page: 0 })}
            placeholder="Name or personal number"
          />
        </div>
      </label>

      <label className="space-y-2">
        <span className="text-xs uppercase text-zinc-500">Unit</span>
        <input
          className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm outline-none"
          value={filters.unitId ?? ""}
          onChange={(event) => onChange({ ...filters, unitId: event.target.value, page: 0 })}
          placeholder="Unit ID"
        />
      </label>

      <label className="space-y-2">
        <span className="text-xs uppercase text-zinc-500">Subdivision</span>
        <input
          className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm outline-none"
          value={filters.subdivisionId ?? ""}
          onChange={(event) => onChange({ ...filters, subdivisionId: event.target.value, page: 0 })}
          placeholder="Subdivision ID"
        />
      </label>

      <label className="space-y-2">
        <span className="text-xs uppercase text-zinc-500">Specialty</span>
        <select
          className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm outline-none"
          value={filters.specialtyId ?? ""}
          onChange={(event) => onChange({ ...filters, specialtyId: event.target.value, page: 0 })}
        >
          <option value="">All</option>
          {specialties.map((specialty) => (
            <option key={specialty.id} value={specialty.id}>
              {specialty.name}
            </option>
          ))}
        </select>
      </label>

      <Button
        type="button"
        variant="secondary"
        onClick={() => onChange({ page: 0, size: filters.size ?? 10, sort: filters.sort ?? "lastName,asc" })}
      >
        <SlidersHorizontal className="size-4" />
        Reset
      </Button>
    </Card>
  )
}
