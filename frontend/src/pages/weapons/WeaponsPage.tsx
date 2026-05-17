import { Crosshair } from "lucide-react"
import { InventoryResourcePage } from "@/pages/equipment/EquipmentPage"

export function WeaponsPage() {
  return <InventoryResourcePage kind="weapons" icon={<Crosshair className="h-4 w-4 shrink-0" />} />
}
