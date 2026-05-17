import { Download, Play, Radar } from "lucide-react"
import { useMemo, useState } from "react"
import { useTranslation } from "react-i18next"
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
  const { t } = useTranslation(["common", "intelligence"])
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
        toast.success(t("intelligence:toast.exported"), t("intelligence:toast.exportedDescription", { code: selectedCode }))
      },
      onError: () => toast.error(t("common:toasts.csvFailed")),
    })
  }

  return (
    <div className="space-y-5">
      <PageHeader
        icon={Radar}
        eyebrow={t("intelligence:page.eyebrow")}
        title={t("intelligence:page.title")}
        description={t("intelligence:page.description")}
        actions={
          <>
          <Button type="button" variant="secondary" disabled={!selectedTemplate || exportMutation.isPending} onClick={exportCsv}>
            <Download className="size-4" />
            {t("actions.exportCsv")}
          </Button>
          <Button type="button" disabled={!selectedTemplate || executeMutation.isPending} onClick={() => executeMutation.mutate({ code: selectedCode, request }, {
            onSuccess: (result) => toast.success(t("intelligence:toast.executed"), t("intelligence:toast.rowsReturned", { count: result.rowCount })),
            onError: () => toast.error(t("intelligence:toast.failed")),
          })}>
            <Play className="size-4" />
            {t("actions.execute")}
          </Button>
          </>
        }
      />

      {isLoading ? (
        <LoadingState title={t("intelligence:states.loadingTemplates")} />
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
              <ErrorState title={t("intelligence:error")} />
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
