import { Activity, AlertTriangle, Binary, Boxes, Building2, Crosshair, FileText, Network, Shield, Users } from "lucide-react"
import { NavLink } from "react-router-dom"
import { useCurrentUserQuery } from "@/features/auth/api/authQueries"
import { cn } from "@/shared/lib/cn"
import { Badge } from "@/shared/ui/badge"

const navigationItems = [
  { label: "Dashboard", path: "/dashboard", icon: Activity, permissions: ["dashboard:read"] },
  { label: "Hierarchy", path: "/hierarchy", icon: Network, permissions: ["structure:read"] },
  { label: "Personnel", path: "/personnel", icon: Users, permissions: ["personnel:read"] },
  { label: "Equipment", path: "/equipment", icon: Boxes, permissions: ["equipment:read"] },
  { label: "Weapons", path: "/weapons", icon: Crosshair, permissions: ["weapon:read"] },
  { label: "Buildings", path: "/buildings", icon: Building2, permissions: ["building:read"] },
  { label: "Intelligence", path: "/intelligence", icon: Binary, permissions: ["query:execute"] },
  { label: "Alerts", path: "/alerts", icon: AlertTriangle, permissions: ["alert:read"] },
  { label: "Reports", path: "/reports", icon: FileText, permissions: ["report:read", "report:generate"] },
  { label: "Users", path: "/admin/users", icon: Shield, permissions: ["user:manage"] },
]

export function Sidebar() {
  const { data: user } = useCurrentUserQuery()
  const visibleItems = navigationItems.filter((item) => item.permissions.some((permission) => user?.permissions.includes(permission)))

  return (
    <>
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-72 border-r border-zinc-800 bg-zinc-950/95 backdrop-blur lg:block">
      <div className="flex h-16 items-center border-b border-zinc-800 px-5">
        <div className="mr-3 flex size-9 items-center justify-center rounded-md border border-emerald-500/25 bg-emerald-500/10">
          <Shield className="size-5 text-emerald-400" />
        </div>
        <div>
          <div className="text-sm font-semibold uppercase tracking-wide">Tactical District</div>
          <div className="text-xs text-zinc-500">Command Center</div>
        </div>
      </div>

      <nav className="space-y-1 p-3">
        {visibleItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-md border border-transparent px-3 py-2.5 text-sm text-zinc-400 transition-colors",
                "hover:border-zinc-800 hover:bg-zinc-900 hover:text-zinc-100",
                isActive && "border-emerald-500/20 bg-emerald-500/10 text-emerald-200",
              )
            }
          >
            <item.icon className="size-4" />
            {item.label}
          </NavLink>
        ))}
      </nav>

      <div className="absolute inset-x-3 bottom-3 rounded-md border border-zinc-800 bg-zinc-900/70 p-3">
        <div className="text-xs uppercase text-zinc-500">Session</div>
        <div className="mt-2 flex flex-wrap gap-2">
          {(user?.effectiveRoles ?? user?.roles ?? []).slice(0, 2).map((role) => (
            <Badge key={role} variant={user?.accessSimulationActive ? "warning" : "default"}>{role}</Badge>
          ))}
        </div>
      </div>
    </aside>
    <nav className="fixed inset-x-0 bottom-0 z-30 flex gap-1 border-t border-zinc-800 bg-zinc-950/95 p-2 backdrop-blur lg:hidden">
      {visibleItems.slice(0, 5).map((item) => (
        <NavLink
          key={item.path}
          to={item.path}
          className={({ isActive }) =>
            cn(
              "flex min-w-0 flex-1 flex-col items-center gap-1 rounded-md px-2 py-2 text-[11px] text-zinc-500",
              isActive && "bg-emerald-500/10 text-emerald-200",
            )
          }
        >
          <item.icon className="size-4" />
          <span className="max-w-full truncate">{item.label}</span>
        </NavLink>
      ))}
    </nav>
    </>
  )
}
