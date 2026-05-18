import { Boxes } from "lucide-react"
import type { ReactNode } from "react"
import { useState } from "react"
import { useTranslation } from "react-i18next"
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
import { Pagination } from "@/shared/ui/pagination"

export function EquipmentPage() {
  return <InventoryResourcePage kind="equipment" icon={<Boxes className="h-4 w-4 shrink-0" />} />
}

export function InventoryResourcePage({ kind, icon }: { kind: "equipment" | "weapons"; icon: ReactNode }) {
  const namespace = kind === "equipment" ? "equipment" : "weapons"
  const { t } = useTranslation(["common", "equipment", "weapons"])
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
      <Header title={t(`${namespace}:page.title`)} eyebrow={t(`${namespace}:page.eyebrow`)} description={t(`${namespace}:page.description`)} icon={icon} readiness={stats?.readinessScore ?? 0} total={stats?.totalQuantity ?? 0} warnings={stats?.warningRows ?? 0} />
      <FiltersPanel filters={filters} categories={dictionaries?.categories} types={dictionaries?.types} onChange={setFilters} />
      {isLoading ? (
        <TableSkeleton columns={6} />
      ) : error ? (
        <ErrorState title={t(`${namespace}:error`)} />
      ) : (
        <InventoryTable rows={data?.content ?? []} canEdit={canEdit} onEdit={setEditing} onDelete={(row) => deleteMutation.mutate({ unitId: row.unitId, typeId: row.typeId }, {
          onSuccess: () => toast.success(t(`${namespace}:toast.deleted`)),
          onError: () => toast.error(t(`${namespace}:toast.deleteFailed`)),
        })} />
      )}
      <Pagination
        page={data?.page ?? filters.page ?? 0}
        size={data?.size ?? filters.size ?? 10}
        totalElements={data?.totalElements ?? 0}
        totalPages={data?.totalPages ?? 1}
        onPageChange={(page) => setFilters({ ...filters, page })}
        onSizeChange={(size) => setFilters({ ...filters, page: 0, size })}
      />
      <InventoryDialog
        row={editing}
        open={Boolean(editing)}
        saving={updateMutation.isPending}
        onClose={() => setEditing(null)}
        onSubmit={(quantity) => {
          if (editing) {
            updateMutation.mutate({ unitId: editing.unitId, typeId: editing.typeId, quantity }, {
              onSuccess: () => {
                toast.success(t(`${namespace}:toast.updated`))
                setEditing(null)
              },
              onError: () => toast.error(t(`${namespace}:toast.updateFailed`)),
            })
          }
        }}
      />
    </div>
  )
}

function Header({ title, eyebrow, description, icon, readiness, total, warnings }: { title: string; eyebrow: string; description: string; icon: ReactNode; readiness: number; total: number; warnings: number }) {
  const { t } = useTranslation(["common", "equipment"])
  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_180px_180px_180px]">
      <div>
        <div className="flex min-w-0 items-center gap-2 text-xs uppercase text-emerald-300">{icon}<span className="truncate" title={eyebrow}>{eyebrow}</span></div>
        <h1 className="mt-1 break-words text-2xl font-semibold text-zinc-100">{title}</h1>
        <p className="mt-1 break-words text-sm text-zinc-500">{description}</p>
      </div>
      <Metric label={t("equipment:metric.readiness")} value={`${readiness}%`} />
      <Metric label={t("equipment:metric.totalQty")} value={total} />
      <Metric label={t("equipment:metric.warnings")} value={warnings} />
    </div>
  )
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <Card className="p-4">
      <div className="truncate text-xs uppercase text-zinc-500" title={label}>{label}</div>
      <div className="mt-2 text-2xl font-semibold text-zinc-100">{value}</div>
    </Card>
  )
}
