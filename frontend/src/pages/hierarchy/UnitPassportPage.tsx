import { ArrowLeft, Building2 } from "lucide-react"
import { useNavigate, useParams } from "react-router-dom"
import { useTranslation } from "react-i18next"
import { useUnitPassportQuery } from "@/features/hierarchy/api/hierarchyQueries"
import { ObjectPassport } from "@/features/hierarchy/ui/ObjectPassport"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"

export function UnitPassportPage() {
  const { t } = useTranslation("hierarchy")
  const params = useParams()
  const navigate = useNavigate()
  const unitId = Number(params.id)
  const { data: passport, isLoading, error } = useUnitPassportQuery(unitId)

  return (
    <div className="space-y-5">
      <div className="flex flex-col gap-3 md:flex-row md:items-center md:justify-between">
        <div>
          <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
            <Building2 className="h-4 w-4 shrink-0" />
            <span className="truncate" title={t("passport.unit")}>{t("passport.unit")}</span>
          </div>
          <h1 className="mt-1 break-words text-2xl font-semibold text-zinc-100">{passport?.name ?? t("passport.fallbackUnit")}</h1>
          <p className="mt-1 break-words text-sm text-zinc-500">{passport?.subtitle ?? t("passport.description")}</p>
        </div>
        <Button type="button" variant="secondary" onClick={() => navigate("/hierarchy")}>
          <ArrowLeft className="h-4 w-4 shrink-0" />
          {t("passport.back")}
        </Button>
      </div>

      {error ? (
        <Card className="border-red-950 bg-red-950/20 text-sm text-red-200">{t("passport.error")}</Card>
      ) : (
        <ObjectPassport passport={passport} loading={isLoading} />
      )}
    </div>
  )
}
