import { ArrowLeft, BadgeCheck, CalendarDays, Network } from "lucide-react"
import { Link, useParams } from "react-router-dom"
import { usePersonnelProfileQuery } from "@/features/personnel/api/personnelQueries"
import { Card } from "@/shared/ui/card"

export function PersonnelProfilePage() {
  const params = useParams()
  const id = Number(params.id)
  const { data, isLoading, error } = usePersonnelProfileQuery(id)

  if (isLoading) {
    return <Card>Loading personnel profile</Card>
  }

  if (error || !data) {
    return <Card className="border-red-950 bg-red-950/20 text-red-200">Personnel profile unavailable</Card>
  }

  return (
    <div className="space-y-5">
      <Link to="/personnel" className="inline-flex items-center gap-2 text-sm text-zinc-400 hover:text-zinc-100">
        <ArrowLeft className="size-4" />
        Personnel
      </Link>

      <Card className="grid gap-6 md:grid-cols-[1.2fr_0.8fr]">
        <div>
          <div className="text-xs uppercase text-emerald-300">Personnel profile</div>
          <h1 className="mt-2 text-3xl font-semibold text-zinc-100">{data.personnel.fullName}</h1>
          <div className="mt-2 text-sm text-zinc-500">{data.personnel.personalNumber}</div>
          <div className="mt-5 grid gap-3 sm:grid-cols-2">
            <Info label="Rank" value={data.personnel.rank?.name ?? "No rank"} />
            <Info label="Unit" value={data.personnel.unitName} />
            <Info label="Subdivision" value={data.personnel.subdivisionName} />
            <Info label="Formation" value={data.formationName ?? "No formation"} />
          </div>
        </div>
        <div className="rounded-md border border-zinc-800 bg-zinc-900 p-4">
          <div className="flex items-center gap-2 text-sm font-medium text-zinc-100">
            <CalendarDays className="size-4 text-emerald-300" />
            Service timeline
          </div>
          <div className="mt-4 space-y-3 text-sm">
            <Info label="Birth date" value={data.personnel.birthDate} />
            <Info label="Service start" value={data.personnel.serviceStart} />
            <Info label="Profile date" value={data.profileGeneratedAt} />
          </div>
        </div>
      </Card>

      <Card>
        <div className="mb-4 flex items-center gap-2 text-sm font-medium text-zinc-100">
          <Network className="size-4 text-emerald-300" />
          Chain of Command
        </div>
        <div className="space-y-3">
          {data.chainOfCommand.map((node) => (
            <div key={`${node.objectType}-${node.objectId}`} className="flex items-center justify-between rounded-md border border-zinc-800 bg-zinc-900 p-3">
              <div>
                <div className="text-sm text-zinc-100">{node.objectName}</div>
                <div className="text-xs uppercase text-zinc-500">{node.objectType}</div>
              </div>
              <div className="flex items-center gap-2 text-sm text-zinc-300">
                <BadgeCheck className="size-4 text-emerald-300" />
                {node.commanderName || "No commander"}
              </div>
            </div>
          ))}
        </div>
      </Card>
    </div>
  )
}

function Info({ label, value }: { label: string; value: string }) {
  return (
    <div>
      <div className="text-xs uppercase text-zinc-500">{label}</div>
      <div className="mt-1 text-sm text-zinc-100">{value}</div>
    </div>
  )
}
