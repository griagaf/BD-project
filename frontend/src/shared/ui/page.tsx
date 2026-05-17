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
      <div>
        <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
          {Icon ? <Icon className="size-4" /> : null}
          {eyebrow}
        </div>
        <h1 className="mt-1 text-2xl font-semibold text-zinc-100">{title}</h1>
        {description ? <p className="mt-1 max-w-3xl text-sm text-zinc-500">{description}</p> : null}
      </div>
      {actions ? <div className="flex flex-wrap items-center gap-2">{actions}</div> : null}
    </div>
  )
}
