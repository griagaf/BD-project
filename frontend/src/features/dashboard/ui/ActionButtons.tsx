import { ExternalLink, Radar } from "lucide-react"
import { useNavigate } from "react-router-dom"
import type { AlertAction } from "@/features/dashboard/model/dashboardTypes"
import { Button } from "@/shared/ui/button"

type ActionButtonsProps = {
  actions: AlertAction[]
}

export function ActionButtons({ actions }: ActionButtonsProps) {
  const navigate = useNavigate()

  return (
    <div className="flex flex-wrap gap-2">
      {actions.map((action) => (
        <Button key={`${action.label}:${action.route}`} type="button" variant="secondary" className="h-8 min-w-0 px-3 text-xs" onClick={() => navigate(action.route)}>
          {action.queryTemplate ? <Radar className="h-4 w-4 shrink-0" /> : <ExternalLink className="h-4 w-4 shrink-0" />}
          <span className="truncate" title={action.label}>{action.label}</span>
        </Button>
      ))}
    </div>
  )
}
