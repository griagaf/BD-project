import { useApiStatusQuery } from "@/features/auth/api/authQueries"
import { Card } from "@/shared/ui/card"

export function DashboardPage() {
  const status = useApiStatusQuery()

  return (
    <div className="space-y-6">
      <div>
        <div className="text-xs uppercase text-emerald-400">Operational overview</div>
        <h1 className="mt-2 text-2xl font-semibold">Tactical Dashboard</h1>
      </div>

      <div className="grid gap-4 md:grid-cols-3">
        <Card>
          <div className="text-sm text-zinc-500">Backend status</div>
          <div className="mt-3 text-3xl font-semibold text-emerald-300">
            {status.data?.status ?? "CHECKING"}
          </div>
        </Card>
        <Card>
          <div className="text-sm text-zinc-500">Security mode</div>
          <div className="mt-3 text-3xl font-semibold">JWT</div>
        </Card>
        <Card>
          <div className="text-sm text-zinc-500">Data access</div>
          <div className="mt-3 text-3xl font-semibold">Scoped</div>
        </Card>
      </div>
    </div>
  )
}

