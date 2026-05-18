import { motion } from "framer-motion"
import { AlertTriangle, Boxes, Building2, Network, Shield, Users } from "lucide-react"
import type { DashboardStatistics } from "@/features/dashboard/model/dashboardTypes"
import { Card } from "@/shared/ui/card"

type StatsCardsProps = {
  statistics: DashboardStatistics
}

export function StatsCards({ statistics }: StatsCardsProps) {
  const cards = [
    { label: "Formations", value: statistics.formations, icon: Network },
    { label: "Units", value: statistics.units, icon: Shield },
    { label: "Personnel", value: statistics.personnel, icon: Users },
    { label: "Equipment", value: statistics.equipmentQuantity, icon: Boxes },
    { label: "Weapons", value: statistics.weaponQuantity, icon: Shield },
    { label: "Buildings", value: statistics.buildings, icon: Building2 },
    { label: "Open alerts", value: statistics.openAlerts, icon: AlertTriangle },
  ]

  return (
    <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
      {cards.map((item, index) => (
        <motion.div key={item.label} initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ delay: index * 0.03 }}>
          <Card className="relative overflow-hidden border-emerald-950/40 bg-zinc-950 shadow-[0_0_24px_rgba(16,185,129,0.05)]">
            <div className="absolute inset-x-0 top-0 h-px bg-emerald-400/40" />
            <div className="flex items-center justify-between">
              <div className="text-sm text-zinc-500">{item.label}</div>
              <item.icon className="size-4 text-emerald-300" />
            </div>
            <motion.div className="mt-3 text-3xl font-semibold text-zinc-100" initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
              {item.value.toLocaleString()}
            </motion.div>
          </Card>
        </motion.div>
      ))}
    </div>
  )
}
