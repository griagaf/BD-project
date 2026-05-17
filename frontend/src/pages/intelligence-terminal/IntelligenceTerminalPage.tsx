import { Download, Play, Radar } from "lucide-react"
import { useMemo, useState } from "react"
import { useExecuteQueryMutation, useExportQueryMutation, useQueryTemplatesQuery } from "@/features/intelligence/api/intelligenceQueries"
import type { ExecuteQueryRequest, QueryScope } from "@/features/intelligence/model/intelligenceTypes"
import { QueryParamsForm } from "@/features/intelligence/ui/QueryParamsForm"
import { QueryPreviewTerminal } from "@/features/intelligence/ui/QueryPreviewTerminal"
import { QueryResultTable } from "@/features/intelligence/ui/QueryResultTable"
import { QueryTemplateSelector } from "@/features/intelligence/ui/QueryTemplateSelector"
import { Button } from "@/shared/ui/button"
import { PageHeader } from "@/shared/ui/page"
import { ErrorState, LoadingState } from "@/shared/ui/state"
import { toast } from "@/shared/ui/toast"

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
        toast.success("CSV exported", `${selectedCode} result exported`)
      },
      onError: () => toast.error("CSV export failed"),
    })
  }

  return (
    <div className="space-y-5">
      <PageHeader
        icon={Radar}
        eyebrow="Intelligence Query Terminal"
        title="Template-driven Query Builder"
        description="Structured analytical SQL execution with role and scope checks."
        actions={
          <>
          <Button type="button" variant="secondary" disabled={!selectedTemplate || exportMutation.isPending} onClick={exportCsv}>
            <Download className="size-4" />
            CSV
          </Button>
          <Button type="button" disabled={!selectedTemplate || executeMutation.isPending} onClick={() => executeMutation.mutate({ code: selectedCode, request }, {
            onSuccess: (result) => toast.success("Query executed", `${result.rowCount} rows returned`),
            onError: () => toast.error("Query execution failed"),
          })}>
            <Play className="size-4" />
            Execute
          </Button>
          </>
        }
      />

      {isLoading ? (
        <LoadingState title="Loading query templates" />
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
              <ErrorState title="Query execution failed" />
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
