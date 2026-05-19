import { Search } from "lucide-react"
import { useMemo, useState } from "react"
import type { LookupOption } from "@/shared/api/lookupApi"
import { cn } from "@/shared/lib/cn"

type SearchableSelectProps = {
  label: string
  value?: number | null
  options: LookupOption[]
  placeholder: string
  searchPlaceholder?: string
  disabled?: boolean
  required?: boolean
  onChange: (value: number | null) => void
}

export function SearchableSelect({
  label,
  value,
  options,
  placeholder,
  searchPlaceholder,
  disabled,
  required,
  onChange,
}: SearchableSelectProps) {
  const [search, setSearch] = useState("")
  const filtered = useMemo(() => {
    const normalized = search.trim().toLowerCase()
    if (!normalized) {
      return options
    }
    return options.filter((option) =>
      `${option.label} ${option.parentLabel ?? ""} ${option.description ?? ""}`.toLowerCase().includes(normalized),
    )
  }, [options, search])

  return (
    <label className="space-y-2">
      <span className="text-xs uppercase text-zinc-500">{label}</span>
      <div className="rounded-md border border-zinc-800 bg-zinc-900 focus-within:border-emerald-500">
        <div className="relative border-b border-zinc-800">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 shrink-0 -translate-y-1/2 text-zinc-600" />
          <input
            value={search}
            disabled={disabled}
            onChange={(event) => setSearch(event.target.value)}
            placeholder={searchPlaceholder ?? placeholder}
            className="h-10 w-full bg-transparent pl-9 pr-3 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 disabled:cursor-not-allowed disabled:opacity-60"
          />
        </div>
        <select
          value={value ?? ""}
          required={required}
          disabled={disabled}
          onChange={(event) => onChange(event.target.value ? Number(event.target.value) : null)}
          className={cn(
            "h-10 w-full min-w-0 bg-transparent px-3 text-sm text-zinc-100 outline-none",
            disabled && "cursor-not-allowed opacity-60",
          )}
        >
          <option value="">{placeholder}</option>
          {filtered.map((option) => (
            <option key={`${option.type ?? "lookup"}:${option.id}`} value={option.id}>
              {option.parentLabel ? `${option.label} - ${option.parentLabel}` : option.label}
            </option>
          ))}
        </select>
      </div>
    </label>
  )
}
