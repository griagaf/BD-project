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

export function LoadingState({ title = "Loading tactical data", description }: Partial<StateProps>) {
  const { t } = useTranslation("common")
  return (
    <Card className="flex items-center gap-3 text-sm text-zinc-400">
      <Loader2 className="size-4 animate-spin text-emerald-300" />
      <div>
        <div className="font-medium text-zinc-200">{title === "Loading tactical data" ? t("states.loading") : title}</div>
        {description ? <div className="mt-1 text-xs text-zinc-500">{description}</div> : <div className="mt-1 text-xs text-zinc-500">{t("states.loadingDescription")}</div>}
      </div>
    </Card>
  )
}

export function EmptyState({ title, description, action }: StateProps) {
  return (
    <Card className="flex min-h-48 items-center justify-center border-dashed text-center">
      <div className="max-w-md">
        <Inbox className="mx-auto size-9 text-zinc-600" />
        <div className="mt-4 text-base font-semibold text-zinc-100">{title}</div>
        {description ? <p className="mt-1 text-sm text-zinc-500">{description}</p> : null}
        {action ? <div className="mt-4">{action}</div> : null}
      </div>
    </Card>
  )
}

export function ErrorState({ title = "Unable to load data", description = "The backend rejected the request or the service is unavailable.", onRetry }: Partial<StateProps> & { onRetry?: () => void }) {
  const { t } = useTranslation("common")
  const resolvedTitle = title === "Unable to load data" ? t("states.error") : title
  const resolvedDescription = description === "The backend rejected the request or the service is unavailable." ? t("states.errorDescription") : description
  return (
    <Card className="border-red-950 bg-red-950/20">
      <div className="flex items-start gap-3">
        <AlertTriangle className="mt-0.5 size-5 text-red-300" />
        <div>
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
