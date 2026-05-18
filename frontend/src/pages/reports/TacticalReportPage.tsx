import { useMemo, useState } from "react"
import { FileText, Radar } from "lucide-react"
import { useExportSmartMissionReportCsvMutation, useGenerateSmartMissionReportMutation } from "@/features/report/api/reportQueries"
import type { ReportObjectType, SmartMissionReportRequest } from "@/features/report/model/reportTypes"
import { ExportActions } from "@/features/report/ui/ExportActions"
import { ReportViewer } from "@/features/report/ui/ReportViewer"
import { Card } from "@/shared/ui/card"

const objectTypes: Array<{ value: ReportObjectType; label: string }> = [
  { value: "ARMY", label: "Army" },
  { value: "FORMATION", label: "Formation" },
  { value: "BRIGADE", label: "Brigade" },
  { value: "MILITARY_UNIT", label: "Military Unit" },
  { value: "COMPANY", label: "Company" },
  { value: "PLATOON", label: "Platoon" },
]

export function TacticalReportPage() {
  const [objectType, setObjectType] = useState<ReportObjectType>("MILITARY_UNIT")
  const [objectId, setObjectId] = useState("1")
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
      <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
        <div>
          <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
            <Radar className="size-4" />
            Tactical reporting
          </div>
          <h1 className="mt-1 text-2xl font-semibold text-zinc-100">Smart Mission Report</h1>
          <p className="mt-1 text-sm text-zinc-500">Aggregated readiness, resources, alerts and rule-based recommendations.</p>
        </div>
        <ExportActions
          canExport={Boolean(generateReport.data) && canSubmit}
          isGenerating={generateReport.isPending}
          isExporting={exportReport.isPending}
          onGenerate={handleGenerate}
          onExportCsv={handleExportCsv}
        />
      </div>

      <Card className="grid gap-4 lg:grid-cols-[220px_160px_minmax(0,1fr)]">
        <label className="space-y-2">
          <span className="text-xs uppercase text-zinc-500">Object type</span>
          <select
            value={objectType}
            onChange={(event) => setObjectType(event.target.value as ReportObjectType)}
            className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          >
            {objectTypes.map((type) => (
              <option key={type.value} value={type.value}>{type.label}</option>
            ))}
          </select>
        </label>
        <label className="space-y-2">
          <span className="text-xs uppercase text-zinc-500">Object id</span>
          <input
            value={objectId}
            onChange={(event) => setObjectId(event.target.value)}
            inputMode="numeric"
            className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          />
        </label>
        <div className="grid gap-2 sm:grid-cols-2 lg:grid-cols-4">
          <ReportOption label="Personnel" checked={includePersonnel} onChange={setIncludePersonnel} />
          <ReportOption label="Resources" checked={includeResources} onChange={setIncludeResources} />
          <ReportOption label="Alerts" checked={includeAlerts} onChange={setIncludeAlerts} />
          <ReportOption label="Recommendations" checked={includeRecommendations} onChange={setIncludeRecommendations} />
        </div>
      </Card>

      {!canSubmit ? <Card className="border-amber-900 bg-amber-950/20 text-sm text-amber-200">Object id must be a positive number</Card> : null}
      {generateReport.error ? <Card className="border-red-950 bg-red-950/20 text-sm text-red-200">Unable to generate report for selected scope</Card> : null}
      {exportReport.error ? <Card className="border-red-950 bg-red-950/20 text-sm text-red-200">Unable to export CSV report</Card> : null}

      {generateReport.data ? (
        <ReportViewer report={generateReport.data} />
      ) : (
        <Card className="flex min-h-[360px] items-center justify-center border-dashed text-center">
          <div>
            <FileText className="mx-auto size-10 text-emerald-300" />
            <div className="mt-4 text-lg font-semibold text-zinc-100">Report preview is ready for generation</div>
            <div className="mt-1 text-sm text-zinc-500">Select scoped object and generate tactical report.</div>
          </div>
        </Card>
      )}
    </div>
  )
}

function ReportOption({ label, checked, onChange }: { label: string; checked: boolean; onChange: (value: boolean) => void }) {
  return (
    <label className="flex h-10 items-center gap-2 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-300">
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
        className="size-4 accent-emerald-400"
      />
      {label}
    </label>
  )
}
