import type { QueryTemplateMetadata } from "@/features/intelligence/model/intelligenceTypes"
import { Search } from "lucide-react"
import { useMemo, useState } from "react"
import { Card } from "@/shared/ui/card"
import { useTranslation } from "react-i18next"
import { Pagination } from "@/shared/ui/pagination"

type QueryTemplateSelectorProps = {
  templates: QueryTemplateMetadata[]
  selectedCode: string
  onSelect: (code: string) => void
}

export function QueryTemplateSelector({ templates, selectedCode, onSelect }: QueryTemplateSelectorProps) {
  const { t } = useTranslation("intelligence")
  const [search, setSearch] = useState("")
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const filtered = useMemo(() => {
    const normalized = search.trim().toLowerCase()
    if (!normalized) {
      return templates
    }
    return templates.filter((template) =>
      `${template.label} ${template.description} ${template.code}`.toLowerCase().includes(normalized),
    )
  }, [search, templates])
  const totalPages = Math.max(1, Math.ceil(filtered.length / size))
  const currentPage = Math.min(page, totalPages - 1)
  const visible = filtered.slice(currentPage * size, currentPage * size + size)

  return (
    <Card className="space-y-2">
      <div className="text-xs uppercase text-emerald-300">{t("builder.templates")}</div>
      <div className="relative">
        <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 shrink-0 -translate-y-1/2 text-zinc-600" />
        <input
          value={search}
          onChange={(event) => {
            setSearch(event.target.value)
            setPage(0)
          }}
          placeholder={t("builder.searchTemplates")}
          className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 pl-9 pr-3 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-emerald-500"
        />
      </div>
      <div className="max-h-[32rem] space-y-2 overflow-y-auto pr-1">
        {visible.map((template) => (
          <button
            key={template.code}
            type="button"
            className={`w-full rounded-md border px-3 py-2 text-left transition-colors ${
              selectedCode === template.code
                ? "border-emerald-500/60 bg-emerald-500/10"
                : "border-zinc-800 bg-zinc-950 hover:bg-zinc-900"
            }`}
            onClick={() => onSelect(template.code)}
          >
            <div className="text-sm font-medium text-zinc-100">{template.label}</div>
            <div className="mt-1 line-clamp-2 text-xs text-zinc-500">{template.description}</div>
          </button>
        ))}
      </div>
      <Pagination
        page={currentPage}
        size={size}
        totalElements={filtered.length}
        totalPages={totalPages}
        onPageChange={setPage}
        onSizeChange={(nextSize) => {
          setSize(nextSize)
          setPage(0)
        }}
      />
    </Card>
  )
}
