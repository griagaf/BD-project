import { Building2, Plus } from "lucide-react"
import { useState } from "react"
import { useCurrentUserQuery } from "@/features/auth/api/authQueries"
import { useBuildingStatsQuery, useBuildingsQuery, useDeleteBuildingMutation, useSaveBuildingMutation } from "@/features/inventory/api/inventoryQueries"
import type { BuildingFilter, BuildingRow } from "@/features/inventory/model/inventoryTypes"
import { BuildingDialog } from "@/features/inventory/ui/BuildingDialog"
import { BuildingsTable } from "@/features/inventory/ui/BuildingsTable"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"
import { ErrorState } from "@/shared/ui/state"
import { TableSkeleton } from "@/shared/ui/skeleton"
import { toast } from "@/shared/ui/toast"

export function BuildingsPage() {
  const [filters, setFilters] = useState<BuildingFilter>({ page: 0, size: 10 })
  const [editing, setEditing] = useState<BuildingRow | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const { data: user } = useCurrentUserQuery()
  const { data, error, isLoading } = useBuildingsQuery(filters)
  const { data: stats } = useBuildingStatsQuery()
  const saveMutation = useSaveBuildingMutation(editing?.id)
  const deleteMutation = useDeleteBuildingMutation()
  const canEdit = user?.permissions.includes("building:update") ?? false

  return (
    <div className="space-y-5">
      <div className="grid gap-4 lg:grid-cols-[1fr_180px_180px_180px_auto]">
        <div>
          <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
            <Building2 className="size-4" />
            Infrastructure
          </div>
          <h1 className="mt-1 text-2xl font-semibold text-zinc-100">Buildings</h1>
          <p className="mt-1 text-sm text-zinc-500">Scoped building usage and readiness indicators.</p>
        </div>
        <Metric label="Readiness" value={`${stats?.readinessScore ?? 0}%`} />
        <Metric label="Buildings" value={stats?.buildings ?? 0} />
        <Metric label="Warnings" value={(stats?.emptyBuildings ?? 0) + (stats?.overloadedBuildings ?? 0)} />
        <Button
          type="button"
          disabled={!canEdit}
          onClick={() => {
            setEditing(null)
            setDialogOpen(true)
          }}
        >
          <Plus className="size-4" />
          Create
        </Button>
      </div>

      <Card className="grid gap-3 md:grid-cols-[1fr_180px_auto]">
        <input
          value={filters.search ?? ""}
          onChange={(event) => setFilters({ ...filters, page: 0, search: event.target.value })}
          placeholder="Search buildings or units"
          className="h-10 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
        />
        <input
          type="number"
          value={filters.unitId ?? ""}
          onChange={(event) => setFilters({ ...filters, page: 0, unitId: event.target.value ? Number(event.target.value) : undefined })}
          placeholder="Unit ID"
          className="h-10 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
        />
        <Button type="button" variant="secondary" onClick={() => setFilters({ page: 0, size: 10 })}>Reset</Button>
      </Card>

      {isLoading ? (
        <TableSkeleton columns={5} />
      ) : error ? (
        <ErrorState title="Unable to load buildings" />
      ) : (
        <BuildingsTable
          rows={data?.content ?? []}
          canEdit={canEdit}
          onEdit={(row) => {
            setEditing(row)
            setDialogOpen(true)
          }}
          onDelete={(row) => deleteMutation.mutate(row.id, {
            onSuccess: () => toast.success("Building deleted"),
            onError: () => toast.error("Building delete failed"),
          })}
        />
      )}

      <div className="flex items-center justify-between text-sm text-zinc-500">
        <span>Page {(filters.page ?? 0) + 1} of {Math.max(data?.totalPages ?? 1, 1)}</span>
        <div className="flex gap-2">
          <Button type="button" variant="secondary" disabled={data?.first ?? true} onClick={() => setFilters({ ...filters, page: Math.max((filters.page ?? 0) - 1, 0) })}>Previous</Button>
          <Button type="button" variant="secondary" disabled={data?.last ?? true} onClick={() => setFilters({ ...filters, page: (filters.page ?? 0) + 1 })}>Next</Button>
        </div>
      </div>

      <BuildingDialog
        row={editing}
        open={dialogOpen}
        saving={saveMutation.isPending}
        onClose={() => {
          setDialogOpen(false)
          setEditing(null)
        }}
        onSubmit={(request) => saveMutation.mutate(request, {
          onSuccess: () => {
            toast.success(editing ? "Building updated" : "Building created")
            setDialogOpen(false)
            setEditing(null)
          },
          onError: () => toast.error("Building save failed"),
        })}
      />
    </div>
  )
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <Card className="p-4">
      <div className="text-xs uppercase text-zinc-500">{label}</div>
      <div className="mt-2 text-2xl font-semibold text-zinc-100">{value}</div>
    </Card>
  )
}
