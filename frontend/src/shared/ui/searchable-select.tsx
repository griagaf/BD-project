import { Check, ChevronDown, Search } from "lucide-react"
import type { KeyboardEvent } from "react"
import { useEffect, useMemo, useRef, useState } from "react"
import { createPortal } from "react-dom"
import { useTranslation } from "react-i18next"
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
  loadOptions?: (search: string) => Promise<LookupOption[]>
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
  loadOptions,
  onChange,
}: SearchableSelectProps) {
  const { t } = useTranslation("common")
  const rootRef = useRef<HTMLLabelElement | null>(null)
  const buttonRef = useRef<HTMLButtonElement | null>(null)
  const dropdownRef = useRef<HTMLDivElement | null>(null)
  const searchRef = useRef<HTMLInputElement | null>(null)
  const [open, setOpen] = useState(false)
  const [search, setSearch] = useState("")
  const [activeIndex, setActiveIndex] = useState(0)
  const [dropdownStyle, setDropdownStyle] = useState({ top: 0, left: 0, width: 0 })
  const [remoteOptions, setRemoteOptions] = useState<LookupOption[] | null>(null)
  const [selectedCache, setSelectedCache] = useState<LookupOption | null>(null)
  const [loading, setLoading] = useState(false)
  const sourceOptions = remoteOptions ?? options
  const selected = useMemo(
    () => sourceOptions.find((option) => option.id === value)
      ?? options.find((option) => option.id === value)
      ?? (selectedCache?.id === value ? selectedCache : null),
    [options, selectedCache, sourceOptions, value],
  )
  const filtered = useMemo(() => {
    if (loadOptions) {
      return sourceOptions
    }
    const normalized = search.trim().toLowerCase()
    if (!normalized) {
      return sourceOptions
    }
    return sourceOptions.filter((option) =>
      `${option.label} ${option.parentLabel ?? ""} ${option.description ?? ""}`.toLowerCase().includes(normalized),
    )
  }, [loadOptions, sourceOptions, search])
  const visible = filtered.slice(0, 80)

  useEffect(() => {
    if (!open || !loadOptions) {
      return
    }
    let cancelled = false
    const timeoutId = window.setTimeout(() => {
      setLoading(true)
      loadOptions(search)
        .then((nextOptions) => {
          if (!cancelled) {
            setRemoteOptions(nextOptions)
            setActiveIndex(0)
          }
        })
        .finally(() => {
          if (!cancelled) {
            setLoading(false)
          }
        })
    }, 180)
    return () => {
      cancelled = true
      window.clearTimeout(timeoutId)
    }
  }, [loadOptions, open, search])

  useEffect(() => {
    function handlePointerDown(event: MouseEvent) {
      const target = event.target as Node
      if (!rootRef.current?.contains(target) && !dropdownRef.current?.contains(target)) {
        setOpen(false)
      }
    }
    document.addEventListener("mousedown", handlePointerDown)
    return () => document.removeEventListener("mousedown", handlePointerDown)
  }, [])

  useEffect(() => {
    if (open) {
      setActiveIndex(0)
      updateDropdownPosition()
      window.setTimeout(() => searchRef.current?.focus(), 0)
    } else {
      setSearch("")
      setRemoteOptions(null)
    }
  }, [open])

  useEffect(() => {
    if (!open) {
      return
    }
    function update() {
      updateDropdownPosition()
    }
    window.addEventListener("resize", update)
    window.addEventListener("scroll", update, true)
    return () => {
      window.removeEventListener("resize", update)
      window.removeEventListener("scroll", update, true)
    }
  }, [open])

  function updateDropdownPosition() {
    const rect = buttonRef.current?.getBoundingClientRect()
    if (!rect) {
      return
    }
    const viewportPadding = 12
    const width = Math.min(Math.max(rect.width, 260), window.innerWidth - viewportPadding * 2)
    const left = Math.min(Math.max(rect.left, viewportPadding), window.innerWidth - width - viewportPadding)
    setDropdownStyle({
      top: rect.bottom + 8,
      left,
      width,
    })
  }

  function choose(option: LookupOption | null) {
    setSelectedCache(option)
    onChange(option?.id ?? null)
    setOpen(false)
  }

  function handleKeyDown(event: KeyboardEvent) {
    if (disabled) {
      return
    }
    if (!open && (event.key === "Enter" || event.key === " " || event.key === "ArrowDown")) {
      event.preventDefault()
      setOpen(true)
      return
    }
    if (!open) {
      return
    }
    if (event.key === "ArrowDown") {
      event.preventDefault()
      setActiveIndex((index) => Math.min(index + 1, Math.max(visible.length - 1, 0)))
    } else if (event.key === "ArrowUp") {
      event.preventDefault()
      setActiveIndex((index) => Math.max(index - 1, 0))
    } else if (event.key === "Enter") {
      event.preventDefault()
      choose(visible[activeIndex] ?? null)
    } else if (event.key === "Escape") {
      event.preventDefault()
      setOpen(false)
    }
  }

  return (
    <label ref={rootRef} className="relative block space-y-2" onKeyDown={handleKeyDown}>
      <span className="text-xs uppercase text-zinc-500">{label}</span>
      <button
        ref={buttonRef}
        type="button"
        disabled={disabled}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={() => setOpen((current) => !current)}
        className={cn(
          "flex h-10 w-full min-w-0 items-center justify-between gap-2 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-left text-sm text-zinc-100 outline-none transition hover:border-zinc-700 focus:border-emerald-500",
          disabled && "cursor-not-allowed opacity-60",
        )}
      >
        <span className={cn("min-w-0 flex-1 truncate", !selected && "text-zinc-600")} title={selected?.label ?? placeholder}>
          {selected ? selected.label : placeholder}
        </span>
        <ChevronDown className={cn("h-4 w-4 shrink-0 text-zinc-500 transition", open && "rotate-180")} />
      </button>
      <input
        tabIndex={-1}
        value={value ?? ""}
        required={required}
        className="sr-only"
        onChange={() => undefined}
      />
      {open ? createPortal(
        <div
          ref={dropdownRef}
          className="fixed z-[9999] max-h-[min(22rem,calc(100vh-2rem))] overflow-hidden rounded-md border border-zinc-800 bg-zinc-950 shadow-2xl shadow-black/60"
          style={{ top: dropdownStyle.top, left: dropdownStyle.left, width: dropdownStyle.width }}
        >
          <div className="sticky top-0 z-10 border-b border-zinc-800 bg-zinc-950 p-2">
            <div className="relative">
              <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 shrink-0 -translate-y-1/2 text-zinc-600" />
              <input
                ref={searchRef}
                value={search}
                disabled={disabled}
                onChange={(event) => {
                  setSearch(event.target.value)
                  setActiveIndex(0)
                }}
                placeholder={searchPlaceholder ?? placeholder}
                className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 pl-9 pr-3 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-emerald-500 disabled:cursor-not-allowed disabled:opacity-60"
              />
            </div>
          </div>
          <div className="max-h-72 overflow-y-auto p-1" role="listbox">
            {loading ? (
              <div className="px-3 py-2 text-sm text-zinc-500">{t("states.loading")}</div>
            ) : null}
            <button
              type="button"
              className="flex w-full items-center gap-2 rounded px-3 py-2 text-left text-sm text-zinc-500 hover:bg-zinc-900 hover:text-zinc-200"
              onClick={() => choose(null)}
            >
              <span className="truncate">{placeholder}</span>
            </button>
            {visible.map((option, index) => (
              <button
                key={`${option.type ?? "lookup"}:${option.id}`}
                type="button"
                role="option"
                aria-selected={option.id === value}
                onMouseEnter={() => setActiveIndex(index)}
                onClick={() => choose(option)}
                className={cn(
                  "grid w-full grid-cols-[1fr_auto] items-center gap-2 rounded px-3 py-2 text-left text-sm transition",
                  index === activeIndex ? "bg-emerald-500/10 text-emerald-100" : "text-zinc-200 hover:bg-zinc-900",
                )}
              >
                <span className="min-w-0">
                  <span className="block truncate" title={option.label}>{option.label}</span>
                  <span className="mt-0.5 block truncate text-xs text-zinc-500" title={[option.parentLabel, option.description, option.type].filter(Boolean).join(" / ")}>
                    {[option.parentLabel, option.description, option.type].filter(Boolean).join(" / ")}
                  </span>
                </span>
                {option.id === value ? <Check className="h-4 w-4 shrink-0 text-emerald-300" /> : null}
              </button>
            ))}
            {!visible.length ? (
              <div className="px-3 py-6 text-center text-sm text-zinc-500">{searchPlaceholder ?? placeholder}</div>
            ) : null}
            {filtered.length > visible.length ? (
              <div className="border-t border-zinc-800 px-3 py-2 text-xs text-zinc-500">
                {t("select.visibleLimit", { visible: visible.length, total: filtered.length })}
              </div>
            ) : null}
          </div>
        </div>,
        document.body,
      ) : null}
    </label>
  )
}
