import type { ReactNode } from "react"
import { AlertTriangle, Inbox, Loader2 } from "lucide-react"
import { useTranslation } from "react-i18next"
import { Card } from "@/shared/ui/card"
import { Button } from "@/shared/ui/button"

type StateProps = {
  title: string
  description?: string
  action?: ReactNode
}

export function LoadingState({ title, description }: Partial<StateProps>) {
  const { t } = useTranslation("common")
  const resolvedTitle = title ?? t("states.loading")
  const resolvedDescription = description ?? t("states.loadingDescription")
  return (
    <Card className="flex items-center gap-3 text-sm text-zinc-400">
      <Loader2 className="h-4 w-4 shrink-0 animate-spin text-emerald-300" />
      <div className="min-w-0">
        <div className="break-words font-medium text-zinc-200">{resolvedTitle}</div>
        <div className="mt-1 break-words text-xs text-zinc-500">{resolvedDescription}</div>
      </div>
    </Card>
  )
}

export function EmptyState({ title, description, action }: StateProps) {
  return (
    <Card className="flex min-h-48 items-center justify-center border-dashed text-center">
      <div className="max-w-md">
        <Inbox className="mx-auto h-9 w-9 shrink-0 text-zinc-600" />
        <div className="mt-4 text-base font-semibold text-zinc-100">{title}</div>
        {description ? <p className="mt-1 text-sm text-zinc-500">{description}</p> : null}
        {action ? <div className="mt-4">{action}</div> : null}
      </div>
    </Card>
  )
}

export function ErrorState({ title, description, onRetry }: Partial<StateProps> & { onRetry?: () => void }) {
  const { t } = useTranslation("common")
  const resolvedTitle = title ?? t("states.error")
  const resolvedDescription = description ?? t("states.errorDescription")
  return (
    <Card className="border-red-950 bg-red-950/20">
      <div className="flex items-start gap-3">
        <AlertTriangle className="mt-0.5 h-5 w-5 shrink-0 text-red-300" />
        <div className="min-w-0">
          <div className="font-medium text-red-100">{resolvedTitle}</div>
          <div className="mt-1 text-sm text-red-200/70">{resolvedDescription}</div>
          {onRetry ? (
            <Button className="mt-4" variant="danger" size="sm" onClick={onRetry}>
              {t("actions.retry")}
            </Button>
          ) : null}
        </div>
      </div>
    </Card>
  )
}
