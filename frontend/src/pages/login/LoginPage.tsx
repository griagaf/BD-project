import { useNavigate } from "react-router-dom"
import { useAuthStore } from "@/features/auth/model/authStore"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"

export function LoginPage() {
  const navigate = useNavigate()
  const setTokens = useAuthStore((state) => state.setTokens)

  function enterSkeleton() {
    setTokens({
      accessToken: "skeleton-token",
      refreshToken: "skeleton-refresh-token",
    })
    navigate("/dashboard", { replace: true })
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-zinc-950 px-6">
      <Card className="w-full max-w-md">
        <div className="mb-8">
          <div className="text-xs uppercase text-emerald-400">TACTICAL DISTRICT COMMAND</div>
          <h1 className="mt-2 text-2xl font-semibold text-zinc-100">Command access</h1>
          <p className="mt-2 text-sm text-zinc-500">
            Production-ready frontend shell. Authentication flow is reserved for the auth module.
          </p>
        </div>

        <Button className="w-full" onClick={enterSkeleton}>
          Enter skeleton interface
        </Button>
      </Card>
    </div>
  )
}

