import type { ReactNode } from "react"
import type { LucideIcon } from "lucide-react"

type PageHeaderProps = {
  icon?: LucideIcon
  eyebrow: string
  title: string
  description?: string
  actions?: ReactNode
}

export function PageHeader({ icon: Icon, eyebrow, title, description, actions }: PageHeaderProps) {
  return (
    <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
      <div className="min-w-0">
        <div className="flex min-w-0 items-center gap-2 text-xs uppercase text-emerald-300">
          {Icon ? <Icon className="h-4 w-4 shrink-0" /> : null}
          <span className="truncate" title={eyebrow}>{eyebrow}</span>
        </div>
        <h1 className="mt-1 break-words text-2xl font-semibold text-zinc-100">{title}</h1>
        {description ? <p className="mt-1 max-w-3xl break-words text-sm text-zinc-500">{description}</p> : null}
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  )
}
