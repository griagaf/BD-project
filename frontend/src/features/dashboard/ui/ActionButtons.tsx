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
        <Button key={`${action.label}:${action.route}`} type="button" variant="secondary" className="h-8 px-3 text-xs" onClick={() => navigate(action.route)}>
          {action.queryTemplate ? <Radar className="size-3" /> : <ExternalLink className="size-3" />}
          {action.label}
        </Button>
      ))}
    </div>
  )
}
