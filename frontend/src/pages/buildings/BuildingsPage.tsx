import { Building2, Plus } from "lucide-react"
import { useQuery } from "@tanstack/react-query"
import { useState } from "react"
import { useTranslation } from "react-i18next"
import { useCurrentUserQuery } from "@/features/auth/api/authQueries"
import { useAssignBuildingSubdivisionMutation, useBuildingStatsQuery, useBuildingsQuery, useDeleteBuildingMutation, useRemoveBuildingSubdivisionMutation, useSaveBuildingMutation } from "@/features/inventory/api/inventoryQueries"
import type { BuildingFilter, BuildingRow } from "@/features/inventory/model/inventoryTypes"
import { BuildingDialog } from "@/features/inventory/ui/BuildingDialog"
import { BuildingsTable } from "@/features/inventory/ui/BuildingsTable"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"
import { ErrorState } from "@/shared/ui/state"
import { TableSkeleton } from "@/shared/ui/skeleton"
import { toast } from "@/shared/ui/toast"
import { Pagination } from "@/shared/ui/pagination"
import { lookupApi } from "@/shared/api/lookupApi"
import { SearchableSelect } from "@/shared/ui/searchable-select"
import { ApiError } from "@/shared/api/apiError"

export function BuildingsPage() {
  const { t } = useTranslation(["common", "buildings"])
  const [filters, setFilters] = useState<BuildingFilter>({ page: 0, size: 10 })
  const [editing, setEditing] = useState<BuildingRow | null>(null)
  const [assigning, setAssigning] = useState<BuildingRow | null>(null)
  const [subdivisionId, setSubdivisionId] = useState<number | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)
  const { data: user } = useCurrentUserQuery()
  const { data, error, isLoading } = useBuildingsQuery(filters)
  const { data: stats } = useBuildingStatsQuery()
  const saveMutation = useSaveBuildingMutation(editing?.id)
  const deleteMutation = useDeleteBuildingMutation()
  const assignMutation = useAssignBuildingSubdivisionMutation()
  const removeAssignmentMutation = useRemoveBuildingSubdivisionMutation()
  const { data: unitOptions = [] } = useQuery({
    queryKey: ["lookups", "units", "buildings"],
    queryFn: () => lookupApi.units(),
    staleTime: 5 * 60_000,
  })
  const canEdit = user?.permissions.includes("building:update") ?? false

  return (
    <div className="space-y-5">
      <div className="grid gap-4 lg:grid-cols-[1fr_180px_180px_180px_auto]">
        <div>
          <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
            <Building2 className="h-4 w-4 shrink-0" />
            <span className="truncate" title={t("buildings:page.eyebrow")}>{t("buildings:page.eyebrow")}</span>
          </div>
          <h1 className="mt-1 break-words text-2xl font-semibold text-zinc-100">{t("buildings:page.title")}</h1>
          <p className="mt-1 break-words text-sm text-zinc-500">{t("buildings:page.description")}</p>
        </div>
        <Metric label={t("buildings:metric.readiness")} value={`${stats?.readinessScore ?? 0}%`} />
        <Metric label={t("buildings:metric.buildings")} value={stats?.buildings ?? 0} />
        <Metric label={t("buildings:metric.warnings")} value={(stats?.emptyBuildings ?? 0) + (stats?.overloadedBuildings ?? 0)} />
        <Button
          type="button"
          disabled={!canEdit}
          onClick={() => {
            setEditing(null)
            setDialogOpen(true)
          }}
        >
          <Plus className="h-4 w-4 shrink-0" />
          {t("actions.create")}
        </Button>
      </div>

      <Card className="grid gap-3 md:grid-cols-[minmax(0,1fr)_180px_auto]">
        <input
          value={filters.search ?? ""}
          onChange={(event) => setFilters({ ...filters, page: 0, search: event.target.value })}
          placeholder={t("buildings:filters.search")}
          className="h-10 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
        />
        <SearchableSelect
          label={t("buildings:filters.unit")}
          value={filters.unitId ?? null}
          options={unitOptions}
          placeholder={t("buildings:filters.unitPlaceholder")}
          onChange={(value) => setFilters({ ...filters, page: 0, unitId: value ?? undefined })}
        />
        <Button type="button" variant="secondary" onClick={() => setFilters({ page: 0, size: 10 })}>{t("actions.reset")}</Button>
      </Card>

      {isLoading ? (
        <TableSkeleton columns={5} />
      ) : error ? (
        <ErrorState title={t("buildings:error")} />
      ) : (
        <BuildingsTable
          rows={data?.content ?? []}
          canEdit={canEdit}
          onEdit={(row) => {
            setEditing(row)
            setDialogOpen(true)
          }}
          onDelete={(row) => deleteMutation.mutate(row.id, {
            onSuccess: () => toast.success(t("buildings:toast.deleted")),
            onError: (error) => toast.error(errorMessage(error, t("buildings:toast.deleteFailed"))),
          })}
          onAssign={(row) => {
            setAssigning(row)
            setSubdivisionId(null)
          }}
          onRemoveAssignment={(row, targetSubdivisionId) => removeAssignmentMutation.mutate({ buildingId: row.id, subdivisionId: targetSubdivisionId }, {
            onSuccess: () => toast.success(t("buildings:toast.unassigned")),
            onError: (error) => toast.error(errorMessage(error, t("buildings:toast.unassignFailed"))),
          })}
        />
      )}

      <Pagination
        page={data?.page ?? filters.page ?? 0}
        size={data?.size ?? filters.size ?? 10}
        totalElements={data?.totalElements ?? 0}
        totalPages={data?.totalPages ?? 1}
        onPageChange={(page) => setFilters({ ...filters, page })}
        onSizeChange={(size) => setFilters({ ...filters, page: 0, size })}
      />

      <BuildingDialog
        row={editing}
        open={dialogOpen}
        saving={saveMutation.isPending}
        unitOptions={unitOptions}
        onClose={() => {
          setDialogOpen(false)
          setEditing(null)
        }}
        onSubmit={(request) => saveMutation.mutate(request, {
          onSuccess: () => {
            toast.success(editing ? t("buildings:toast.updated") : t("buildings:toast.created"))
            setDialogOpen(false)
            setEditing(null)
          },
          onError: (error) => toast.error(errorMessage(error, t("buildings:toast.saveFailed"))),
        })}
      />

      {assigning ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="w-full max-w-lg rounded-md border border-zinc-800 bg-zinc-950 p-5 shadow-2xl">
            <div className="text-xs uppercase text-emerald-300">{t("buildings:assignment.eyebrow")}</div>
            <h2 className="mt-1 break-words text-xl font-semibold text-zinc-100">{assigning.name}</h2>
            <p className="mt-1 text-sm text-zinc-500">{t("buildings:assignment.description")}</p>
            <div className="mt-5">
              <SearchableSelect
                label={t("buildings:assignment.subdivision")}
                value={subdivisionId}
                options={[]}
                loadOptions={lookupApi.subdivisions}
                placeholder={t("buildings:assignment.selectSubdivision")}
                onChange={setSubdivisionId}
              />
            </div>
            <div className="mt-5 flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
              <Button type="button" variant="ghost" onClick={() => setAssigning(null)}>{t("actions.cancel")}</Button>
              <Button
                type="button"
                disabled={!subdivisionId || assignMutation.isPending}
                onClick={() => {
                  if (!subdivisionId) return
                  assignMutation.mutate({ buildingId: assigning.id, subdivisionId }, {
                    onSuccess: () => {
                      toast.success(t("buildings:toast.assigned"))
                      setAssigning(null)
                    },
                    onError: (error) => toast.error(errorMessage(error, t("buildings:toast.assignFailed"))),
                  })
                }}
              >
                {t("buildings:assignment.submit")}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message || fallback : fallback
}

function Metric({ label, value }: { label: string; value: string | number }) {
  return (
    <Card className="p-4">
      <div className="truncate text-xs uppercase text-zinc-500" title={label}>{label}</div>
      <div className="mt-2 text-2xl font-semibold text-zinc-100">{value}</div>
    </Card>
  )
}
