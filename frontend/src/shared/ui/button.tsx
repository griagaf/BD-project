import type { ButtonHTMLAttributes } from "react"
import { cn } from "@/shared/lib/cn"

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost"
}

export function Button({ className, variant = "primary", ...props }: ButtonProps) {
  return (
    <button
      className={cn(
        "inline-flex h-10 items-center justify-center gap-2 rounded-md px-4 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50",
        variant === "primary" && "bg-emerald-500 text-zinc-950 hover:bg-emerald-400",
        variant === "secondary" && "bg-zinc-800 text-zinc-100 hover:bg-zinc-700",
        variant === "ghost" && "bg-transparent text-zinc-300 hover:bg-zinc-900 hover:text-zinc-100",
        className,
      )}
      {...props}
    />
  )
}

