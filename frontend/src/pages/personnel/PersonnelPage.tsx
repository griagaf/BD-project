import { Plus, ShieldCheck } from "lucide-react"
import { useMemo, useState } from "react"
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
import { Card } from "@/shared/ui/card"

const initialFilters: PersonnelFilter = {
  page: 0,
  size: 10,
  sort: "lastName,asc",
}

export function PersonnelPage() {
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
      return "Loading scoped personnel"
    }
    return `${data?.totalElements ?? 0} records visible`
  }, [data?.totalElements, isLoading])

  function submit(request: PersonnelRequest) {
    const mutation = editing ? updateMutation : createMutation
    mutation.mutate(request, {
      onSuccess: () => {
        setModalOpen(false)
        setEditing(null)
      },
    })
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
            <ShieldCheck className="size-4" />
            Personnel Registry
          </div>
          <h1 className="mt-1 text-2xl font-semibold text-zinc-100">Military Personnel</h1>
          <p className="mt-1 text-sm text-zinc-500">{statusText}</p>
        </div>
        <Button
          type="button"
          disabled={!canCreate}
          onClick={() => {
            setEditing(null)
            setModalOpen(true)
          }}
        >
          <Plus className="size-4" />
          Create
        </Button>
      </div>

      <PersonnelFilters
        filters={filters}
        specialties={dictionaries?.specialties ?? []}
        onChange={(nextFilters) => setFilters({ ...filters, ...nextFilters })}
      />

      {error ? (
        <Card className="border-red-950 bg-red-950/20 text-sm text-red-200">Unable to load personnel data</Card>
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
            deleteMutation.mutate(id)
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
