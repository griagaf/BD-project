import { AlertTriangle, RotateCcw } from "lucide-react"
import { useTranslation } from "react-i18next"
import { ApiError } from "@/shared/api/apiError"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"

type QueryErrorPanelProps = {
  error: unknown
  parameters: Record<string, string | number>
  onRetry: () => void
}

export function QueryErrorPanel({ error, parameters, onRetry }: QueryErrorPanelProps) {
  const { t } = useTranslation(["common", "intelligence"])
  const message = error instanceof ApiError ? error.message : t("intelligence:error")
  const filledParameters = Object.entries(parameters).filter(([, value]) => `${value ?? ""}`.trim() !== "")

  return (
    <Card className="border-red-950 bg-red-950/20">
      <div className="flex items-start gap-3">
        <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-red-300" />
        <div className="min-w-0 flex-1">
          <div className="font-medium text-red-100">{t("intelligence:errorPanel.title")}</div>
          <p className="mt-1 break-words text-sm text-red-200/75">{message}</p>
          {filledParameters.length ? (
            <div className="mt-3 flex flex-wrap gap-2">
              {filledParameters.map(([key, value]) => (
                <span key={key} className="max-w-full rounded border border-red-900/60 bg-black/20 px-2 py-1 text-xs text-red-100/80">
                  <span className="text-red-200">{key}</span>: <span className="break-words">{value}</span>
                </span>
              ))}
            </div>
          ) : null}
          <Button type="button" className="mt-4" variant="danger" size="sm" onClick={onRetry}>
            <RotateCcw className="h-4 w-4 shrink-0" />
            {t("actions.retry")}
          </Button>
        </div>
      </div>
    </Card>
  )
}
