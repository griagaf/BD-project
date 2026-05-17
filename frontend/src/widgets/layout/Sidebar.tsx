import { Activity, AlertTriangle, Binary, Boxes, Building2, Crosshair, FileText, Network, Shield, Users } from "lucide-react"
import { NavLink } from "react-router-dom"
import { useCurrentUserQuery } from "@/features/auth/api/authQueries"
import { cn } from "@/shared/lib/cn"

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
    <aside className="fixed inset-y-0 left-0 w-72 border-r border-zinc-800 bg-zinc-950">
      <div className="flex h-16 items-center border-b border-zinc-800 px-5">
        <Shield className="mr-3 size-5 text-emerald-400" />
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
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm text-zinc-400",
                "hover:bg-zinc-900 hover:text-zinc-100",
                isActive && "bg-zinc-900 text-emerald-300",
              )
            }
          >
            <item.icon className="size-4" />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
