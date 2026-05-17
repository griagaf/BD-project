import { Download, FileText } from "lucide-react"
import { PermissionGuard } from "@/features/permissions/ui/PermissionGuard"
import { Button } from "@/shared/ui/button"

type ExportActionsProps = {
  canExport: boolean
  isGenerating: boolean
  isExporting: boolean
  onGenerate: () => void
  onExportCsv: () => void
}

export function ExportActions({ canExport, isGenerating, isExporting, onGenerate, onExportCsv }: ExportActionsProps) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <PermissionGuard permissions={["report:generate"]}>
        <Button onClick={onGenerate} disabled={isGenerating}>
          <FileText className="size-4" />
          {isGenerating ? "Generating" : "Generate Tactical Report"}
        </Button>
        <Button variant="secondary" onClick={onExportCsv} disabled={!canExport || isExporting}>
          <Download className="size-4" />
          {isExporting ? "Exporting" : "Export CSV"}
        </Button>
      </PermissionGuard>
    </div>
  )
}
