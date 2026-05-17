import { Bell, LogOut, Menu } from "lucide-react"
import { useNavigate } from "react-router-dom"
import { useAuthStore } from "@/features/auth/model/authStore"
import { RolePreview } from "@/features/auth/ui/RolePreview"
import { ViewAsSelector } from "@/features/auth/ui/ViewAsSelector"
import { Button } from "@/shared/ui/button"
import { toast } from "@/shared/ui/toast"

export function Topbar() {
  const navigate = useNavigate()
  const clearTokens = useAuthStore((state) => state.clearTokens)

  function logout() {
    clearTokens()
    toast.info("Session closed", "Authentication state has been cleared")
    navigate("/login", { replace: true })
  }

  return (
    <header className="sticky top-0 z-20 flex min-h-16 items-center justify-between gap-3 border-b border-zinc-800 bg-zinc-950/88 px-4 py-3 backdrop-blur sm:px-6">
      <div className="flex items-center gap-3">
        <div className="flex size-9 items-center justify-center rounded-md border border-zinc-800 bg-zinc-900 lg:hidden">
          <Menu className="size-4 text-emerald-300" />
        </div>
        <RolePreview />
      </div>

      <div className="flex flex-wrap items-center justify-end gap-2">
        <ViewAsSelector />
        <Button variant="ghost" size="icon" aria-label="Alerts">
          <Bell className="size-4" />
        </Button>
        <Button variant="secondary" onClick={logout}>
          <LogOut className="size-4" />
          Logout
        </Button>
      </div>
    </header>
  )
}
