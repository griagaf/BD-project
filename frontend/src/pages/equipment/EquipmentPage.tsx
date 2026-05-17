import { Boxes } from "lucide-react"
import type { ReactNode } from "react"
import { useState } from "react"
import { useCurrentUserQuery } from "@/features/auth/api/authQueries"
import { useDeleteInventoryMutation, useInventoryDictionariesQuery, useInventoryQuery, useInventoryStatsQuery, useUpdateInventoryMutation } from "@/features/inventory/api/inventoryQueries"
import type { InventoryFilter, InventoryRow } from "@/features/inventory/model/inventoryTypes"
import { FiltersPanel } from "@/features/inventory/ui/FiltersPanel"
import { InventoryDialog } from "@/features/inventory/ui/InventoryDialog"
import { InventoryTable } from "@/features/inventory/ui/InventoryTable"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"
import { ErrorState } from "@/shared/ui/state"
import { TableSkeleton } from "@/shared/ui/skeleton"
import { toast } from "@/shared/ui/toast"

export function EquipmentPage() {
  return <InventoryResourcePage kind="equipment" title="Equipment" icon={<Boxes className="size-4" />} />
}

export function InventoryResourcePage({ kind, title, icon }: { kind: "equipment" | "weapons"; title: string; icon: ReactNode }) {
  const [filters, setFilters] = useState<InventoryFilter>({ page: 0, size: 10 })
  const [editing, setEditing] = useState<InventoryRow | null>(null)
  const { data: user } = useCurrentUserQuery()
  const { data, error, isLoading } = useInventoryQuery(kind, filters)
  const { data: stats } = useInventoryStatsQuery(kind)
  const { data: dictionaries } = useInventoryDictionariesQuery(kind)
  const updateMutation = useUpdateInventoryMutation(kind)
  const deleteMutation = useDeleteInventoryMutation(kind)
  const canEdit = user?.permissions.includes(`${kind === "equipment" ? "equipment" : "weapon"}:update`) ?? false

  return (
    <div className="space-y-5">
      <Header title={title} icon={icon} readiness={stats?.readinessScore ?? 0} total={stats?.totalQuantity ?? 0} warnings={stats?.warningRows ?? 0} />
      <FiltersPanel filters={filters} categories={dictionaries?.categories} types={dictionaries?.types} onChange={setFilters} />
      {isLoading ? (
        <TableSkeleton columns={6} />
      ) : error ? (
        <ErrorState title="Unable to load inventory" />
      ) : (
        <InventoryTable rows={data?.content ?? []} canEdit={canEdit} onEdit={setEditing} onDelete={(row) => deleteMutation.mutate({ unitId: row.unitId, typeId: row.typeId }, {
          onSuccess: () => toast.success(`${title} row deleted`),
          onError: () => toast.error(`${title} delete failed`),
        })} />
      )}
      <Pager filters={filters} totalPages={data?.totalPages ?? 1} first={data?.first ?? true} last={data?.last ?? true} onChange={setFilters} />
      <InventoryDialog
        row={editing}
        open={Boolean(editing)}
        saving={updateMutation.isPending}
        onClose={() => setEditing(null)}
        onSubmit={(quantity) => {
          if (editing) {
            updateMutation.mutate({ unitId: editing.unitId, typeId: editing.typeId, quantity }, {
              onSuccess: () => {
                toast.success(`${title} row updated`)
                setEditing(null)
              },
              onError: () => toast.error(`${title} update failed`),
            })
          }
        }}
      />
    </div>
  )
}

function Header({ title, icon, readiness, total, warnings }: { title: string; icon: ReactNode; readiness: number; total: number; warnings: number }) {
  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_180px_180px_180px]">
      <div>
        <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">{icon}{title} inventory</div>
        <h1 className="mt-1 text-2xl font-semibold text-zinc-100">{title}</h1>
        <p className="mt-1 text-sm text-zinc-500">Scoped inventory view with readiness indicators.</p>
      </div>
      <Metric label="Readiness" value={`${readiness}%`} />
      <Metric label="Total qty" value={total} />
      <Metric label="Warnings" value={warnings} />
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

function Pager({ filters, totalPages, first, last, onChange }: { filters: InventoryFilter; totalPages: number; first: boolean; last: boolean; onChange: (filters: InventoryFilter) => void }) {
  return (
    <div className="flex items-center justify-between text-sm text-zinc-500">
      <span>Page {(filters.page ?? 0) + 1} of {Math.max(totalPages, 1)}</span>
      <div className="flex gap-2">
        <Button type="button" variant="secondary" disabled={first} onClick={() => onChange({ ...filters, page: Math.max((filters.page ?? 0) - 1, 0) })}>Previous</Button>
        <Button type="button" variant="secondary" disabled={last} onClick={() => onChange({ ...filters, page: (filters.page ?? 0) + 1 })}>Next</Button>
      </div>
    </div>
  )
}
