import { FormEvent, useState } from "react"
import { LockKeyhole, RadioTower } from "lucide-react"
import { useNavigate } from "react-router-dom"
import { useQueryClient } from "@tanstack/react-query"
import { useTranslation } from "react-i18next"
import { useLoginMutation } from "@/features/auth/api/authQueries"
import { useAuthStore } from "@/features/auth/model/authStore"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"
import { toast } from "@/shared/ui/toast"

const demoUsers = [
  "admin.district",
  "analyst.staff",
  "army.cmd.1",
  "formation.cmd.1",
  "unit.cmd.1",
  "company.cmd.1",
  "platoon.cmd.1",
  "squad.cmd.1",
  "soldier.demo",
]

export function LoginPage() {
  const navigate = useNavigate()
  const { t } = useTranslation(["common", "auth"])
  const queryClient = useQueryClient()
  const setTokens = useAuthStore((state) => state.setTokens)
  const clearSimulation = useAuthStore((state) => state.clearSimulation)
  const loginMutation = useLoginMutation()
  const [username, setUsername] = useState("admin.district")
  const [password, setPassword] = useState("password")

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const response = await loginMutation.mutateAsync({ username, password })
    clearSimulation()
    setTokens({
      accessToken: response.accessToken,
      refreshToken: response.refreshToken,
    })
    toast.success(t("toasts.accessGranted"), t("toasts.signedInAs", { name: response.user.displayName }))
    queryClient.invalidateQueries({ queryKey: ["auth"] })
    navigate("/dashboard", { replace: true })
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-6 py-8 text-zinc-100">
      <Card className="w-full max-w-md border-emerald-500/20 shadow-[0_0_42px_rgba(16,185,129,0.12)]">
        <div className="mb-8">
          <div className="flex items-center gap-2 text-xs uppercase text-emerald-400">
            <RadioTower className="h-4 w-4 shrink-0" />
            {t("app.name")}
          </div>
          <h1 className="mt-2 text-2xl font-semibold text-zinc-100">{t("auth:login.title")}</h1>
          <p className="mt-2 text-sm text-zinc-500">{t("auth:login.description")}</p>
        </div>

        <form className="space-y-4" onSubmit={submit}>
          <label className="space-y-2">
            <span className="text-xs uppercase text-zinc-500">{t("auth:login.username")}</span>
            <input
              value={username}
              onChange={(event) => setUsername(event.target.value)}
              autoComplete="username"
              className="h-11 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none transition-colors focus:border-emerald-500"
            />
          </label>
          <label className="space-y-2">
            <span className="text-xs uppercase text-zinc-500">{t("auth:login.password")}</span>
            <input
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              type="password"
              autoComplete="current-password"
              className="h-11 w-full rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none transition-colors focus:border-emerald-500"
            />
          </label>

          {loginMutation.error ? (
            <div className="rounded-md border border-red-950 bg-red-950/20 px-3 py-2 text-sm text-red-200">
              {t("auth:login.failed")}
            </div>
          ) : null}

          <Button className="h-11 w-full" disabled={loginMutation.isPending || !username || !password}>
            <LockKeyhole className="h-4 w-4 shrink-0" />
            {loginMutation.isPending ? t("auth:login.pending") : t("actions.login")}
          </Button>
        </form>

        <div className="mt-6">
          <div className="mb-2 text-xs uppercase text-zinc-500">{t("auth:login.demoUsers")}</div>
          <div className="grid gap-2 sm:grid-cols-2">
            {demoUsers.map((demoUser) => (
              <button
                key={demoUser}
                type="button"
                onClick={() => {
                  setUsername(demoUser)
                  setPassword("password")
                }}
                className="rounded-md border border-zinc-800 bg-zinc-900/60 px-3 py-2 text-left text-xs text-zinc-300 transition-colors hover:border-emerald-500/40 hover:text-emerald-200"
              >
                {demoUser}
              </button>
            ))}
          </div>
        </div>
      </Card>
    </div>
  )
}
