import { ArrowLeft, BadgeCheck, CalendarDays, Network } from "lucide-react"
import { useTranslation } from "react-i18next"
import { Link, useParams } from "react-router-dom"
import { usePersonnelProfileQuery } from "@/features/personnel/api/personnelQueries"
import { Card } from "@/shared/ui/card"

export function PersonnelProfilePage() {
  const { t } = useTranslation(["personnel", "common"])
  const params = useParams()
  const id = Number(params.id)
  const { data, isLoading, error } = usePersonnelProfileQuery(id)

  if (isLoading) {
    return <Card>{t("profile.loading")}</Card>
  }

  if (error || !data) {
    return <Card className="border-red-950 bg-red-950/20 text-red-200">{t("profile.unavailable")}</Card>
  }

  return (
    <div className="space-y-5">
      <Link to="/personnel" className="inline-flex items-center gap-2 text-sm text-zinc-400 hover:text-zinc-100">
        <ArrowLeft className="h-4 w-4 shrink-0" />
        {t("page.title")}
      </Link>

      <Card className="grid gap-6 md:grid-cols-[1.2fr_0.8fr]">
        <div>
          <div className="text-xs uppercase text-emerald-300">{t("profile.eyebrow")}</div>
          <h1 className="mt-2 text-3xl font-semibold text-zinc-100">{data.personnel.fullName}</h1>
          <div className="mt-2 text-sm text-zinc-500">{data.personnel.personalNumber}</div>
          <div className="mt-5 grid gap-3 sm:grid-cols-2">
            <Info label={t("table.rank")} value={data.personnel.rank?.name ?? t("table.noRank")} />
            <Info label={t("table.unit")} value={data.personnel.unitName} />
            <Info label={t("filters.subdivision")} value={data.personnel.subdivisionName} />
            <Info label={t("profile.formation")} value={data.formationName ?? t("profile.noFormation")} />
          </div>
        </div>
        <div className="rounded-md border border-zinc-800 bg-zinc-900 p-4">
          <div className="flex items-center gap-2 text-sm font-medium text-zinc-100">
            <CalendarDays className="h-4 w-4 shrink-0 text-emerald-300" />
            {t("profile.timeline")}
          </div>
          <div className="mt-4 space-y-3 text-sm">
            <Info label={t("form.birthDate")} value={data.personnel.birthDate} />
            <Info label={t("form.serviceStart")} value={data.personnel.serviceStart} />
            <Info label={t("profile.profileDate")} value={data.profileGeneratedAt} />
          </div>
        </div>
      </Card>

      <Card>
        <div className="mb-4 flex items-center gap-2 text-sm font-medium text-zinc-100">
          <Network className="h-4 w-4 shrink-0 text-emerald-300" />
          {t("profile.chainOfCommand")}
        </div>
        <div className="space-y-3">
          {data.chainOfCommand.map((node) => (
            <div key={`${node.objectType}-${node.objectId}`} className="flex items-center justify-between rounded-md border border-zinc-800 bg-zinc-900 p-3">
              <div>
                <div className="truncate text-sm text-zinc-100" title={node.objectName}>{node.objectName}</div>
                <div className="text-xs uppercase text-zinc-500">{node.objectType}</div>
              </div>
              <div className="flex items-center gap-2 text-sm text-zinc-300">
                <BadgeCheck className="h-4 w-4 shrink-0 text-emerald-300" />
                <span className="truncate" title={node.commanderName || t("profile.noCommander")}>{node.commanderName || t("profile.noCommander")}</span>
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
      <div className="mt-1 break-words text-sm text-zinc-100">{value}</div>
    </div>
  )
}
