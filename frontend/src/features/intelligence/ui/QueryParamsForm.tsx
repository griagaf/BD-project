import type { QueryParameterMetadata, QueryScope } from "@/features/intelligence/model/intelligenceTypes"
import { Card } from "@/shared/ui/card"

type QueryParamsFormProps = {
  parameters: QueryParameterMetadata[]
  values: Record<string, string | number>
  scope: QueryScope
  onValuesChange: (values: Record<string, string | number>) => void
  onScopeChange: (scope: QueryScope) => void
}

export function QueryParamsForm({ parameters, values, scope, onValuesChange, onScopeChange }: QueryParamsFormProps) {
  return (
    <Card className="space-y-4">
      <div>
        <div className="text-xs uppercase text-emerald-300">Scope</div>
        <div className="mt-3 grid gap-3 md:grid-cols-[180px_140px_1fr]">
          <select
            value={scope.type}
            onChange={(event) => onScopeChange({ ...scope, type: event.target.value })}
            className="h-10 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          >
            <option value="FORMATION">Formation</option>
            <option value="MILITARY_UNIT">Unit</option>
            <option value="PERSONNEL">Personnel</option>
            <option value="GLOBAL">Global</option>
          </select>
          <input
            type="number"
            value={scope.id}
            onChange={(event) => onScopeChange({ ...scope, id: Number(event.target.value) })}
            className="h-10 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          />
          <input
            value={scope.name ?? ""}
            onChange={(event) => onScopeChange({ ...scope, name: event.target.value })}
            placeholder="Display name"
            className="h-10 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          />
        </div>
      </div>

      <div>
        <div className="text-xs uppercase text-emerald-300">Parameters</div>
        <div className="mt-3 grid gap-3 md:grid-cols-2">
          {parameters.map((parameter) => (
            <label key={parameter.name} className="block">
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
          ))}
          {!parameters.length ? <div className="text-sm text-zinc-500">No additional parameters required</div> : null}
        </div>
      </div>
    </Card>
  )
}
