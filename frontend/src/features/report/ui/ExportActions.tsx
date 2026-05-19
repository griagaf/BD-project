import { Download, FileText } from "lucide-react"
import { useTranslation } from "react-i18next"
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
  const { t } = useTranslation(["common", "reports"])
  return (
    <div className="flex flex-wrap items-center gap-2">
      <PermissionGuard permissions={["report:generate"]}>
        <Button onClick={onGenerate} disabled={isGenerating}>
          <FileText className="h-4 w-4 shrink-0" />
          {isGenerating ? t("reports:actions.generating") : t("reports:actions.generate")}
        </Button>
        <Button variant="secondary" onClick={onExportCsv} disabled={!canExport || isExporting}>
          <Download className="h-4 w-4 shrink-0" />
          {isExporting ? t("reports:actions.exporting") : t("actions.exportCsv")}
        </Button>
      </PermissionGuard>
    </div>
  )
}
