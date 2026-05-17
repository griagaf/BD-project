import type { QueryResult } from "@/features/intelligence/model/intelligenceTypes"
import { Card } from "@/shared/ui/card"
import { EmptyState } from "@/shared/ui/state"
import { Table, TableShell, tableCellClass, tableHeadClass, tableRowClass } from "@/shared/ui/table"
import { cn } from "@/shared/lib/cn"

type QueryResultTableProps = {
  result?: QueryResult
}

export function QueryResultTable({ result }: QueryResultTableProps) {
  if (!result) {
    return <EmptyState title="No query executed" description="Execute a template to inspect scoped SQL results." />
  }

  return (
    <Card className="p-0">
      <div className="border-b border-zinc-800 px-4 py-3 text-sm text-zinc-400">
        {result.rowCount} rows returned at {new Date(result.executedAt).toLocaleString()}
      </div>
      {result.rows.length ? (
        <TableShell className="rounded-none border-0">
        <Table>
          <thead className={tableHeadClass}>
            <tr>
              {result.columns.map((column) => (
                <th key={column} className={cn(tableCellClass, "whitespace-nowrap")}>{column}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {result.rows.map((row, index) => (
              <tr key={index} className={tableRowClass}>
                {result.columns.map((column) => (
                  <td key={column} className={cn(tableCellClass, "whitespace-nowrap text-zinc-300")}>{String(row[column] ?? "")}</td>
                ))}
              </tr>
            ))}
          </tbody>
        </Table>
      </TableShell>
      ) : (
        <div className="p-5">
          <EmptyState title="No rows in current scope" description="The selected template executed successfully but returned no rows." />
        </div>
      )}
    </Card>
  )
}
