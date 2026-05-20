import { Save, X } from "lucide-react"
import { useQuery } from "@tanstack/react-query"
import { useEffect, useMemo, useState } from "react"
import { useTranslation } from "react-i18next"
import type { Personnel, PersonnelRequest, Rank, Specialty } from "@/features/personnel/model/personnelTypes"
import { apiClient } from "@/shared/api/apiClient"
import { lookupApi, type LookupOption } from "@/shared/api/lookupApi"
import { Button } from "@/shared/ui/button"
import { SearchableSelect } from "@/shared/ui/searchable-select"

type PersonnelEditModalProps = {
  open: boolean
  personnel: Personnel | null
  ranks: Rank[]
  specialties: Specialty[]
  subdivisionOptions: LookupOption[]
  saving: boolean
  onClose: () => void
  onSubmit: (request: PersonnelRequest) => void
}

const emptyForm: PersonnelRequest = {
  lastName: "",
  firstName: "",
  middleName: "",
  personalNumber: "",
  birthDate: "1998-01-01",
  serviceStart: new Date().toISOString().slice(0, 10),
  subdivisionId: 0,
  rankId: null,
  rankAssignmentDate: new Date().toISOString().slice(0, 10),
  specialtyIds: [],
  rankAttributes: [],
}

export function PersonnelEditModal({
  open,
  personnel,
  ranks,
  specialties,
  subdivisionOptions,
  saving,
  onClose,
  onSubmit,
}: PersonnelEditModalProps) {
  const { t } = useTranslation(["common", "personnel"])
  const [form, setForm] = useState<PersonnelRequest>(emptyForm)
  const [rankAttributeValues, setRankAttributeValues] = useState<Record<number, string>>({})
  const [step, setStep] = useState(0)
  const [armyId, setArmyId] = useState<number | null>(null)
  const [formationId, setFormationId] = useState<number | null>(null)
  const [unitId, setUnitId] = useState<number | null>(null)
  const [companyId, setCompanyId] = useState<number | null>(null)
  const [platoonId, setPlatoonId] = useState<number | null>(null)
  const [squadId, setSquadId] = useState<number | null>(null)
  const rankSchemaQuery = useQuery({
    queryKey: ["attributes", "rank", form.rankId],
    queryFn: () => apiClient<Array<{ id: number; name: string; dataType: "text" | "number" | "date" | "boolean"; required: boolean }>>(`/api/attributes/ranks/${form.rankId}`),
    enabled: Boolean(open && form.rankId),
    staleTime: 5 * 60_000,
  })
  const armiesQuery = useQuery({
    queryKey: ["lookups", "formations", "army", open],
    queryFn: () => lookupApi.formations("", "ARMY", { limit: 100 }),
    enabled: open,
  })
  const formationsQuery = useQuery({
    queryKey: ["lookups", "formations", "children", armyId],
    queryFn: () => lookupApi.formations("", "CORPS,DIVISION,BRIGADE", { parentId: armyId, limit: 100 }),
    enabled: open && Boolean(armyId),
  })
  const unitsQuery = useQuery({
    queryKey: ["lookups", "units", formationId],
    queryFn: () => lookupApi.units("", { formationId, limit: 100 }),
    enabled: open && Boolean(formationId),
  })
  const companiesQuery = useQuery({
    queryKey: ["lookups", "subdivisions", "companies", unitId],
    queryFn: () => lookupApi.subdivisions("", { unitId, type: "COMPANY", limit: 100 }),
    enabled: open && Boolean(unitId),
  })
  const platoonsQuery = useQuery({
    queryKey: ["lookups", "subdivisions", "platoons", companyId],
    queryFn: () => lookupApi.subdivisions("", { parentId: companyId, type: "PLATOON", limit: 100 }),
    enabled: open && Boolean(companyId),
  })
  const squadsQuery = useQuery({
    queryKey: ["lookups", "subdivisions", "squads", platoonId],
    queryFn: () => lookupApi.subdivisions("", { parentId: platoonId, type: "SQUAD", limit: 100 }),
    enabled: open && Boolean(platoonId),
  })

  useEffect(() => {
    if (!open) {
      return
    }
    if (!personnel) {
      setForm(emptyForm)
      resetCascade()
      setStep(0)
      return
    }
    setForm({
      lastName: personnel.lastName,
      firstName: personnel.firstName,
      middleName: personnel.middleName ?? "",
      personalNumber: personnel.personalNumber,
      birthDate: personnel.birthDate,
      serviceStart: personnel.serviceStart,
      subdivisionId: personnel.subdivisionId,
      rankId: personnel.rank?.id ?? null,
      rankAssignmentDate: new Date().toISOString().slice(0, 10),
      specialtyIds: personnel.specialties.map((specialty) => specialty.id),
      rankAttributes: [],
    })
    setRankAttributeValues({})
    resetCascade()
    setStep(0)
  }, [open, personnel])

  const title = useMemo(() => (personnel ? t("personnel:form.edit") : t("personnel:form.create")), [personnel, t])
  const steps = [
    t("personnel:wizard.basic"),
    t("personnel:wizard.assignment"),
    t("personnel:wizard.specialties"),
    t("personnel:wizard.rankAttributes"),
    t("personnel:wizard.confirm"),
  ]
  const selectedSubdivision = subdivisionOptions.find((option) => option.id === form.subdivisionId)
  const selectedPath = [
    armiesQuery.data?.find((item) => item.id === armyId)?.label,
    formationsQuery.data?.find((item) => item.id === formationId)?.label,
    unitsQuery.data?.find((item) => item.id === unitId)?.label,
    companiesQuery.data?.find((item) => item.id === companyId)?.label,
    platoonsQuery.data?.find((item) => item.id === platoonId)?.label,
    squadsQuery.data?.find((item) => item.id === squadId)?.label,
  ].filter(Boolean).join(" → ")

  function resetCascade() {
    setArmyId(null)
    setFormationId(null)
    setUnitId(null)
    setCompanyId(null)
    setPlatoonId(null)
    setSquadId(null)
  }

  function applyAssignment(next: Partial<{
    armyId: number | null
    formationId: number | null
    unitId: number | null
    companyId: number | null
    platoonId: number | null
    squadId: number | null
    subdivisionId: number
  }>) {
    if ("armyId" in next) setArmyId(next.armyId ?? null)
    if ("formationId" in next) setFormationId(next.formationId ?? null)
    if ("unitId" in next) setUnitId(next.unitId ?? null)
    if ("companyId" in next) setCompanyId(next.companyId ?? null)
    if ("platoonId" in next) setPlatoonId(next.platoonId ?? null)
    if ("squadId" in next) setSquadId(next.squadId ?? null)
    setForm((current) => ({ ...current, subdivisionId: next.subdivisionId ?? current.subdivisionId }))
  }

  if (!open) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/70 p-4">
      <form
        className="max-h-[90vh] w-full max-w-3xl overflow-y-auto rounded-md border border-zinc-800 bg-zinc-950 shadow-2xl"
        onSubmit={(event) => {
          event.preventDefault()
          onSubmit({
            ...form,
            rankAttributes: (rankSchemaQuery.data ?? []).map((attribute) => ({
              attributeId: attribute.id,
              value: rankAttributeValues[attribute.id] ?? "",
            })),
          })
        }}
      >
        <div className="flex items-center justify-between border-b border-zinc-800 px-5 py-4">
          <div>
            <div className="text-sm font-semibold uppercase text-zinc-100">{title}</div>
            <div className="text-xs text-zinc-500">{t("personnel:form.record")}</div>
          </div>
          <Button type="button" variant="ghost" className="h-9 w-9 shrink-0 px-0" onClick={onClose}>
            <X className="h-4 w-4 shrink-0" />
          </Button>
        </div>

        <div className="border-b border-zinc-800 px-5 py-3">
          <div className="flex gap-2 overflow-x-auto">
            {steps.map((label, index) => (
              <button
                key={label}
                type="button"
                onClick={() => setStep(index)}
                className={`shrink-0 rounded-md px-3 py-2 text-xs transition ${step === index ? "bg-emerald-500 text-zinc-950" : "bg-zinc-900 text-zinc-400 hover:text-zinc-100"}`}
              >
                {index + 1}. {label}
              </button>
            ))}
          </div>
        </div>

        <div className="grid gap-4 p-5 md:grid-cols-2">
          {step === 0 ? (
            <>
              <Field label={t("personnel:form.lastName")} value={form.lastName} onChange={(value) => setForm({ ...form, lastName: value })} />
              <Field label={t("personnel:form.firstName")} value={form.firstName} onChange={(value) => setForm({ ...form, firstName: value })} />
              <Field label={t("personnel:form.middleName")} optional value={form.middleName ?? ""} onChange={(value) => setForm({ ...form, middleName: value })} />
              <Field label={t("personnel:form.personalNumber")} value={form.personalNumber} onChange={(value) => setForm({ ...form, personalNumber: value })} />
              <Field label={t("personnel:form.birthDate")} type="date" value={form.birthDate} onChange={(value) => setForm({ ...form, birthDate: value })} />
              <Field label={t("personnel:form.serviceStart")} type="date" value={form.serviceStart} onChange={(value) => setForm({ ...form, serviceStart: value })} />
              <SearchableSelect
                label={t("personnel:form.rank")}
                value={form.rankId ?? null}
                options={ranks.map((rank) => ({ id: rank.id, label: rank.name, description: rank.category }))}
                placeholder={t("personnel:table.noRank")}
                onChange={(value) => {
                  setForm({ ...form, rankId: value })
                  setRankAttributeValues({})
                }}
              />
            </>
          ) : null}

          {step === 1 ? (
            <div className="grid gap-4 md:col-span-2 md:grid-cols-2">
              <SearchableSelect
                label={t("personnel:wizard.army")}
                value={armyId}
                options={armiesQuery.data ?? []}
                placeholder={t("personnel:wizard.selectArmy")}
                onChange={(value) => applyAssignment({
                  armyId: value,
                  formationId: null,
                  unitId: null,
                  companyId: null,
                  platoonId: null,
                  squadId: null,
                  subdivisionId: 0,
                })}
              />
              <SearchableSelect
                label={t("personnel:wizard.formation")}
                value={formationId}
                options={formationsQuery.data ?? []}
                placeholder={t("personnel:wizard.selectFormation")}
                disabled={!armyId}
                onChange={(value) => applyAssignment({
                  formationId: value,
                  unitId: null,
                  companyId: null,
                  platoonId: null,
                  squadId: null,
                  subdivisionId: 0,
                })}
              />
              <SearchableSelect
                label={t("personnel:wizard.unit")}
                value={unitId}
                options={unitsQuery.data ?? []}
                placeholder={t("personnel:wizard.selectUnit")}
                disabled={!formationId}
                onChange={(value) => applyAssignment({
                  unitId: value,
                  companyId: null,
                  platoonId: null,
                  squadId: null,
                  subdivisionId: 0,
                })}
              />
              <SearchableSelect
                label={t("personnel:wizard.company")}
                value={companyId}
                options={companiesQuery.data ?? []}
                placeholder={t("personnel:wizard.selectCompany")}
                disabled={!unitId}
                onChange={(value) => applyAssignment({
                  companyId: value,
                  platoonId: null,
                  squadId: null,
                  subdivisionId: value ?? 0,
                })}
              />
              <SearchableSelect
                label={t("personnel:wizard.platoon")}
                value={platoonId}
                options={platoonsQuery.data ?? []}
                placeholder={t("personnel:wizard.selectPlatoon")}
                disabled={!companyId}
                onChange={(value) => applyAssignment({
                  platoonId: value,
                  squadId: null,
                  subdivisionId: value ?? companyId ?? 0,
                })}
              />
              <SearchableSelect
                label={t("personnel:wizard.squad")}
                value={squadId}
                options={squadsQuery.data ?? []}
                placeholder={t("personnel:wizard.selectSquad")}
                disabled={!platoonId}
                onChange={(value) => applyAssignment({
                  squadId: value,
                  subdivisionId: value ?? platoonId ?? companyId ?? 0,
                })}
              />
              <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-3 text-sm text-zinc-400 md:col-span-2">
                <div className="text-xs uppercase text-zinc-500">{t("personnel:wizard.selectedPath")}</div>
                <div className="mt-1 break-words text-zinc-200">
                  {selectedPath
                    || selectedSubdivision?.description
                    || selectedSubdivision?.parentLabel
                    || t("personnel:wizard.selectPath")}
                </div>
              </div>
              <SearchableSelect
                label={t("personnel:form.subdivision")}
                value={form.subdivisionId}
                options={subdivisionOptions}
                placeholder={t("personnel:form.selectSubdivision")}
                onChange={(value) => setForm({ ...form, subdivisionId: value ?? form.subdivisionId })}
              />
              <div className="rounded-md border border-zinc-800 bg-zinc-950 p-3 text-xs text-zinc-500">
                {t("personnel:wizard.directSearchHint")}
              </div>
            </div>
          ) : null}

          {step === 2 ? (
            <label className="space-y-2 md:col-span-2">
              <span className="text-xs uppercase text-zinc-500">{t("personnel:form.specialties")}</span>
              <div className="grid gap-2 rounded-md border border-zinc-800 bg-zinc-900 p-3 sm:grid-cols-2">
                {specialties.map((specialty) => (
                  <label key={specialty.id} className="flex min-w-0 items-center gap-2 text-sm text-zinc-300">
                    <input
                      type="checkbox"
                      className="h-4 w-4 shrink-0 accent-emerald-400"
                      checked={form.specialtyIds.includes(specialty.id)}
                      onChange={(event) => {
                        const specialtyIds = event.target.checked
                          ? [...form.specialtyIds, specialty.id]
                          : form.specialtyIds.filter((id) => id !== specialty.id)
                        setForm({ ...form, specialtyIds })
                      }}
                    />
                    <span className="truncate" title={specialty.name}>{specialty.name}</span>
                  </label>
                ))}
              </div>
            </label>
          ) : null}

          {step === 3 ? rankSchemaQuery.data?.length ? (
            <div className="grid gap-3 rounded-md border border-zinc-800 bg-zinc-900/50 p-3 md:col-span-2 md:grid-cols-2">
              {rankSchemaQuery.data.map((attribute) => (
                <DynamicRankAttributeField
                  key={attribute.id}
                  attribute={attribute}
                  value={rankAttributeValues[attribute.id] ?? ""}
                  onChange={(value) => setRankAttributeValues((current) => ({ ...current, [attribute.id]: value }))}
                />
              ))}
            </div>
          ) : (
            <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-3 text-sm text-zinc-500 md:col-span-2">
              {t("personnel:wizard.noRankAttributes")}
            </div>
          ) : null}

          {step === 4 ? (
            <div className="space-y-3 rounded-md border border-zinc-800 bg-zinc-900/50 p-4 text-sm md:col-span-2">
              <Summary label={t("personnel:form.lastName")} value={`${form.lastName} ${form.firstName} ${form.middleName ?? ""}`} />
              <Summary label={t("personnel:form.rank")} value={ranks.find((rank) => rank.id === form.rankId)?.name ?? t("personnel:table.noRank")} />
              <Summary label={t("personnel:form.subdivision")} value={subdivisionOptions.find((option) => option.id === form.subdivisionId)?.label ?? t("personnel:form.selectSubdivision")} />
              <Summary label={t("personnel:form.specialties")} value={`${form.specialtyIds.length}`} />
            </div>
          ) : null}
        </div>

        <div className="flex flex-col-reverse gap-2 border-t border-zinc-800 px-5 py-4 sm:flex-row sm:justify-end">
          <Button type="button" variant="secondary" onClick={onClose}>
            {t("actions.cancel")}
          </Button>
          {step > 0 ? (
            <Button type="button" variant="secondary" onClick={() => setStep((current) => Math.max(0, current - 1))}>
              {t("actions.previous")}
            </Button>
          ) : null}
          {step < steps.length - 1 ? (
            <Button type="button" onClick={() => setStep((current) => Math.min(steps.length - 1, current + 1))}>
              {t("actions.next")}
            </Button>
          ) : (
          <Button type="submit" disabled={saving || !form.subdivisionId}>
            <Save className="h-4 w-4 shrink-0" />
            {t("actions.save")}
          </Button>
          )}
        </div>
      </form>
    </div>
  )
}

function Summary({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex gap-3 rounded border border-zinc-800 bg-zinc-950 px-3 py-2">
      <span className="w-40 shrink-0 truncate text-zinc-500" title={label}>{label}</span>
      <span className="min-w-0 break-words text-zinc-100">{value}</span>
    </div>
  )
}

function Field({
  label,
  value,
  type = "text",
  optional = false,
  onChange,
}: {
  label: string
  value: string
  type?: string
  optional?: boolean
  onChange: (value: string) => void
}) {
  return (
    <label className="space-y-2">
      <span className="text-xs uppercase text-zinc-500">{label}</span>
      <input
        className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm outline-none"
        type={type}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        required={!optional}
      />
    </label>
  )
}

function DynamicRankAttributeField({
  attribute,
  value,
  onChange,
}: {
  attribute: { id: number; name: string; dataType: "text" | "number" | "date" | "boolean"; required: boolean }
  value: string
  onChange: (value: string) => void
}) {
  if (attribute.dataType === "boolean") {
    return (
      <label className="flex h-10 items-center gap-2 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-300">
        <input
          type="checkbox"
          checked={value === "true"}
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
