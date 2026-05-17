import { Outlet } from "react-router-dom"
import { SimulationBanner } from "@/features/auth/ui/SimulationBanner"
import { ContentArea } from "@/widgets/layout/ContentArea"
import { Sidebar } from "@/widgets/layout/Sidebar"
import { Topbar } from "@/widgets/layout/Topbar"

export function TacticalLayout() {
  return (
    <div className="min-h-screen text-zinc-100">
      <Sidebar />
      <div className="min-h-screen lg:pl-72">
        <Topbar />
        <SimulationBanner />
        <ContentArea>
          <Outlet />
        </ContentArea>
      </div>
    </div>
  )
}
