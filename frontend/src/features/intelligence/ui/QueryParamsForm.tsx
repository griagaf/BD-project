import type { QueryParameterMetadata, QueryScope } from "@/features/intelligence/model/intelligenceTypes"
import { Card } from "@/shared/ui/card"
import { lookupApi } from "@/shared/api/lookupApi"
import { SearchableSelect } from "@/shared/ui/searchable-select"
import { useQuery } from "@tanstack/react-query"
import { useTranslation } from "react-i18next"
import { useMemo, useState } from "react"

type QueryParamsFormProps = {
  parameters: QueryParameterMetadata[]
  values: Record<string, string | number>
  scope: QueryScope
  onValuesChange: (values: Record<string, string | number>) => void
  onScopeChange: (scope: QueryScope) => void
}

function QueryParameterField({
  parameter,
  values,
  onValuesChange,
}: {
  parameter: QueryParameterMetadata
  values: Record<string, string | number>
  onValuesChange: (values: Record<string, string | number>) => void
}) {
  const lookupKind = parameter.name === "personnelId"
    ? "personnel"
    : parameter.name === "formationId"
      ? "formations"
      : parameter.name === "unitId"
        ? "units"
        : parameter.name === "equipmentType"
          ? "equipmentTypes"
          : parameter.name === "weaponType"
            ? "weaponTypes"
            : parameter.name === "specialtyName"
              ? "specialties"
              : parameter.name === "rankName"
                ? "ranks"
                : null

  const { data: options = [] } = useQuery({
    queryKey: ["lookups", lookupKind, "query-param"],
    queryFn: () => {
      if (lookupKind === "personnel") return lookupApi.personnel("", { limit: 500 })
      if (lookupKind === "formations") return lookupApi.formations("", "DISTRICT,ARMY,CORPS,DIVISION,BRIGADE", { limit: 500 })
      if (lookupKind === "units") return lookupApi.units("", { limit: 500 })
      if (lookupKind === "equipmentTypes") return lookupApi.equipmentTypes("", 500)
      if (lookupKind === "weaponTypes") return lookupApi.weaponTypes("", 500)
      if (lookupKind === "specialties") return lookupApi.specialties("", 500)
      return lookupApi.ranks("", 500)
    },
    enabled: Boolean(lookupKind),
    staleTime: 5 * 60_000,
  })

  if (parameter.name === "personnelId") {
    return (
      <HierarchyPersonnelPicker
        label={parameter.label}
        value={typeof values[parameter.name] === "number" ? Number(values[parameter.name]) : null}
        placeholder={parameter.placeholder}
        onChange={(id) => onValuesChange({ ...values, [parameter.name]: id ?? "" })}
      />
    )
  }

  if (lookupKind) {
    const isIdValue = parameter.name.endsWith("Id")
    const currentLabel = isIdValue
      ? null
      : options.find((option) => option.label === values[parameter.name])?.id ?? null
    const currentValue = isIdValue
      ? typeof values[parameter.name] === "number" ? Number(values[parameter.name]) : null
      : currentLabel

    return (
      <SearchableSelect
        label={parameter.label}
        value={currentValue}
        options={options}
        placeholder={parameter.placeholder}
        loadOptions={(search) => {
          if (lookupKind === "personnel") return lookupApi.personnel(search, { limit: 500 })
          if (lookupKind === "formations") return lookupApi.formations(search, "DISTRICT,ARMY,CORPS,DIVISION,BRIGADE", { limit: 500 })
          if (lookupKind === "units") return lookupApi.units(search, { limit: 500 })
          if (lookupKind === "equipmentTypes") return lookupApi.equipmentTypes(search, 500)
          if (lookupKind === "weaponTypes") return lookupApi.weaponTypes(search, 500)
          if (lookupKind === "specialties") return lookupApi.specialties(search, 500)
          return lookupApi.ranks(search, 500)
        }}
        onChange={(id) => {
          const selected = options.find((option) => option.id === id)
          onValuesChange({
            ...values,
            [parameter.name]: isIdValue ? (id ?? 0) : (selected?.label ?? ""),
          })
        }}
      />
    )
  }

  return (
    <label className="block">
      <span className="text-xs uppercase text-zinc-500">{parameter.label}</span>
      {parameter.type === "enum" ? (
        <select
          value={values[parameter.name] ?? parameter.placeholder ?? ""}
          onChange={(event) => onValuesChange({ ...values, [parameter.name]: event.target.value })}
          className="mt-2 h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
        >
          {parameter.options.map((option) => (
            <option key={option} value={option}>{option}</option>
          ))}
        </select>
      ) : (
        <input
          type={parameter.type === "number" ? "number" : "text"}
          value={values[parameter.name] ?? ""}
          placeholder={parameter.placeholder}
          onChange={(event) => {
            const value = event.target.value
            onValuesChange({ ...values, [parameter.name]: parameter.type === "number" ? (value === "" ? "" : Number(value)) : value })
          }}
          className="mt-2 h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
        />
      )}
    </label>
  )
}

function HierarchyPersonnelPicker({
  label,
  value,
  placeholder,
  onChange,
}: {
  label: string
  value: number | null
  placeholder: string
  onChange: (value: number | null) => void
}) {
  const { t } = useTranslation("intelligence")
  const [armyId, setArmyId] = useState<number | null>(null)
  const [formationId, setFormationId] = useState<number | null>(null)
  const [unitId, setUnitId] = useState<number | null>(null)
  const [companyId, setCompanyId] = useState<number | null>(null)
  const [platoonId, setPlatoonId] = useState<number | null>(null)
  const [squadId, setSquadId] = useState<number | null>(null)
  const armiesQuery = useQuery({
    queryKey: ["lookups", "intelligence", "armies"],
    queryFn: () => lookupApi.formations("", "ARMY", { limit: 500 }),
  })
  const formationsQuery = useQuery({
    queryKey: ["lookups", "intelligence", "formations", armyId],
    queryFn: () => lookupApi.formations("", "CORPS,DIVISION,BRIGADE", { parentId: armyId, limit: 500 }),
    enabled: Boolean(armyId),
  })
  const unitsQuery = useQuery({
    queryKey: ["lookups", "intelligence", "units", formationId],
    queryFn: () => lookupApi.units("", { formationId, limit: 500 }),
    enabled: Boolean(formationId),
  })
  const companiesQuery = useQuery({
    queryKey: ["lookups", "intelligence", "companies", unitId],
    queryFn: () => lookupApi.subdivisions("", { unitId, type: "COMPANY", limit: 500 }),
    enabled: Boolean(unitId),
  })
  const platoonsQuery = useQuery({
    queryKey: ["lookups", "intelligence", "platoons", companyId],
    queryFn: () => lookupApi.subdivisions("", { parentId: companyId, type: "PLATOON", limit: 500 }),
    enabled: Boolean(companyId),
  })
  const squadsQuery = useQuery({
    queryKey: ["lookups", "intelligence", "squads", platoonId],
    queryFn: () => lookupApi.subdivisions("", { parentId: platoonId, type: "SQUAD", limit: 500 }),
    enabled: Boolean(platoonId),
  })
  const personnelQuery = useQuery({
    queryKey: ["lookups", "intelligence", "personnel", unitId, companyId, platoonId, squadId],
    queryFn: () => lookupApi.personnel("", {
      unitId,
      subdivisionId: squadId ?? platoonId ?? companyId,
      limit: 500,
    }),
    enabled: Boolean(unitId || companyId || platoonId || squadId),
  })
  const selectedPath = useMemo(() => [
    armiesQuery.data?.find((item) => item.id === armyId)?.label,
    formationsQuery.data?.find((item) => item.id === formationId)?.label,
    unitsQuery.data?.find((item) => item.id === unitId)?.label,
    companiesQuery.data?.find((item) => item.id === companyId)?.label,
    platoonsQuery.data?.find((item) => item.id === platoonId)?.label,
    squadsQuery.data?.find((item) => item.id === squadId)?.label,
    personnelQuery.data?.find((item) => item.id === value)?.label,
  ].filter(Boolean).join(" → "), [armiesQuery.data, armyId, companiesQuery.data, companyId, formationId, formationsQuery.data, personnelQuery.data, platoonId, platoonsQuery.data, squadId, squadsQuery.data, unitId, unitsQuery.data, value])

  return (
    <div className="space-y-3 rounded-md border border-zinc-800 bg-zinc-950/60 p-3 md:col-span-2">
      <div className="text-xs uppercase text-zinc-500">{label}</div>
      <div className="grid gap-3 md:grid-cols-2">
        <SearchableSelect label={t("builder.army")} value={armyId} options={armiesQuery.data ?? []} placeholder={t("builder.selectArmy")} onChange={(id) => {
          setArmyId(id); setFormationId(null); setUnitId(null); setCompanyId(null); setPlatoonId(null); setSquadId(null); onChange(null)
        }} />
        <SearchableSelect label={t("builder.formation")} value={formationId} options={formationsQuery.data ?? []} placeholder={t("builder.selectFormation")} disabled={!armyId} onChange={(id) => {
          setFormationId(id); setUnitId(null); setCompanyId(null); setPlatoonId(null); setSquadId(null); onChange(null)
        }} />
        <SearchableSelect label={t("builder.unit")} value={unitId} options={unitsQuery.data ?? []} placeholder={t("builder.selectUnit")} disabled={!formationId} onChange={(id) => {
          setUnitId(id); setCompanyId(null); setPlatoonId(null); setSquadId(null); onChange(null)
        }} />
        <SearchableSelect label={t("builder.company")} value={companyId} options={companiesQuery.data ?? []} placeholder={t("builder.selectCompany")} disabled={!unitId} onChange={(id) => {
          setCompanyId(id); setPlatoonId(null); setSquadId(null); onChange(null)
        }} />
        <SearchableSelect label={t("builder.platoon")} value={platoonId} options={platoonsQuery.data ?? []} placeholder={t("builder.selectPlatoon")} disabled={!companyId} onChange={(id) => {
          setPlatoonId(id); setSquadId(null); onChange(null)
        }} />
        <SearchableSelect label={t("builder.squad")} value={squadId} options={squadsQuery.data ?? []} placeholder={t("builder.selectSquad")} disabled={!platoonId} onChange={(id) => {
          setSquadId(id); onChange(null)
        }} />
      </div>
      <SearchableSelect
        label={t("builder.personnel")}
        value={value}
        options={personnelQuery.data ?? []}
        placeholder={placeholder}
        searchPlaceholder={t("builder.searchPersonnel")}
        loadOptions={(search) => lookupApi.personnel(search, { unitId, subdivisionId: squadId ?? platoonId ?? companyId, limit: 500 })}
        onChange={onChange}
      />
      <div className="rounded border border-zinc-800 bg-zinc-900/60 px-3 py-2 text-xs text-zinc-500">
        <span className="text-zinc-600">{t("builder.selectedPath")}: </span>
        <span className="break-words text-zinc-300">{selectedPath || t("builder.selectPersonnelPath")}</span>
      </div>
    </div>
  )
}

export function QueryParamsForm({ parameters, values, scope, onValuesChange, onScopeChange }: QueryParamsFormProps) {
  const { t } = useTranslation("intelligence")
  const { data: formationOptions = [] } = useQuery({
    queryKey: ["lookups", "formations", "intelligence"],
    queryFn: () => lookupApi.formations("", "DISTRICT,ARMY,CORPS,DIVISION,BRIGADE", { limit: 500 }),
    staleTime: 0,
  })
  const { data: unitOptions = [] } = useQuery({
    queryKey: ["lookups", "units", "intelligence"],
    queryFn: () => lookupApi.units("", { limit: 500 }),
    staleTime: 0,
  })
  const { data: personnelOptions = [] } = useQuery({
    queryKey: ["lookups", "personnel", "intelligence"],
    queryFn: () => lookupApi.personnel("", { limit: 500 }),
    staleTime: 0,
  })

  const scopeOptions = scope.type === "MILITARY_UNIT"
    ? unitOptions
    : scope.type === "PERSONNEL"
      ? personnelOptions
      : scope.type === "GLOBAL"
        ? []
        : formationOptions

  return (
    <Card className="space-y-4">
      <div>
        <div className="text-xs uppercase text-emerald-300">{t("builder.scope")}</div>
        <div className="mt-3 grid gap-3 md:grid-cols-[180px_140px_1fr]">
          <select
            value={scope.type}
            onChange={(event) => onScopeChange({ type: event.target.value, id: 0, name: "" })}
            className="h-10 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          >
            <option value="FORMATION">{t("builder.formation")}</option>
            <option value="MILITARY_UNIT">{t("builder.unit")}</option>
            <option value="PERSONNEL">{t("builder.personnel")}</option>
            <option value="GLOBAL">{t("builder.global")}</option>
          </select>
          <div className="md:col-span-2">
            {scope.type === "GLOBAL" ? (
              <div className="flex h-10 items-center rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-500">
                {t("builder.global")}
              </div>
            ) : (
              <SearchableSelect
                label={t("builder.scope")}
                value={scope.id || null}
                options={scopeOptions}
                placeholder={t("builder.selectScope")}
                loadOptions={(search) => {
                  if (scope.type === "MILITARY_UNIT") return lookupApi.units(search, { limit: 500 })
                  if (scope.type === "PERSONNEL") return lookupApi.personnel(search, { limit: 500 })
                  return lookupApi.formations(search, "DISTRICT,ARMY,CORPS,DIVISION,BRIGADE", { limit: 500 })
                }}
                onChange={(id) => {
                  const selected = scopeOptions.find((option) => option.id === id)
                  onScopeChange({ ...scope, id: id ?? 0, name: selected?.label ?? "" })
                }}
              />
            )}
          </div>
          <input type="hidden" value={scope.name ?? ""} readOnly />
          {false ? (
          <input
            value={scope.name ?? ""}
            onChange={(event) => onScopeChange({ ...scope, name: event.target.value })}
            placeholder={t("builder.displayName")}
            className="h-10 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          />
          ) : null}
        </div>
      </div>

      <div>
        <div className="text-xs uppercase text-emerald-300">{t("builder.parameters")}</div>
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          {parameters.map((parameter) => (
            <QueryParameterField key={parameter.name} parameter={parameter} values={values} onValuesChange={onValuesChange} />
          ))}
          {!parameters.length ? <div className="text-sm text-zinc-500">{t("builder.noParameters")}</div> : null}
        </div>
      </div>
    </Card>
  )
}
