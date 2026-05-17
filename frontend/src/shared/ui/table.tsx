import type { ReactNode, TableHTMLAttributes } from "react"
import { cn } from "@/shared/lib/cn"

export function TableShell({ children, className }: { children: ReactNode; className?: string }) {
  return (
    <div className={cn("overflow-hidden rounded-md border border-zinc-800 bg-zinc-950/80", className)}>
      <div className="overflow-x-auto">{children}</div>
    </div>
  )
}

export function Table({ className, ...props }: TableHTMLAttributes<HTMLTableElement>) {
  return <table className={cn("w-full border-collapse text-left text-sm", className)} {...props} />
}

export const tableHeadClass = "border-b border-zinc-800 bg-zinc-900/70 text-xs uppercase text-zinc-500"
export const tableRowClass = "border-b border-zinc-900/90 transition-colors hover:bg-zinc-900/45"
export const tableCellClass = "px-4 py-3 align-middle"
