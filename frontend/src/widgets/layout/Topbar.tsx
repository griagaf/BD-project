import { Bell, LogOut } from "lucide-react"
import { useNavigate } from "react-router-dom"
import { useAuthStore } from "@/features/auth/model/authStore"
import { Button } from "@/shared/ui/button"

export function Topbar() {
  const navigate = useNavigate()
  const clearTokens = useAuthStore((state) => state.clearTokens)

  function logout() {
    clearTokens()
    navigate("/login", { replace: true })
  }

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-zinc-800 bg-zinc-950/90 px-6 backdrop-blur">
      <div>
        <div className="text-xs uppercase text-zinc-500">Active command scope</div>
        <div className="text-sm font-medium text-zinc-200">Skeleton / unrestricted preview</div>
      </div>

      <div className="flex items-center gap-3">
        <Button variant="ghost" aria-label="Alerts">
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

