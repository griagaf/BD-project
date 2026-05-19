import { Bell, LogOut, Menu } from "lucide-react"
import { useNavigate } from "react-router-dom"
import { useTranslation } from "react-i18next"
import { useAuthStore } from "@/features/auth/model/authStore"
import { RolePreview } from "@/features/auth/ui/RolePreview"
import { ViewAsSelector } from "@/features/auth/ui/ViewAsSelector"
import { Button } from "@/shared/ui/button"
import { toast } from "@/shared/ui/toast"
import { LanguageSwitcher } from "@/shared/ui/language-switcher"

export function Topbar() {
  const navigate = useNavigate()
  const { t } = useTranslation("common")
  const clearTokens = useAuthStore((state) => state.clearTokens)

  function logout() {
    clearTokens()
    toast.info(t("session.closed"), t("session.closedDescription"))
    navigate("/login", { replace: true })
  }

  return (
    <header className="sticky top-0 z-20 flex min-h-16 items-center justify-between gap-3 border-b border-zinc-800 bg-zinc-950/88 px-4 py-3 backdrop-blur sm:px-6">
      <div className="flex items-center gap-3">
        <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-md border border-zinc-800 bg-zinc-900 lg:hidden">
          <Menu className="h-4 w-4 shrink-0 text-emerald-300" />
        </div>
        <RolePreview />
      </div>

      <div className="flex flex-wrap items-center justify-end gap-2">
        <ViewAsSelector />
        <LanguageSwitcher />
        <Button variant="ghost" size="icon" aria-label={t("alerts:page.title")}>
          <Bell className="h-4 w-4 shrink-0" />
        </Button>
        <Button variant="secondary" onClick={logout}>
          <LogOut className="h-4 w-4 shrink-0" />
          <span className="hidden sm:inline">{t("actions.logout")}</span>
        </Button>
      </div>
    </header>
  )
}
