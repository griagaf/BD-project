import { Download, Play, Radar } from "lucide-react"
import { useMemo, useState } from "react"
import { useExecuteQueryMutation, useExportQueryMutation, useQueryTemplatesQuery } from "@/features/intelligence/api/intelligenceQueries"
import type { ExecuteQueryRequest, QueryScope } from "@/features/intelligence/model/intelligenceTypes"
import { QueryParamsForm } from "@/features/intelligence/ui/QueryParamsForm"
import { QueryPreviewTerminal } from "@/features/intelligence/ui/QueryPreviewTerminal"
import { QueryResultTable } from "@/features/intelligence/ui/QueryResultTable"
import { QueryTemplateSelector } from "@/features/intelligence/ui/QueryTemplateSelector"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"

export function IntelligenceTerminalPage() {
  const { data: templates = [], isLoading } = useQueryTemplatesQuery()
  const [selectedCode, setSelectedCode] = useState("FIND_UNITS_IN_FORMATION")
  const [scope, setScope] = useState<QueryScope>({ type: "FORMATION", id: 1, name: "Siberian Tactical District" })
  const [parameters, setParameters] = useState<Record<string, string | number>>({ formationId: 1 })
  const executeMutation = useExecuteQueryMutation()
  const exportMutation = useExportQueryMutation()

  const selectedTemplate = templates.find((template) => template.code === selectedCode) ?? templates[0]
  const command = useMemo(() => buildCommand(selectedCode, scope, parameters, selectedTemplate?.exampleCommand), [parameters, scope, selectedCode, selectedTemplate?.exampleCommand])
  const request: ExecuteQueryRequest = { scope, parameters, previewCommand: command }

  function selectTemplate(code: string) {
    setSelectedCode(code)
    const next = templates.find((template) => template.code === code)
    const defaults = Object.fromEntries(
      next?.parameters
        .filter((parameter) => parameter.type === "enum" && parameter.placeholder)
        .map((parameter) => [parameter.name, parameter.placeholder]) ?? [],
    )
    setParameters(defaults)
  }

  function exportCsv() {
    exportMutation.mutate({ code: selectedCode, request }, {
      onSuccess: (blob) => {
        const url = URL.createObjectURL(blob)
        const anchor = document.createElement("a")
        anchor.href = url
        anchor.download = `${selectedCode.toLowerCase()}.csv`
        anchor.click()
        URL.revokeObjectURL(url)
      },
    })
  }

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
            <Radar className="size-4" />
            Intelligence Query Terminal
          </div>
          <h1 className="mt-1 text-2xl font-semibold text-zinc-100">Template-driven Query Builder</h1>
          <p className="mt-1 text-sm text-zinc-500">Structured analytical SQL execution with role and scope checks.</p>
        </div>
        <div className="flex gap-2">
          <Button type="button" variant="secondary" disabled={!selectedTemplate || exportMutation.isPending} onClick={exportCsv}>
            <Download className="size-4" />
            CSV
          </Button>
          <Button type="button" disabled={!selectedTemplate || executeMutation.isPending} onClick={() => executeMutation.mutate({ code: selectedCode, request })}>
            <Play className="size-4" />
            Execute
          </Button>
        </div>
      </div>

      {isLoading ? (
        <Card className="text-sm text-zinc-500">Loading query templates</Card>
      ) : (
        <div className="grid gap-5 xl:grid-cols-[360px_minmax(0,1fr)]">
          <QueryTemplateSelector templates={templates} selectedCode={selectedCode} onSelect={selectTemplate} />
          <div className="space-y-5">
            <QueryParamsForm
              parameters={selectedTemplate?.parameters ?? []}
              values={parameters}
              scope={scope}
              onValuesChange={setParameters}
              onScopeChange={setScope}
            />
            <QueryPreviewTerminal command={command} />
            {executeMutation.error ? (
              <Card className="border-red-950 bg-red-950/20 text-sm text-red-200">Query execution failed</Card>
            ) : null}
            <QueryResultTable result={executeMutation.data} />
          </div>
        </div>
      )}
    </div>
  )
}

function buildCommand(code: string, scope: QueryScope, parameters: Record<string, string | number>, fallback?: string) {
  const target = code.replaceAll("_", " ")
  const scopeText = scope.type === "GLOBAL" ? "GLOBAL" : `${scope.type} ${scope.name || scope.id}`
  const params = Object.entries(parameters)
    .filter(([, value]) => value !== undefined && value !== null && `${value}`.trim() !== "")
    .map(([key, value]) => `${key}=${value}`)
    .join(" ")
  return `QUERY: ${target}${params ? ` ${params}` : ""} IN ${scopeText}` || fallback || `QUERY: ${target}`
}
