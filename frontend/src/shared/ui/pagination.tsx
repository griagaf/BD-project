import { ChevronLeft, ChevronRight, ChevronsLeft, ChevronsRight } from "lucide-react"
import type { ReactNode } from "react"
import { useTranslation } from "react-i18next"
import { Button } from "@/shared/ui/button"
import { cn } from "@/shared/lib/cn"

type PaginationProps = {
  page: number
  size: number
  totalElements: number
  totalPages: number
  onPageChange: (page: number) => void
  onSizeChange: (size: number) => void
  className?: string
}

const pageSizes = [10, 25, 50, 100]

export function Pagination({
  page,
  size,
  totalElements,
  totalPages,
  onPageChange,
  onSizeChange,
  className,
}: PaginationProps) {
  const { t } = useTranslation("common")
  const normalizedTotalPages = Math.max(totalPages, 1)
  const currentPage = Math.min(Math.max(page, 0), normalizedTotalPages - 1)
  const from = totalElements === 0 ? 0 : currentPage * size + 1
  const to = Math.min((currentPage + 1) * size, totalElements)
  const pages = buildPageItems(currentPage, normalizedTotalPages)

  return (
    <div className={cn("flex flex-col gap-3 rounded-md border border-zinc-800 bg-zinc-950/70 px-3 py-3 text-sm text-zinc-400 lg:flex-row lg:items-center lg:justify-between", className)}>
      <div className="flex flex-wrap items-center gap-3">
        <span className="text-zinc-500">
          {t("pagination.range", { from, to, total: totalElements })}
        </span>
        <label className="flex items-center gap-2">
          <span className="text-zinc-500">{t("pagination.rowsPerPage")}</span>
          <select
            value={size}
            onChange={(event) => onSizeChange(Number(event.target.value))}
            className="h-8 rounded-md border border-zinc-800 bg-zinc-900 px-2 text-xs text-zinc-100 outline-none focus:border-emerald-500"
            aria-label={t("pagination.rowsPerPage")}
          >
            {pageSizes.map((item) => (
              <option key={item} value={item}>{item}</option>
            ))}
          </select>
        </label>
      </div>

      <div className="flex flex-wrap items-center gap-1">
        <PageButton disabled={currentPage === 0} label={t("pagination.first")} onClick={() => onPageChange(0)}>
          <ChevronsLeft className="h-4 w-4 shrink-0" />
        </PageButton>
        <PageButton disabled={currentPage === 0} label={t("actions.previous")} onClick={() => onPageChange(currentPage - 1)}>
          <ChevronLeft className="h-4 w-4 shrink-0" />
        </PageButton>

        <div className="mx-1 flex items-center gap-1">
          {pages.map((item, index) => (
            item === "ellipsis" ? (
              <span key={`ellipsis-${index}`} className="px-2 text-zinc-600">{t("pagination.ellipsis")}</span>
            ) : (
              <button
                key={item}
                type="button"
                onClick={() => onPageChange(item)}
                className={cn(
                  "h-8 min-w-8 rounded-md border px-2 text-xs transition-colors",
                  item === currentPage
                    ? "border-emerald-500/40 bg-emerald-500/15 text-emerald-100"
                    : "border-zinc-800 bg-zinc-900 text-zinc-400 hover:border-zinc-700 hover:text-zinc-100",
                )}
                aria-current={item === currentPage ? "page" : undefined}
              >
                {item + 1}
              </button>
            )
          ))}
        </div>

        <PageButton disabled={currentPage >= normalizedTotalPages - 1} label={t("actions.next")} onClick={() => onPageChange(currentPage + 1)}>
          <ChevronRight className="h-4 w-4 shrink-0" />
        </PageButton>
        <PageButton disabled={currentPage >= normalizedTotalPages - 1} label={t("pagination.last")} onClick={() => onPageChange(normalizedTotalPages - 1)}>
          <ChevronsRight className="h-4 w-4 shrink-0" />
        </PageButton>
      </div>
    </div>
  )
}

function PageButton({ children, disabled, label, onClick }: { children: ReactNode; disabled: boolean; label: string; onClick: () => void }) {
  return (
    <Button
      type="button"
      variant="secondary"
      size="icon"
      className="h-8 w-8 shrink-0"
      disabled={disabled}
      onClick={onClick}
      title={label}
      aria-label={label}
    >
      {children}
    </Button>
  )
}

function buildPageItems(currentPage: number, totalPages: number): Array<number | "ellipsis"> {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index)
  }

  const pages = new Set<number>([0, totalPages - 1, currentPage - 1, currentPage, currentPage + 1])
  if (currentPage <= 2) {
    pages.add(1)
    pages.add(2)
    pages.add(3)
  }
  if (currentPage >= totalPages - 3) {
    pages.add(totalPages - 2)
    pages.add(totalPages - 3)
    pages.add(totalPages - 4)
  }

  const sorted = [...pages]
    .filter((item) => item >= 0 && item < totalPages)
    .sort((left, right) => left - right)

  return sorted.reduce<Array<number | "ellipsis">>((result, item, index) => {
    const previous = sorted[index - 1]
    if (index > 0 && item - previous > 1) {
      result.push("ellipsis")
    }
    result.push(item)
    return result
  }, [])
}
