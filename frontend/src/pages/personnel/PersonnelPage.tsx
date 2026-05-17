import { Plus, ShieldCheck } from "lucide-react"
import { useMemo, useState } from "react"
import { useTranslation } from "react-i18next"
import { useCurrentUserQuery } from "@/features/auth/api/authQueries"
import {
  useCreatePersonnelMutation,
  useDeletePersonnelMutation,
  usePersonnelDictionariesQuery,
  usePersonnelQuery,
  useUpdatePersonnelMutation,
} from "@/features/personnel/api/personnelQueries"
import type { Personnel, PersonnelFilter, PersonnelRequest } from "@/features/personnel/model/personnelTypes"
import { PersonnelEditModal } from "@/features/personnel/ui/PersonnelEditModal"
import { PersonnelFilters } from "@/features/personnel/ui/PersonnelFilters"
import { PersonnelTable } from "@/features/personnel/ui/PersonnelTable"
import { Button } from "@/shared/ui/button"
import { PageHeader } from "@/shared/ui/page"
import { TableSkeleton } from "@/shared/ui/skeleton"
import { ErrorState } from "@/shared/ui/state"
import { toast } from "@/shared/ui/toast"

const initialFilters: PersonnelFilter = {
  page: 0,
  size: 10,
  sort: "lastName,asc",
}

export function PersonnelPage() {
  const { t } = useTranslation(["common", "personnel"])
  const [filters, setFilters] = useState<PersonnelFilter>(initialFilters)
  const [editing, setEditing] = useState<Personnel | null>(null)
  const [modalOpen, setModalOpen] = useState(false)
  const { data: user } = useCurrentUserQuery()
  const { data, isLoading, error } = usePersonnelQuery(filters)
  const { data: dictionaries } = usePersonnelDictionariesQuery()
  const createMutation = useCreatePersonnelMutation()
  const updateMutation = useUpdatePersonnelMutation(editing?.id ?? 0)
  const deleteMutation = useDeletePersonnelMutation()

  const permissions = user?.permissions ?? []
  const canCreate = permissions.includes("personnel:create")
  const canEdit = permissions.includes("personnel:update")
  const canDelete = permissions.includes("personnel:delete")

  const statusText = useMemo(() => {
    if (isLoading) {
      return t("personnel:page.loading")
    }
    return t("personnel:page.recordsVisible", { count: data?.totalElements ?? 0 })
  }, [data?.totalElements, isLoading, t])

  function submit(request: PersonnelRequest) {
    const mutation = editing ? updateMutation : createMutation
    mutation.mutate(request, {
      onSuccess: () => {
        toast.success(editing ? t("personnel:toast.updated") : t("personnel:toast.created"))
        setModalOpen(false)
        setEditing(null)
      },
      onError: () => toast.error(t("personnel:toast.saveFailed")),
    })
  }

  return (
    <div className="space-y-5">
      <PageHeader
        icon={ShieldCheck}
        eyebrow={t("personnel:page.eyebrow")}
        title={t("personnel:page.title")}
        description={statusText}
        actions={<Button
          type="button"
          disabled={!canCreate}
          onClick={() => {
            setEditing(null)
            setModalOpen(true)
          }}
        >
          <Plus className="size-4" />
          {t("actions.create")}
        </Button>}
      />

      <PersonnelFilters
        filters={filters}
        specialties={dictionaries?.specialties ?? []}
        onChange={(nextFilters) => setFilters({ ...filters, ...nextFilters })}
      />

      {isLoading ? (
        <TableSkeleton columns={5} />
      ) : error ? (
        <ErrorState title={t("personnel:error")} />
      ) : (
        <PersonnelTable
          rows={data?.content ?? []}
          canEdit={canEdit}
          canDelete={canDelete}
          onEdit={(personnel) => {
            setEditing(personnel)
            setModalOpen(true)
          }}
          onDelete={(id) => {
            deleteMutation.mutate(id, {
              onSuccess: () => toast.success(t("personnel:toast.deleted")),
              onError: () => toast.error(t("personnel:toast.deleteFailed")),
            })
          }}
        />
      )}

      <div className="flex items-center justify-between text-sm text-zinc-500">
        <span>
          Page {(data?.page ?? 0) + 1} of {Math.max(data?.totalPages ?? 1, 1)}
        </span>
        <div className="flex gap-2">
          <Button
            type="button"
            variant="secondary"
            disabled={data?.first ?? true}
            onClick={() => setFilters({ ...filters, page: Math.max((filters.page ?? 0) - 1, 0) })}
          >
            Previous
          </Button>
          <Button
            type="button"
            variant="secondary"
            disabled={data?.last ?? true}
            onClick={() => setFilters({ ...filters, page: (filters.page ?? 0) + 1 })}
          >
            Next
          </Button>
        </div>
      </div>

      <PersonnelEditModal
        open={modalOpen}
        personnel={editing}
        ranks={dictionaries?.ranks ?? []}
        specialties={dictionaries?.specialties ?? []}
        saving={createMutation.isPending || updateMutation.isPending}
        onClose={() => {
          setModalOpen(false)
          setEditing(null)
        }}
        onSubmit={submit}
      />
    </div>
  )
}
