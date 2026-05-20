import { Save, X } from "lucide-react"
import { useQuery } from "@tanstack/react-query"
import { useEffect, useMemo, useState } from "react"
import { useTranslation } from "react-i18next"
import type { Personnel, PersonnelRequest, Rank, Specialty } from "@/features/personnel/model/personnelTypes"
import { apiClient } from "@/shared/api/apiClient"
import type { LookupOption } from "@/shared/api/lookupApi"
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
  subdivisionId: 12,
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
  const rankSchemaQuery = useQuery({
    queryKey: ["attributes", "rank", form.rankId],
    queryFn: () => apiClient<Array<{ id: number; name: string; dataType: "text" | "number" | "date" | "boolean"; required: boolean }>>(`/api/attributes/ranks/${form.rankId}`),
    enabled: Boolean(open && form.rankId),
    staleTime: 5 * 60_000,
  })

  useEffect(() => {
    if (!open) {
      return
    }
    if (!personnel) {
      setForm(emptyForm)
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
  }, [open, personnel])

  const title = useMemo(() => (personnel ? t("personnel:form.edit") : t("personnel:form.create")), [personnel, t])

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

        <div className="grid gap-4 p-5 md:grid-cols-2">
          <Field label={t("personnel:form.lastName")} value={form.lastName} onChange={(value) => setForm({ ...form, lastName: value })} />
          <Field label={t("personnel:form.firstName")} value={form.firstName} onChange={(value) => setForm({ ...form, firstName: value })} />
          <Field label={t("personnel:form.middleName")} optional value={form.middleName ?? ""} onChange={(value) => setForm({ ...form, middleName: value })} />
          <Field label={t("personnel:form.personalNumber")} value={form.personalNumber} onChange={(value) => setForm({ ...form, personalNumber: value })} />
          <Field label={t("personnel:form.birthDate")} type="date" value={form.birthDate} onChange={(value) => setForm({ ...form, birthDate: value })} />
          <Field label={t("personnel:form.serviceStart")} type="date" value={form.serviceStart} onChange={(value) => setForm({ ...form, serviceStart: value })} />
          <SearchableSelect
            label={t("personnel:form.subdivision")}
            value={form.subdivisionId}
            options={subdivisionOptions}
            placeholder={t("personnel:form.selectSubdivision")}
            onChange={(value) => setForm({ ...form, subdivisionId: value ?? form.subdivisionId })}
          />
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
          {rankSchemaQuery.data?.length ? (
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
          ) : null}
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
        </div>

        <div className="flex flex-col-reverse gap-2 border-t border-zinc-800 px-5 py-4 sm:flex-row sm:justify-end">
          <Button type="button" variant="secondary" onClick={onClose}>
            {t("actions.cancel")}
          </Button>
          <Button type="submit" disabled={saving}>
            <Save className="h-4 w-4 shrink-0" />
            {t("actions.save")}
          </Button>
        </div>
      </form>
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
