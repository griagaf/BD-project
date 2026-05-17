import { Save, X } from "lucide-react"
import { useEffect, useMemo, useState } from "react"
import { useTranslation } from "react-i18next"
import type { Personnel, PersonnelRequest, Rank, Specialty } from "@/features/personnel/model/personnelTypes"
import { Button } from "@/shared/ui/button"

type PersonnelEditModalProps = {
  open: boolean
  personnel: Personnel | null
  ranks: Rank[]
  specialties: Specialty[]
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
}

export function PersonnelEditModal({
  open,
  personnel,
  ranks,
  specialties,
  saving,
  onClose,
  onSubmit,
}: PersonnelEditModalProps) {
  const { t } = useTranslation(["common", "personnel"])
  const [form, setForm] = useState<PersonnelRequest>(emptyForm)

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
    })
  }, [open, personnel])

  const title = useMemo(() => (personnel ? t("personnel:form.edit") : t("personnel:form.create")), [personnel, t])

  if (!open) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/70 p-4">
      <form
        className="w-full max-w-3xl rounded-md border border-zinc-800 bg-zinc-950 shadow-2xl"
        onSubmit={(event) => {
          event.preventDefault()
          onSubmit(form)
        }}
      >
        <div className="flex items-center justify-between border-b border-zinc-800 px-5 py-4">
          <div>
            <div className="text-sm font-semibold uppercase text-zinc-100">{title}</div>
            <div className="text-xs text-zinc-500">{t("personnel:form.record")}</div>
          </div>
          <Button type="button" variant="ghost" className="size-9 px-0" onClick={onClose}>
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
          <Field
            label={t("personnel:form.subdivisionId")}
            type="number"
            value={`${form.subdivisionId}`}
            onChange={(value) => setForm({ ...form, subdivisionId: Number(value) })}
          />
          <label className="space-y-2">
            <span className="text-xs uppercase text-zinc-500">{t("personnel:form.rank")}</span>
            <select
              className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm outline-none"
              value={form.rankId ?? ""}
              onChange={(event) => setForm({ ...form, rankId: event.target.value ? Number(event.target.value) : null })}
            >
              <option value="">{t("personnel:table.noRank")}</option>
              {ranks.map((rank) => (
                <option key={rank.id} value={rank.id}>
                  {rank.name}
                </option>
              ))}
            </select>
          </label>
          <label className="space-y-2 md:col-span-2">
            <span className="text-xs uppercase text-zinc-500">{t("personnel:form.specialties")}</span>
            <div className="grid gap-2 rounded-md border border-zinc-800 bg-zinc-900 p-3 sm:grid-cols-2">
              {specialties.map((specialty) => (
                <label key={specialty.id} className="flex items-center gap-2 text-sm text-zinc-300">
                  <input
                    type="checkbox"
                    checked={form.specialtyIds.includes(specialty.id)}
                    onChange={(event) => {
                      const specialtyIds = event.target.checked
                        ? [...form.specialtyIds, specialty.id]
                        : form.specialtyIds.filter((id) => id !== specialty.id)
                      setForm({ ...form, specialtyIds })
                    }}
                  />
                  {specialty.name}
                </label>
              ))}
            </div>
          </label>
        </div>

        <div className="flex justify-end gap-2 border-t border-zinc-800 px-5 py-4">
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
