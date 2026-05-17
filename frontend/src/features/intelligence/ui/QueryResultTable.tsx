import type { QueryResult } from "@/features/intelligence/model/intelligenceTypes"
import { Card } from "@/shared/ui/card"

type QueryResultTableProps = {
  result?: QueryResult
}

export function QueryResultTable({ result }: QueryResultTableProps) {
  if (!result) {
    return <Card className="text-sm text-zinc-500">Execute a query to inspect scoped results</Card>
  }

  return (
    <Card className="overflow-hidden p-0">
      <div className="border-b border-zinc-800 px-4 py-3 text-sm text-zinc-400">
        {result.rowCount} rows returned at {new Date(result.executedAt).toLocaleString()}
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-zinc-800 bg-zinc-900/60 text-xs uppercase text-zinc-500">
            <tr>
              {result.columns.map((column) => (
                <th key={column} className="whitespace-nowrap px-4 py-3">{column}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {result.rows.map((row, index) => (
              <tr key={index} className="border-b border-zinc-900">
                {result.columns.map((column) => (
                  <td key={column} className="whitespace-nowrap px-4 py-3 text-zinc-300">{String(row[column] ?? "")}</td>
                ))}
              </tr>
            ))}
            {!result.rows.length ? (
              <tr>
                <td className="px-4 py-8 text-center text-zinc-500" colSpan={Math.max(result.columns.length, 1)}>No rows in current scope</td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </div>
    </Card>
  )
}
