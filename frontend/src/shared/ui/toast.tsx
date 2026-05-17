import { create } from "zustand"
import { AnimatePresence, motion } from "framer-motion"
import { AlertTriangle, CheckCircle2, Info, X } from "lucide-react"
import { Button } from "@/shared/ui/button"
import { cn } from "@/shared/lib/cn"

type ToastVariant = "success" | "error" | "info"

type Toast = {
  id: string
  title: string
  description?: string
  variant: ToastVariant
}

type ToastStore = {
  toasts: Toast[]
  push: (toast: Omit<Toast, "id">) => void
  dismiss: (id: string) => void
}

export const useToastStore = create<ToastStore>((set) => ({
  toasts: [],
  push: (toast) => {
    const id = crypto.randomUUID()
    set((state) => ({ toasts: [...state.toasts, { ...toast, id }].slice(-4) }))
    window.setTimeout(() => {
      set((state) => ({ toasts: state.toasts.filter((item) => item.id !== id) }))
    }, 4500)
  },
  dismiss: (id) => set((state) => ({ toasts: state.toasts.filter((toast) => toast.id !== id) })),
}))

export const toast = {
  success: (title: string, description?: string) => useToastStore.getState().push({ title, description, variant: "success" }),
  error: (title: string, description?: string) => useToastStore.getState().push({ title, description, variant: "error" }),
  info: (title: string, description?: string) => useToastStore.getState().push({ title, description, variant: "info" }),
}

export function ToastViewport() {
  const toasts = useToastStore((state) => state.toasts)
  const dismiss = useToastStore((state) => state.dismiss)

  return (
    <div className="fixed right-4 top-4 z-50 flex w-[min(420px,calc(100vw-2rem))] flex-col gap-2">
      <AnimatePresence>
        {toasts.map((item) => {
          const Icon = item.variant === "success" ? CheckCircle2 : item.variant === "error" ? AlertTriangle : Info
          return (
            <motion.div
              key={item.id}
              initial={{ opacity: 0, x: 24, scale: 0.98 }}
              animate={{ opacity: 1, x: 0, scale: 1 }}
              exit={{ opacity: 0, x: 24, scale: 0.98 }}
              className={cn(
                "rounded-md border bg-zinc-950/95 p-4 shadow-2xl backdrop-blur",
                item.variant === "success" && "border-emerald-500/30",
                item.variant === "error" && "border-red-500/30",
                item.variant === "info" && "border-cyan-500/30",
              )}
            >
              <div className="flex items-start gap-3">
                <Icon className={cn("mt-0.5 size-5", item.variant === "success" && "text-emerald-300", item.variant === "error" && "text-red-300", item.variant === "info" && "text-cyan-300")} />
                <div className="min-w-0 flex-1">
                  <div className="text-sm font-semibold text-zinc-100">{item.title}</div>
                  {item.description ? <div className="mt-1 text-sm text-zinc-500">{item.description}</div> : null}
                </div>
                <Button variant="ghost" size="icon" className="size-7" onClick={() => dismiss(item.id)} aria-label="Dismiss notification">
                  <X className="size-4" />
                </Button>
              </div>
            </motion.div>
          )
        })}
      </AnimatePresence>
    </div>
  )
}
