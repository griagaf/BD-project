import { Outlet } from "react-router-dom"
import { ContentArea } from "@/widgets/layout/ContentArea"
import { Sidebar } from "@/widgets/layout/Sidebar"
import { Topbar } from "@/widgets/layout/Topbar"

export function TacticalLayout() {
  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      <Sidebar />
      <div className="min-h-screen pl-72">
        <Topbar />
        <ContentArea>
          <Outlet />
        </ContentArea>
      </div>
    </div>
  )
}

