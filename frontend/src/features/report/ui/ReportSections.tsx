import type { ReactNode } from "react"
import { useTranslation } from "react-i18next"
import { Boxes, Building2, Crosshair, ShieldCheck, Users } from "lucide-react"
import type { SmartMissionReport } from "@/features/report/model/reportTypes"

type SectionProps = {
  title: string
  action?: ReactNode
  children: ReactNode
}

export function ReportSection({ title, action, children }: SectionProps) {
  return (
    <section className="rounded-md border border-zinc-800 bg-zinc-950/70 p-5">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-sm font-semibold uppercase text-zinc-300">{title}</h2>
        {action}
      </div>
      {children}
    </section>
  )
}

export function ReportSections({ report }: { report: SmartMissionReport }) {
  const { t } = useTranslation(["reports", "equipment", "weapons", "buildings", "personnel"])

  return (
    <div className="space-y-4">
      <ReportSection title={t("sections.commanders")}>
        <div className="grid gap-3 md:grid-cols-2">
          {report.commanders.map((commander) => (
            <div key={`${commander.position}-${commander.personnelId}`} className="rounded-md border border-zinc-800 bg-zinc-900/50 p-3">
              <div className="flex items-center gap-2 text-sm font-medium text-zinc-100">
                <ShieldCheck className="h-4 w-4 shrink-0 text-emerald-300" />
                <span className="truncate" title={commander.fullName}>{commander.fullName}</span>
              </div>
              <div className="mt-1 line-clamp-2 text-xs text-zinc-500" title={`${commander.rankName ?? t("personnel:table.noRank")} / ${commander.position} / ${commander.objectName}`}>
                {commander.rankName ?? t("personnel:table.noRank")} / {commander.position} / {commander.objectName}
              </div>
            </div>
          ))}
          {!report.commanders.length ? <div className="text-sm text-zinc-500">{t("sections.commandersEmpty")}</div> : null}
        </div>
      </ReportSection>

      <ReportSection title={t("sections.personnel")}>
        <MetricGrid
          items={[
            [t("metrics.total"), report.personnel.total],
            [t("metrics.officers"), report.personnel.officers],
            [t("metrics.enlisted"), report.personnel.enlisted],
            [t("sections.commanders"), report.personnel.commanders],
          ]}
          icon={<Users className="h-4 w-4 shrink-0 text-emerald-300" />}
        />
        <KeyValueList values={report.personnel.bySubdivision} empty={t("sections.personnelEmpty")} />
      </ReportSection>

      <ReportSection title={t("sections.resources")}>
        <div className="grid gap-4 lg:grid-cols-2">
          <ResourcePanel
            title={t("equipment:page.title")}
            icon={<Boxes className="h-4 w-4 shrink-0 text-emerald-300" />}
            total={report.equipment.totalQuantity}
            types={report.equipment.typesCount}
            gaps={report.equipment.unitsWithoutEquipment}
            items={report.equipment.topEquipment.map((item) => [item.typeName, `${item.quantity} / ${item.categoryName}`])}
          />
          <ResourcePanel
            title={t("weapons:page.title")}
            icon={<Crosshair className="h-4 w-4 shrink-0 text-emerald-300" />}
            total={report.weapons.totalQuantity}
            types={report.weapons.typesCount}
            gaps={report.weapons.unitsWithoutWeapons}
            items={report.weapons.topWeapons.map((item) => [item.typeName, `${item.quantity} / ${item.categoryName}`])}
          />
        </div>
      </ReportSection>

      <ReportSection title={t("sections.infrastructure")}>
        <MetricGrid
          items={[
            [t("buildings:page.title"), report.buildings.total],
            [t("metrics.unused"), report.buildings.unused],
            [t("metrics.overloaded"), report.buildings.overloaded],
          ]}
          icon={<Building2 className="h-4 w-4 shrink-0 text-emerald-300" />}
        />
        <div className="mt-4 space-y-2">
          {report.buildings.problemBuildings.map((building) => (
            <div key={building.buildingId} className="flex items-center justify-between rounded-md border border-zinc-800 bg-zinc-900/50 px-3 py-2">
              <span className="truncate text-sm text-zinc-200" title={building.buildingName}>{building.buildingName}</span>
              <span className="shrink-0 text-xs text-zinc-500">{building.unitName} / {t("metrics.subdivisions", { count: building.subdivisionsCount })}</span>
            </div>
          ))}
          {!report.buildings.problemBuildings.length ? <div className="text-sm text-zinc-500">{t("sections.buildingsEmpty")}</div> : null}
        </div>
      </ReportSection>

      <ReportSection title={t("sections.specialties")}>
        <MetricGrid
          items={[
            [t("metrics.totalSpecialties"), report.specialties.totalSpecialties],
            [t("metrics.covered"), report.specialties.coveredSpecialties],
            [t("metrics.missing"), report.specialties.missingSpecialties],
          ]}
        />
        <KeyValueList values={report.specialties.topSpecialties} empty={t("sections.specialtiesEmpty")} />
      </ReportSection>

      <ReportSection title={t("sections.alerts")}>
        <div className="space-y-2">
          {report.alerts.map((alert) => (
            <div key={alert.alertId} className="rounded-md border border-zinc-800 bg-zinc-900/50 px-3 py-2">
              <div className="flex items-center justify-between gap-3">
                <div className="text-sm font-medium text-zinc-100">{alert.title}</div>
                <span className="rounded bg-zinc-800 px-2 py-1 text-xs text-amber-200">{alert.severity}</span>
              </div>
              <div className="mt-1 text-xs text-zinc-500">{alert.message}</div>
            </div>
          ))}
          {!report.alerts.length ? <div className="text-sm text-zinc-500">{t("sections.alertsEmpty")}</div> : null}
        </div>
      </ReportSection>
    </div>
  )
}

function MetricGrid({ items, icon }: { items: Array<[string, number]>; icon?: ReactNode }) {
  return (
    <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
      {items.map(([label, value]) => (
        <div key={label} className="rounded-md border border-zinc-800 bg-zinc-900/50 p-3">
          <div className="flex items-center justify-between text-xs uppercase text-zinc-500">
            {label}
            {icon}
          </div>
          <div className="mt-2 text-2xl font-semibold text-zinc-100">{value}</div>
        </div>
      ))}
    </div>
  )
}

function ResourcePanel({ title, icon, total, types, gaps, items }: { title: string; icon: ReactNode; total: number; types: number; gaps: number; items: Array<[string, string]> }) {
  const { t } = useTranslation("reports")

  return (
    <div className="rounded-md border border-zinc-800 bg-zinc-900/50 p-4">
      <div className="mb-3 flex items-center gap-2 text-sm font-semibold text-zinc-100">{icon}<span className="truncate">{title}</span></div>
      <MetricGrid items={[[t("metrics.quantity"), total], [t("metrics.types"), types], [t("metrics.gaps"), gaps]]} />
      <div className="mt-3 space-y-2">
        {items.map(([name, value]) => (
          <div key={name} className="flex justify-between gap-3 text-sm">
            <span className="truncate text-zinc-300" title={name}>{name}</span>
            <span className="shrink-0 text-zinc-500">{value}</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function KeyValueList({ values, empty }: { values: Record<string, number>; empty: string }) {
  const entries = Object.entries(values)
  if (!entries.length) {
    return <div className="mt-4 text-sm text-zinc-500">{empty}</div>
  }
  return (
    <div className="mt-4 grid gap-2 md:grid-cols-2">
      {entries.map(([key, value]) => (
        <div key={key} className="flex items-center justify-between rounded-md border border-zinc-800 bg-zinc-900/40 px-3 py-2 text-sm">
          <span className="text-zinc-300">{key}</span>
          <span className="text-zinc-500">{value}</span>
        </div>
      ))}
    </div>
  )
}
