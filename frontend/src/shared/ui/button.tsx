import type { ButtonHTMLAttributes } from "react"
import { cn } from "@/shared/lib/cn"

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: "primary" | "secondary" | "ghost" | "danger"
  size?: "sm" | "md" | "icon"
}

export function Button({ className, variant = "primary", size = "md", ...props }: ButtonProps) {
  return (
    <button
      className={cn(
        "inline-flex shrink-0 items-center justify-center gap-2 rounded-md border text-sm font-medium outline-none transition-all",
        "focus-visible:ring-2 focus-visible:ring-emerald-400/70 focus-visible:ring-offset-2 focus-visible:ring-offset-zinc-950",
        "disabled:cursor-not-allowed disabled:opacity-50",
        size === "md" && "h-10 px-4",
        size === "sm" && "h-8 px-3 text-xs",
        size === "icon" && "size-9 px-0",
        variant === "primary" && "border-emerald-400/40 bg-emerald-500 text-zinc-950 shadow-[0_0_22px_rgba(16,185,129,0.18)] hover:bg-emerald-400",
        variant === "secondary" && "border-zinc-700 bg-zinc-800 text-zinc-100 hover:border-zinc-600 hover:bg-zinc-700",
        variant === "ghost" && "border-transparent bg-transparent text-zinc-300 hover:border-zinc-800 hover:bg-zinc-900 hover:text-zinc-100",
        variant === "danger" && "border-red-500/30 bg-red-500/10 text-red-200 hover:bg-red-500/20",
        className,
      )}
      {...props}
    />
  )
}
