import type { QueryParameterMetadata, QueryScope } from "@/features/intelligence/model/intelligenceTypes"
import { Card } from "@/shared/ui/card"
import { lookupApi } from "@/shared/api/lookupApi"
import { SearchableSelect } from "@/shared/ui/searchable-select"
import { useQuery } from "@tanstack/react-query"
import { useTranslation } from "react-i18next"

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
      if (lookupKind === "personnel") return lookupApi.personnel()
      if (lookupKind === "formations") return lookupApi.formations("", "DISTRICT,ARMY,CORPS,DIVISION,BRIGADE")
      if (lookupKind === "units") return lookupApi.units()
      if (lookupKind === "equipmentTypes") return lookupApi.equipmentTypes()
      if (lookupKind === "weaponTypes") return lookupApi.weaponTypes()
      if (lookupKind === "specialties") return lookupApi.specialties()
      return lookupApi.ranks()
    },
    enabled: Boolean(lookupKind),
    staleTime: 5 * 60_000,
  })

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
          if (lookupKind === "personnel") return lookupApi.personnel(search)
          if (lookupKind === "formations") return lookupApi.formations(search, "DISTRICT,ARMY,CORPS,DIVISION,BRIGADE")
          if (lookupKind === "units") return lookupApi.units(search)
          if (lookupKind === "equipmentTypes") return lookupApi.equipmentTypes(search)
          if (lookupKind === "weaponTypes") return lookupApi.weaponTypes(search)
          if (lookupKind === "specialties") return lookupApi.specialties(search)
          return lookupApi.ranks(search)
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
          onChange={(event) => onValuesChange({ ...values, [parameter.name]: parameter.type === "number" ? Number(event.target.value) : event.target.value })}
          className="mt-2 h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
        />
      )}
    </label>
  )
}

export function QueryParamsForm({ parameters, values, scope, onValuesChange, onScopeChange }: QueryParamsFormProps) {
  const { t } = useTranslation("intelligence")
  const { data: formationOptions = [] } = useQuery({
    queryKey: ["lookups", "formations", "intelligence"],
    queryFn: () => lookupApi.formations("", "DISTRICT,ARMY,CORPS,DIVISION,BRIGADE"),
    staleTime: 5 * 60_000,
  })
  const { data: unitOptions = [] } = useQuery({
    queryKey: ["lookups", "units", "intelligence"],
    queryFn: () => lookupApi.units(),
    staleTime: 5 * 60_000,
  })
  const { data: personnelOptions = [] } = useQuery({
    queryKey: ["lookups", "personnel", "intelligence"],
    queryFn: () => lookupApi.personnel(),
    staleTime: 5 * 60_000,
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
                  if (scope.type === "MILITARY_UNIT") return lookupApi.units(search)
                  if (scope.type === "PERSONNEL") return lookupApi.personnel(search)
                  return lookupApi.formations(search, "DISTRICT,ARMY,CORPS,DIVISION,BRIGADE")
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
