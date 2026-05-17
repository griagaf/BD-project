import { createBrowserRouter, Navigate } from "react-router-dom"
import { ProtectedRoute } from "@/features/auth/ui/ProtectedRoute"
import { AdminUsersPage } from "@/pages/admin-users/AdminUsersPage"
import { AlertsPage } from "@/pages/alerts/AlertsPage"
import { BuildingsPage } from "@/pages/buildings/BuildingsPage"
import { DashboardPage } from "@/pages/dashboard/DashboardPage"
import { EquipmentPage } from "@/pages/equipment/EquipmentPage"
import { HierarchyPage } from "@/pages/hierarchy/HierarchyPage"
import { UnitPassportPage } from "@/pages/hierarchy/UnitPassportPage"
import { IntelligenceTerminalPage } from "@/pages/intelligence-terminal/IntelligenceTerminalPage"
import { LoginPage } from "@/pages/login/LoginPage"
import { PersonnelPage } from "@/pages/personnel/PersonnelPage"
import { PersonnelProfilePage } from "@/pages/personnel/PersonnelProfilePage"
import { ReportsPage } from "@/pages/reports/ReportsPage"
import { WeaponsPage } from "@/pages/weapons/WeaponsPage"
import { TacticalLayout } from "@/widgets/layout/TacticalLayout"

export const router = createBrowserRouter([
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/",
    element: (
      <ProtectedRoute>
        <TacticalLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: "dashboard", element: <DashboardPage /> },
      { path: "hierarchy", element: <HierarchyPage /> },
      { path: "units/:id", element: <UnitPassportPage /> },
      { path: "personnel", element: <PersonnelPage /> },
      { path: "personnel/:id", element: <PersonnelProfilePage /> },
      { path: "equipment", element: <EquipmentPage /> },
      { path: "weapons", element: <WeaponsPage /> },
      { path: "buildings", element: <BuildingsPage /> },
      { path: "intelligence", element: <IntelligenceTerminalPage /> },
      { path: "alerts", element: <AlertsPage /> },
      { path: "reports", element: <ReportsPage /> },
      { path: "reports/smart-mission", element: <ReportsPage /> },
      { path: "admin/users", element: <AdminUsersPage /> },
    ],
  },
])
