import { useMemo, useState } from "react"
import { FileText, Radar } from "lucide-react"
import { useTranslation } from "react-i18next"
import { useExportSmartMissionReportCsvMutation, useGenerateSmartMissionReportMutation } from "@/features/report/api/reportQueries"
import type { ReportObjectType, SmartMissionReportRequest } from "@/features/report/model/reportTypes"
import { ExportActions } from "@/features/report/ui/ExportActions"
import { ReportViewer } from "@/features/report/ui/ReportViewer"
import { Card } from "@/shared/ui/card"
import { PageHeader } from "@/shared/ui/page"
import { ErrorState, EmptyState } from "@/shared/ui/state"

const objectTypes: ReportObjectType[] = [
  "ARMY",
  "FORMATION",
  "BRIGADE",
  "MILITARY_UNIT",
  "COMPANY",
  "PLATOON",
]

export function TacticalReportPage() {
  const { t } = useTranslation("reports")
  const [objectType, setObjectType] = useState<ReportObjectType>("MILITARY_UNIT")
  const [objectId, setObjectId] = useState("11101")
  const [includePersonnel, setIncludePersonnel] = useState(true)
  const [includeResources, setIncludeResources] = useState(true)
  const [includeAlerts, setIncludeAlerts] = useState(true)
  const [includeRecommendations, setIncludeRecommendations] = useState(true)
  const generateReport = useGenerateSmartMissionReportMutation()
  const exportReport = useExportSmartMissionReportCsvMutation()

  const request = useMemo<SmartMissionReportRequest>(() => ({
    objectType,
    objectId: Number(objectId || 0),
    includePersonnel,
    includeResources,
    includeAlerts,
    includeRecommendations,
  }), [objectId, objectType, includePersonnel, includeResources, includeAlerts, includeRecommendations])

  const canSubmit = Number.isInteger(request.objectId) && request.objectId > 0

  function handleGenerate() {
    if (canSubmit) {
      generateReport.mutate(request)
    }
  }

  async function handleExportCsv() {
    if (!canSubmit) {
      return
    }
    const blob = await exportReport.mutateAsync(request)
    const url = URL.createObjectURL(blob)
    const link = document.createElement("a")
    link.href = url
    link.download = `smart-mission-report-${request.objectType.toLowerCase()}-${request.objectId}.csv`
    link.click()
    URL.revokeObjectURL(url)
  }

  return (
    <div className="space-y-5">
      <PageHeader
        icon={Radar}
        eyebrow={t("page.eyebrow")}
        title={t("page.title")}
        description={t("page.description")}
        actions={<ExportActions
          canExport={Boolean(generateReport.data) && canSubmit}
          isGenerating={generateReport.isPending}
          isExporting={exportReport.isPending}
          onGenerate={handleGenerate}
          onExportCsv={handleExportCsv}
        />}
      />

      <Card className="grid gap-4 lg:grid-cols-[220px_180px_minmax(0,1fr)]">
        <label className="space-y-2">
          <span className="text-xs uppercase text-zinc-500">{t("form.objectType")}</span>
          <select
            value={objectType}
            onChange={(event) => setObjectType(event.target.value as ReportObjectType)}
            className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          >
            {objectTypes.map((type) => (
              <option key={type} value={type}>{t(`objectTypes.${type}`)}</option>
            ))}
          </select>
        </label>
        <label className="space-y-2">
          <span className="text-xs uppercase text-zinc-500">{t("form.objectId")}</span>
          <input
            value={objectId}
            onChange={(event) => setObjectId(event.target.value)}
            inputMode="numeric"
            className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          />
        </label>
        <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
          <ReportOption label={t("form.personnel")} checked={includePersonnel} onChange={setIncludePersonnel} />
          <ReportOption label={t("form.resources")} checked={includeResources} onChange={setIncludeResources} />
          <ReportOption label={t("form.alerts")} checked={includeAlerts} onChange={setIncludeAlerts} />
          <ReportOption label={t("form.recommendations")} checked={includeRecommendations} onChange={setIncludeRecommendations} />
        </div>
      </Card>

      {!canSubmit ? <Card className="border-amber-900 bg-amber-950/20 text-sm text-amber-200">{t("states.invalidObject")}</Card> : null}
      {generateReport.error ? <ErrorState title={t("states.generateError")} /> : null}
      {exportReport.error ? <ErrorState title={t("states.exportError")} /> : null}

      {generateReport.data ? (
        <ReportViewer report={generateReport.data} />
      ) : (
        <EmptyState title={t("states.previewReady")} description={t("states.previewDescription")} action={<FileText className="mx-auto h-10 w-10 shrink-0 text-emerald-300" />} />
      )}
    </div>
  )
}

function ReportOption({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return (
    <label className="flex h-10 min-w-0 items-center gap-2 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-300">
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="h-4 w-4 shrink-0 accent-emerald-400"
      />
      <span className="truncate" title={label}>{label}</span>
    </label>
  )
}
