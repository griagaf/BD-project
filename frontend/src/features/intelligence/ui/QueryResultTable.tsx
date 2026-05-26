import type { QueryResult } from "@/features/intelligence/model/intelligenceTypes"
import { useEffect, useMemo, useState } from "react"
import { useTranslation } from "react-i18next"
import { Card } from "@/shared/ui/card"
import { EmptyState } from "@/shared/ui/state"
import { Table, TableShell, tableCellClass, tableHeadClass, tableRowClass } from "@/shared/ui/table"
import { cn } from "@/shared/lib/cn"
import { Pagination } from "@/shared/ui/pagination"

type QueryResultTableProps = {
  result?: QueryResult
}

export function QueryResultTable({ result }: QueryResultTableProps) {
  const { t } = useTranslation(["common", "intelligence"])
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(25)

  useEffect(() => {
    setPage(0)
  }, [result?.executedAt])

  const rows = result?.rows ?? []
  const displayColumns = useMemo(() => {
    const columns = result?.columns ?? []
    const readableColumns = columns.filter((column) => !column.endsWith("_id"))
    return readableColumns.length ? readableColumns : columns
  }, [result?.columns])
  const totalPages = Math.max(1, Math.ceil(rows.length / size))
  const currentPage = Math.min(page, totalPages - 1)
  const visibleRows = useMemo(
    () => rows.slice(currentPage * size, currentPage * size + size),
    [currentPage, rows, size],
  )

  if (!result) {
    return <EmptyState title={t("intelligence:states.noQueryTitle")} description={t("intelligence:states.noQueryDescription")} />
  }

  return (
    <Card className="p-0">
      <div className="border-b border-zinc-800 px-4 py-3 text-sm text-zinc-400">
        {t("intelligence:result.rowsReturned", { count: result.rowCount, time: new Date(result.executedAt).toLocaleString() })}
      </div>
      {rows.length ? (
        <>
        <TableShell className="rounded-none border-0">
        <Table>
          <thead className={tableHeadClass}>
            <tr>
              {displayColumns.map((column) => (
                <th key={column} className={cn(tableCellClass, "whitespace-nowrap")}>
                  <span className="block max-w-56 truncate" title={columnLabel(t, column)}>{columnLabel(t, column)}</span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {visibleRows.map((row, index) => (
              <tr key={`${currentPage}:${index}`} className={tableRowClass}>
                {displayColumns.map((column) => (
                  <td key={column} className={cn(tableCellClass, "text-zinc-300")}>
                    <span className="block max-w-72 truncate" title={formatCellValue(t, row[column])}>{formatCellValue(t, row[column])}</span>
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </Table>
      </TableShell>
      <Pagination
        className="rounded-none border-x-0 border-b-0"
        page={currentPage}
        size={size}
        totalElements={rows.length}
        totalPages={totalPages}
        onPageChange={setPage}
        onSizeChange={(nextSize) => {
          setSize(nextSize)
          setPage(0)
        }}
      />
      </>
      ) : (
        <div className="p-5">
          <EmptyState title={t("intelligence:states.noRowsTitle")} description={t("intelligence:states.noRowsDescription")} />
        </div>
      )}
    </Card>
  )
}

function columnLabel(t: (key: string, options?: Record<string, unknown>) => string, column: string) {
  return t(`intelligence:result.columns.${column}`, {
    defaultValue: column.replaceAll("_", " "),
  })
}

function formatCellValue(t: (key: string, options?: Record<string, unknown>) => string, value: unknown) {
  if (value === null || value === undefined || value === "") {
    return ""
  }
  if (typeof value !== "string") {
    return String(value)
  }
  return t(`intelligence:result.values.${value}`, { defaultValue: value })
}
