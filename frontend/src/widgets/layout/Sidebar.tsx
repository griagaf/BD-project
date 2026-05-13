import { Activity, AlertTriangle, Binary, FileText, Network, Shield, Users } from "lucide-react"
import { NavLink } from "react-router-dom"
import { cn } from "@/shared/lib/cn"

const navigationItems = [
  { label: "Dashboard", path: "/dashboard", icon: Activity },
  { label: "Hierarchy", path: "/hierarchy", icon: Network },
  { label: "Personnel", path: "/personnel", icon: Users },
  { label: "Intelligence", path: "/intelligence", icon: Binary },
  { label: "Alerts", path: "/alerts", icon: AlertTriangle },
  { label: "Reports", path: "/reports", icon: FileText },
  { label: "Users", path: "/admin/users", icon: Shield },
]

export function Sidebar() {
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
        {navigationItems.map((item) => (
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

