import type { QueryResult } from "@/features/intelligence/model/intelligenceTypes"
import { useTranslation } from "react-i18next"
import { Card } from "@/shared/ui/card"
import { EmptyState } from "@/shared/ui/state"
import { Table, TableShell, tableCellClass, tableHeadClass, tableRowClass } from "@/shared/ui/table"
import { cn } from "@/shared/lib/cn"

type QueryResultTableProps = {
  result?: QueryResult
}

export function QueryResultTable({ result }: QueryResultTableProps) {
  const { t } = useTranslation(["common", "intelligence"])
  if (!result) {
    return <EmptyState title={t("intelligence:states.noQueryTitle")} description={t("intelligence:states.noQueryDescription")} />
  }

  return (
    <Card className="p-0">
      <div className="border-b border-zinc-800 px-4 py-3 text-sm text-zinc-400">
        {t("intelligence:result.rowsReturned", { count: result.rowCount, time: new Date(result.executedAt).toLocaleString() })}
      </div>
      {result.rows.length ? (
        <TableShell className="rounded-none border-0">
        <Table>
          <thead className={tableHeadClass}>
            <tr>
              {result.columns.map((column) => (
                <th key={column} className={cn(tableCellClass, "whitespace-nowrap")}>
                  <span className="block max-w-56 truncate" title={column}>{column}</span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {result.rows.map((row, index) => (
              <tr key={index} className={tableRowClass}>
                {result.columns.map((column) => (
                  <td key={column} className={cn(tableCellClass, "text-zinc-300")}>
                    <span className="block max-w-72 truncate" title={String(row[column] ?? "")}>{String(row[column] ?? "")}</span>
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </Table>
      </TableShell>
      ) : (
        <div className="p-5">
          <EmptyState title={t("intelligence:states.noRowsTitle")} description={t("intelligence:states.noRowsDescription")} />
        </div>
      )}
    </Card>
  )
}
