import type { HTMLAttributes } from "react"
import { cn } from "@/shared/lib/cn"

export function Card({ className, ...props }: HTMLAttributes<HTMLDivElement>) {
  return <div className={cn("rounded-md border border-zinc-800 bg-zinc-950 p-5", className)} {...props} />
}

