import { Boxes, ExternalLink, FileText, Plus, Settings } from "lucide-react"
import { useQuery } from "@tanstack/react-query"
import type { ReactNode } from "react"
import { useEffect, useState } from "react"
import { useTranslation } from "react-i18next"
import { useSearchParams } from "react-router-dom"
import { useCurrentUserQuery } from "@/features/auth/api/authQueries"
import { useAssignInventoryAttributeMutation, useCreateInventoryAttributeTypeMutation, useDeleteInventoryMutation, useInventoryAttributeSchemaQuery, useInventoryAttributeTypesQuery, useInventoryDictionariesQuery, useInventoryQuery, useInventoryStatsQuery, useInventoryTypePassportQuery, useSaveInventoryCategoryMutation, useSaveInventoryTypeMutation, useUpdateInventoryMutation } from "@/features/inventory/api/inventoryQueries"
import type { DynamicAttributeMetadata, EquipmentTypePassport, EquipmentTypeRequest, InventoryCategory, InventoryFilter, InventoryRow, InventoryType, WeaponTypePassport, WeaponTypeRequest } from "@/features/inventory/model/inventoryTypes"
import { FiltersPanel } from "@/features/inventory/ui/FiltersPanel"
import { InventoryDialog } from "@/features/inventory/ui/InventoryDialog"
import { InventoryTable } from "@/features/inventory/ui/InventoryTable"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"
import { ErrorState } from "@/shared/ui/state"
import { TableSkeleton } from "@/shared/ui/skeleton"
import { toast } from "@/shared/ui/toast"
import { Pagination } from "@/shared/ui/pagination"
import { lookupApi } from "@/shared/api/lookupApi"
import { SearchableSelect } from "@/shared/ui/searchable-select"

export function EquipmentPage() {
  return <InventoryResourcePage kind="equipment" icon={<Boxes className="h-4 w-4 shrink-0" />} />
}

export function InventoryResourcePage({ kind, icon }: { kind: "equipment" | "weapons"; icon: ReactNode }) {
  const namespace = kind === "equipment" ? "equipment" : "weapons"
  const { t } = useTranslation(["common", "equipment", "weapons"])
  const [filters, setFilters] = useState<InventoryFilter>({ page: 0, size: 10 })
  const [editing, setEditing] = useState<InventoryRow | null>(null)
  const [inventoryDialogOpen, setInventoryDialogOpen] = useState(false)
  const [prefillUnitId, setPrefillUnitId] = useState<number | null>(null)
  const [prefillTypeId, setPrefillTypeId] = useState<number | null>(null)
  const [typeDialogOpen, setTypeDialogOpen] = useState(false)
  const [selectedTypeId, setSelectedTypeId] = useState<number | null>(null)
  const { data: user } = useCurrentUserQuery()
  const [searchParams, setSearchParams] = useSearchParams()
  const { data, error, isLoading } = useInventoryQuery(kind, filters)
  const { data: stats } = useInventoryStatsQuery(kind)
  const { data: dictionaries } = useInventoryDictionariesQuery(kind)
  const updateMutation = useUpdateInventoryMutation(kind)
  const deleteMutation = useDeleteInventoryMutation(kind)
  const saveCategoryMutation = useSaveInventoryCategoryMutation(kind)
  const saveTypeMutation = useSaveInventoryTypeMutation(kind)
  const typePassportQuery = useInventoryTypePassportQuery(kind, selectedTypeId)
  const { data: unitOptions = [] } = useQuery({
    queryKey: ["lookups", "units", kind],
    queryFn: () => lookupApi.units(),
    staleTime: 5 * 60_000,
  })
  const typeOptions = (dictionaries?.types ?? []).map((type) => ({
    id: type.id,
    label: type.name,
    parentLabel: type.categoryName,
    type: kind === "equipment" ? "EQUIPMENT" : "WEAPON",
  }))
  const canEdit = user?.permissions.includes(`${kind === "equipment" ? "equipment" : "weapon"}:update`) ?? false
  const canManageDictionary = user?.roles.includes("ADMIN_DISTRICT") ?? false

  useEffect(() => {
    const unitId = Number(searchParams.get("unitId"))
    const action = searchParams.get("action")
    if (Number.isFinite(unitId) && unitId > 0) {
      setFilters((current) => ({ ...current, page: 0, unitId }))
      setPrefillUnitId(unitId)
      if (action === "add" && canEdit) {
        setEditing(null)
        setInventoryDialogOpen(true)
        setSearchParams((current) => {
          current.delete("action")
          return current
        }, { replace: true })
      }
    }
  }, [canEdit, searchParams, setSearchParams])

  return (
    <div className="space-y-5">
      <Header
        title={t(`${namespace}:page.title`)}
        eyebrow={t(`${namespace}:page.eyebrow`)}
        description={t(`${namespace}:page.description`)}
        icon={icon}
        readiness={stats?.readinessScore ?? 0}
        total={stats?.totalQuantity ?? 0}
        warnings={stats?.warningRows ?? 0}
        actions={(
          <div className="flex flex-wrap gap-2">
            <Button type="button" disabled={!canEdit} onClick={() => {
              setEditing(null)
              setPrefillUnitId(filters.unitId ?? null)
              setPrefillTypeId(null)
              setInventoryDialogOpen(true)
            }}>
              <Plus className="h-4 w-4 shrink-0" />
              {t(`${namespace}:actions.addInventory`)}
            </Button>
            <Button type="button" variant="secondary" disabled={!canManageDictionary} onClick={() => setTypeDialogOpen(true)}>
              <Settings className="h-4 w-4 shrink-0" />
              {t(`${namespace}:actions.manageTypes`)}
            </Button>
          </div>
        )}
      />
      <FiltersPanel filters={filters} categories={dictionaries?.categories} types={dictionaries?.types} onChange={setFilters} />
      {isLoading ? (
        <TableSkeleton columns={6} />
      ) : error ? (
        <ErrorState title={t(`${namespace}:error`)} />
      ) : (
        <InventoryTable rows={data?.content ?? []} canEdit={canEdit} onEdit={(row) => {
          setEditing(row)
          setInventoryDialogOpen(true)
        }} onOpenType={setSelectedTypeId} onDelete={(row) => deleteMutation.mutate({ unitId: row.unitId, typeId: row.typeId }, {
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
        open={inventoryDialogOpen}
        saving={updateMutation.isPending}
        unitOptions={unitOptions}
        typeOptions={typeOptions}
        initialUnitId={prefillUnitId}
        initialTypeId={prefillTypeId}
        onClose={() => {
          setInventoryDialogOpen(false)
          setEditing(null)
        }}
        onSubmit={(request) => {
          updateMutation.mutate(request, {
            onSuccess: () => {
              toast.success(editing ? t(`${namespace}:toast.updated`) : t(`${namespace}:toast.created`))
              setInventoryDialogOpen(false)
              setEditing(null)
            },
            onError: () => toast.error(t(`${namespace}:toast.updateFailed`)),
          })
        }}
      />
      <InventoryTypeDialog
        kind={kind}
        open={typeDialogOpen}
        categories={dictionaries?.categories ?? []}
        types={dictionaries?.types ?? []}
        saving={saveCategoryMutation.isPending || saveTypeMutation.isPending}
        onClose={() => setTypeDialogOpen(false)}
        onCreateCategory={(name) => saveCategoryMutation.mutate({ name }, {
          onSuccess: () => toast.success(t(`${namespace}:toast.categoryCreated`)),
          onError: () => toast.error(t(`${namespace}:toast.updateFailed`)),
        })}
        onCreateType={(request) => saveTypeMutation.mutate(request, {
          onSuccess: () => toast.success(t(`${namespace}:toast.typeCreated`)),
          onError: () => toast.error(t(`${namespace}:toast.updateFailed`)),
        })}
      />
      <TypePassportDialog
        open={Boolean(selectedTypeId)}
        loading={typePassportQuery.isLoading}
        passport={typePassportQuery.data}
        kind={kind}
        canEdit={canEdit}
        onAddToUnit={(typeId) => {
          setPrefillTypeId(typeId)
          setPrefillUnitId(filters.unitId ?? null)
          setSelectedTypeId(null)
          setEditing(null)
          setInventoryDialogOpen(true)
        }}
        onOpenUnits={(typeId) => {
          setFilters({ page: 0, size: filters.size ?? 10, typeId })
          setSelectedTypeId(null)
        }}
        onClose={() => setSelectedTypeId(null)}
      />
    </div>
  )
}

function Header({ title, eyebrow, description, icon, readiness, total, warnings, actions }: { title: string; eyebrow: string; description: string; icon: ReactNode; readiness: number; total: number; warnings: number; actions?: ReactNode }) {
  const { t } = useTranslation(["common", "equipment"])
  return (
    <div className="grid gap-4 lg:grid-cols-[1fr_180px_180px_180px]">
      <div className="min-w-0">
        <div className="flex min-w-0 items-center gap-2 text-xs uppercase text-emerald-300">{icon}<span className="truncate" title={eyebrow}>{eyebrow}</span></div>
        <h1 className="mt-1 break-words text-2xl font-semibold text-zinc-100">{title}</h1>
        <p className="mt-1 break-words text-sm text-zinc-500">{description}</p>
        {actions ? <div className="mt-4">{actions}</div> : null}
      </div>
      <Metric label={t("equipment:metric.readiness")} value={`${readiness}%`} />
      <Metric label={t("equipment:metric.totalQty")} value={total} />
      <Metric label={t("equipment:metric.warnings")} value={warnings} />
    </div>
  )
}

function InventoryTypeDialog({
  kind,
  open,
  categories,
  types,
  saving,
  onClose,
  onCreateCategory,
  onCreateType,
}: {
  kind: "equipment" | "weapons"
  open: boolean
  categories: InventoryCategory[]
  types: InventoryType[]
  saving: boolean
  onClose: () => void
  onCreateCategory: (name: string) => void
  onCreateType: (request: EquipmentTypeRequest | WeaponTypeRequest) => void
}) {
  const namespace = kind === "equipment" ? "equipment" : "weapons"
  const { t } = useTranslation(["common", "equipment", "weapons"])
  const [categoryName, setCategoryName] = useState("")
  const [typeName, setTypeName] = useState("")
  const [categoryId, setCategoryId] = useState<number | null>(categories[0]?.id ?? null)
  const [attributeValues, setAttributeValues] = useState<Record<number, string>>({})
  const [attributeName, setAttributeName] = useState("")
  const [attributeType, setAttributeType] = useState<DynamicAttributeMetadata["dataType"]>("text")
  const [assignAttributeId, setAssignAttributeId] = useState<number | null>(null)
  const [assignRequired, setAssignRequired] = useState(false)
  const schemaQuery = useInventoryAttributeSchemaQuery(kind, categoryId)
  const attributeTypesQuery = useInventoryAttributeTypesQuery(kind)
  const createAttributeMutation = useCreateInventoryAttributeTypeMutation(kind)
  const assignAttributeMutation = useAssignInventoryAttributeMutation(kind)

  useEffect(() => {
    if (!categoryId && categories[0]?.id) {
      setCategoryId(categories[0].id)
    }
  }, [categories, categoryId])

  if (!open) {
    return null
  }

  const categoryOptions = categories.map((category) => ({ id: category.id, label: category.name }))
  const selectedCategory = categories.find((category) => category.id === categoryId) ?? null
  const categoryTypes = types.filter((type) => type.categoryId === categoryId)
  const categoryAttributes = schemaQuery.data ?? []

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
      <div className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-md border border-zinc-800 bg-zinc-950 p-5 shadow-2xl">
        <div className="text-xs uppercase text-emerald-300">{t(`${namespace}:actions.manageTypes`)}</div>
        <h2 className="mt-1 text-xl font-semibold text-zinc-100">{t(`${namespace}:dictionary.title`)}</h2>
        <div className="mt-5 grid gap-4 lg:grid-cols-[minmax(0,1fr)_260px]">
          <div className="space-y-4">
            <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-4">
              <div className="text-sm font-medium text-zinc-100">{t(`${namespace}:dictionary.newType`)}</div>
              <div className="mt-3 grid gap-3 md:grid-cols-2">
                <Field label={t(`${namespace}:dictionary.typeName`)} value={typeName} onChange={setTypeName} />
                <SearchableSelect label={t("table.category")} value={categoryId} options={categoryOptions} placeholder={t(`${namespace}:dictionary.selectCategory`)} onChange={setCategoryId} />
                {schemaQuery.isLoading ? (
                  <div className="text-sm text-zinc-500">{t("states.loading")}</div>
                ) : schemaQuery.data?.length ? (
                  schemaQuery.data.map((attribute) => (
                    <DynamicAttributeField
                      key={attribute.id}
                      attribute={attribute}
                      value={attributeValues[attribute.id] ?? ""}
                      onChange={(value) => setAttributeValues((current) => ({ ...current, [attribute.id]: value }))}
                    />
                  ))
                ) : (
                  <div className="rounded-md border border-zinc-800 bg-zinc-950 p-3 text-sm text-zinc-500 md:col-span-2">
                    {t(`${namespace}:dictionary.noAttributes`)}
                  </div>
                )}
              </div>
              <Button className="mt-4" disabled={saving || !typeName.trim() || !categoryId} onClick={() => {
                if (!categoryId) return
                const request = {
                  name: typeName,
                  categoryId,
                  attributes: (schemaQuery.data ?? []).map((attribute) => ({
                    attributeId: attribute.id,
                    value: attributeValues[attribute.id] ?? "",
                  })),
                } satisfies EquipmentTypeRequest | WeaponTypeRequest
                onCreateType(request)
                setTypeName("")
                setAttributeValues({})
              }}>
                <Plus className="h-4 w-4 shrink-0" />
                {t("actions.create")}
              </Button>
            </div>
            <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-4">
              <div className="text-sm font-medium text-zinc-100">{t(`${namespace}:dictionary.newCategory`)}</div>
              <div className="mt-3 flex flex-col gap-2 sm:flex-row">
                <input value={categoryName} onChange={(event) => setCategoryName(event.target.value)} className="h-10 flex-1 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500" />
                <Button disabled={saving || !categoryName.trim()} onClick={() => {
                  onCreateCategory(categoryName)
                  setCategoryName("")
                }}>{t("actions.create")}</Button>
              </div>
            </div>
            <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-4">
              <div className="text-sm font-medium text-zinc-100">{t(`${namespace}:dictionary.attributes`)}</div>
              <div className="mt-3 grid gap-3 md:grid-cols-[1fr_150px]">
                <Field label={t(`${namespace}:dictionary.attributeName`)} value={attributeName} onChange={setAttributeName} />
                <label className="space-y-2">
                  <span className="text-xs uppercase text-zinc-500">{t(`${namespace}:dictionary.attributeType`)}</span>
                  <select value={attributeType} onChange={(event) => setAttributeType(event.target.value as DynamicAttributeMetadata["dataType"])} className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500">
                    <option value="text">text</option>
                    <option value="number">number</option>
                    <option value="date">date</option>
                    <option value="boolean">boolean</option>
                  </select>
                </label>
              </div>
              <Button className="mt-3" variant="secondary" disabled={createAttributeMutation.isPending || !attributeName.trim()} onClick={() => {
                createAttributeMutation.mutate({ name: attributeName, dataType: attributeType }, {
                  onSuccess: () => {
                    toast.success(t(`${namespace}:toast.attributeCreated`))
                    setAttributeName("")
                  },
                  onError: () => toast.error(t(`${namespace}:toast.updateFailed`)),
                })
              }}>{t(`${namespace}:dictionary.createAttribute`)}</Button>
              <div className="mt-4 grid gap-3 md:grid-cols-[1fr_auto]">
                <SearchableSelect
                  label={t(`${namespace}:dictionary.assignAttribute`)}
                  value={assignAttributeId}
                  options={(attributeTypesQuery.data ?? []).map((attribute) => ({ id: attribute.id, label: attribute.name, description: attribute.dataType }))}
                  placeholder={t(`${namespace}:dictionary.selectAttribute`)}
                  onChange={setAssignAttributeId}
                />
                <label className="mt-6 flex h-10 items-center gap-2 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-300">
                  <input type="checkbox" checked={assignRequired} onChange={(event) => setAssignRequired(event.target.checked)} className="h-4 w-4 shrink-0 accent-emerald-400" />
                  {t(`${namespace}:dictionary.required`)}
                </label>
              </div>
              <Button className="mt-3" variant="secondary" disabled={assignAttributeMutation.isPending || !categoryId || !assignAttributeId} onClick={() => {
                if (!categoryId || !assignAttributeId) return
                assignAttributeMutation.mutate({ categoryId, attributeId: assignAttributeId, required: assignRequired }, {
                  onSuccess: () => {
                    toast.success(t(`${namespace}:toast.attributeAssigned`))
                    setAssignAttributeId(null)
                    setAssignRequired(false)
                  },
                  onError: () => toast.error(t(`${namespace}:toast.updateFailed`)),
                })
              }}>{t(`${namespace}:dictionary.addToCategory`)}</Button>
            </div>
          </div>
          <div className="space-y-4">
            <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-4">
              <div className="text-sm font-medium text-zinc-100">{t(`${namespace}:dictionary.categoryPassport`)}</div>
              <div className="mt-3 max-h-44 space-y-2 overflow-y-auto">
                {categories.map((category) => (
                  <button
                    key={category.id}
                    type="button"
                    onClick={() => setCategoryId(category.id)}
                    className={`w-full rounded border px-3 py-2 text-left transition ${category.id === categoryId ? "border-emerald-500/50 bg-emerald-500/10" : "border-zinc-800 bg-zinc-950 hover:bg-zinc-900"}`}
                  >
                    <div className="truncate text-sm text-zinc-100" title={category.name}>{category.name}</div>
                    <div className="text-xs text-zinc-500">{t(`${namespace}:dictionary.typesCount`, { count: types.filter((type) => type.categoryId === category.id).length })}</div>
                  </button>
                ))}
              </div>
              {selectedCategory ? (
                <div className="mt-4 rounded border border-zinc-800 bg-zinc-950 p-3">
                  <div className="truncate text-xs uppercase text-emerald-300" title={selectedCategory.name}>{selectedCategory.name}</div>
                  <div className="mt-2 grid grid-cols-2 gap-2 text-xs text-zinc-400">
                    <div className="rounded border border-zinc-800 p-2">
                      <div className="text-zinc-500">{t(`${namespace}:dictionary.currentTypes`)}</div>
                      <div className="mt-1 text-lg font-semibold text-zinc-100">{categoryTypes.length}</div>
                    </div>
                    <div className="rounded border border-zinc-800 p-2">
                      <div className="text-zinc-500">{t(`${namespace}:dictionary.attributes`)}</div>
                      <div className="mt-1 text-lg font-semibold text-zinc-100">{categoryAttributes.length}</div>
                    </div>
                  </div>
                  <div className="mt-3 flex flex-wrap gap-1.5">
                    {categoryAttributes.length ? categoryAttributes.map((attribute) => (
                      <span key={attribute.id} className="rounded border border-zinc-800 bg-zinc-900 px-2 py-1 text-xs text-zinc-300">
                        {attribute.name}
                      </span>
                    )) : <span className="text-xs text-zinc-600">{t(`${namespace}:dictionary.noAttributes`)}</span>}
                  </div>
                </div>
              ) : null}
            </div>
            <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-4">
              <div className="text-sm font-medium text-zinc-100">{t(`${namespace}:dictionary.typesInCategory`)}</div>
              <div className="mt-3 max-h-72 space-y-2 overflow-y-auto">
                {(categoryId ? categoryTypes : types).map((type) => (
                <div key={type.id} className="rounded border border-zinc-800 bg-zinc-950 px-3 py-2">
                  <div className="truncate text-sm text-zinc-100" title={type.name}>{type.name}</div>
                  <div className="truncate text-xs text-zinc-500" title={type.categoryName}>{type.categoryName}</div>
                </div>
              ))}
              </div>
            </div>
          </div>
        </div>
        <div className="mt-5 flex justify-end">
          <Button type="button" variant="secondary" onClick={onClose}>{t("actions.close")}</Button>
        </div>
      </div>
    </div>
  )
}

function TypePassportDialog({
  open,
  loading,
  passport,
  kind,
  canEdit,
  onAddToUnit,
  onOpenUnits,
  onClose,
}: {
  open: boolean
  loading: boolean
  passport?: EquipmentTypePassport | WeaponTypePassport
  kind: "equipment" | "weapons"
  canEdit: boolean
  onAddToUnit: (typeId: number) => void
  onOpenUnits: (typeId: number) => void
  onClose: () => void
}) {
  const namespace = kind === "equipment" ? "equipment" : "weapons"
  const { t } = useTranslation(["common", "equipment", "weapons"])
  const [tab, setTab] = useState("overview")
  if (!open) {
    return null
  }
  const tabs = ["overview", "attributes", "distribution", "units", "alerts", "actions"]
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
      <div className="max-h-[90vh] w-full max-w-5xl overflow-y-auto rounded-md border border-zinc-800 bg-zinc-950 p-5 shadow-2xl">
        {loading || !passport ? (
          <div className="text-sm text-zinc-400">{t("states.loading")}</div>
        ) : (
          <div className="space-y-5">
            <div>
              <div className="text-xs uppercase text-emerald-300">{t(`${namespace}:passport.title`)}</div>
              <h2 className="mt-1 break-words text-2xl font-semibold text-zinc-100">{passport.name}</h2>
              <p className="mt-2 break-words text-sm text-zinc-500">{passport.categoryName} / {passport.description || passport.purpose || t("states.notAvailable")}</p>
            </div>

            <div className="flex gap-2 overflow-x-auto border-b border-zinc-800 pb-2">
              {tabs.map((item) => (
                <button
                  key={item}
                  type="button"
                  onClick={() => setTab(item)}
                  className={`shrink-0 rounded-md px-3 py-2 text-xs transition ${tab === item ? "bg-emerald-500 text-zinc-950" : "bg-zinc-900 text-zinc-400 hover:text-zinc-100"}`}
                >
                  {t(`${namespace}:passport.tabs.${item}`)}
                </button>
              ))}
            </div>

            {tab === "overview" ? (
              <>
                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  <Metric label={t("table.quantity")} value={passport.totalQuantity} />
                  <Metric label={t("fields.unitsCount")} value={passport.unitsCount} />
                  <Metric label={t("fields.adoptionYear")} value={passport.adoptionYear ?? t("states.notAvailable")} />
                  <Metric label={t("fields.manufacturer")} value={passport.manufacturer ?? t("states.notAvailable")} />
                </div>
                <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-4">
                  <div className="mb-2 text-sm font-medium text-zinc-100">{t("fields.description")}</div>
                  <p className="break-words text-sm text-zinc-400">{passport.description || passport.purpose || t("states.notAvailable")}</p>
                </div>
              </>
            ) : null}

            {tab === "attributes" ? (
              <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-4">
                <div className="mb-3 text-sm font-medium text-zinc-100">{t("fields.attributes")}</div>
                <div className="grid gap-2 sm:grid-cols-2">
                  {passport.attributes?.length ? passport.attributes.map((attribute) => (
                    <div key={attribute.id} className="min-w-0 rounded border border-zinc-800 bg-zinc-950 px-3 py-2">
                      <div className="truncate text-xs uppercase text-zinc-500" title={attribute.name}>{attribute.name}</div>
                      <div className="mt-1 break-words text-sm text-zinc-100">{attribute.displayValue || t("states.notAvailable")}</div>
                    </div>
                  )) : <div className="text-sm text-zinc-500">{t(`${namespace}:passport.noAttributes`)}</div>}
                </div>
              </div>
            ) : null}

            {tab === "distribution" || tab === "units" ? (
              <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-4">
              <div className="mb-3 text-sm font-medium text-zinc-100">{t("fields.distribution")}</div>
              <div className="max-h-96 space-y-2 overflow-y-auto">
                {passport.distribution?.map((row: InventoryRow) => (
                  <div key={`${row.unitId}:${row.typeId}`} className="flex items-center justify-between gap-3 rounded border border-zinc-800 bg-zinc-950 px-3 py-2 text-sm">
                    <span className="min-w-0">
                      <span className="block truncate text-zinc-300" title={row.unitName}>{row.unitName}</span>
                      <span className="block truncate text-xs text-zinc-500" title={row.categoryName}>{row.categoryName}</span>
                    </span>
                    <span className="shrink-0 rounded border border-zinc-800 bg-zinc-900 px-2 py-1 font-medium text-zinc-100">{row.quantity}</span>
                  </div>
                ))}
                {!passport.distribution?.length ? <div className="text-sm text-zinc-500">{t(`${namespace}:passport.noDistribution`)}</div> : null}
              </div>
              </div>
            ) : null}

            {tab === "alerts" ? (
              <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-4 text-sm text-zinc-400">
                {passport.totalQuantity <= 0 ? t(`${namespace}:passport.noQuantityAlert`) : t(`${namespace}:passport.noLinkedAlerts`)}
              </div>
            ) : null}

            {tab === "actions" ? (
              <div className="grid gap-3 sm:grid-cols-2">
                <Button type="button" disabled={!canEdit} onClick={() => onAddToUnit(passport.id)}>
                  <Plus className="h-4 w-4 shrink-0" />
                  {t(`${namespace}:passport.addToUnit`)}
                </Button>
                <Button type="button" variant="secondary" onClick={() => onOpenUnits(passport.id)}>
                  <ExternalLink className="h-4 w-4 shrink-0" />
                  {t(`${namespace}:passport.openUnits`)}
                </Button>
                <Button type="button" variant="secondary" disabled>
                  <FileText className="h-4 w-4 shrink-0" />
                  {t(`${namespace}:passport.reportByType`)}
                </Button>
              </div>
            ) : null}
          </div>
        )}
        <div className="mt-5 flex justify-end">
          <Button type="button" variant="secondary" onClick={onClose}>{t("actions.close")}</Button>
        </div>
      </div>
    </div>
  )
}

function DynamicAttributeField({ attribute, value, onChange }: { attribute: DynamicAttributeMetadata; value: string; onChange: (value: string) => void }) {
  if (attribute.dataType === "boolean") {
    return (
      <label className="flex h-10 items-center gap-2 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-300">
        <input
          type="checkbox"
          checked={value === "true"}
          required={attribute.required}
          onChange={(event) => onChange(event.target.checked ? "true" : "false")}
          className="h-4 w-4 shrink-0 accent-emerald-400"
        />
        <span className="truncate" title={attribute.name}>{attribute.name}</span>
      </label>
    )
  }
  return (
    <Field
      label={attribute.required ? `${attribute.name} *` : attribute.name}
      value={value}
      type={attribute.dataType === "number" ? "number" : attribute.dataType === "date" ? "date" : "text"}
      optional={!attribute.required}
      onChange={onChange}
    />
  )
}

function Field({ label, value, type = "text", optional, onChange }: { label: string; value: string; type?: string; optional?: boolean; onChange: (value: string) => void }) {
  return (
    <label className="space-y-2">
      <span className="text-xs uppercase text-zinc-500">{label}</span>
      <input
        type={type}
        required={!optional}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
      />
    </label>
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
