import { Crosshair } from "lucide-react"
import { InventoryResourcePage } from "@/pages/equipment/EquipmentPage"

export function WeaponsPage() {
  return <InventoryResourcePage kind="weapons" title="Weapons" icon={<Crosshair className="size-4" />} />
}
