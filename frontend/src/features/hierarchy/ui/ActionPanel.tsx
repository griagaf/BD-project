import { ExternalLink, FileText, Plus, ShieldPlus } from "lucide-react"
import { useNavigate } from "react-router-dom"
import { useState } from "react"
import { useQuery } from "@tanstack/react-query"
import { useTranslation } from "react-i18next"
import { useAssignCommanderMutation, useCreateFormationMutation, useCreateSubdivisionMutation, useCreateUnitMutation } from "@/features/hierarchy/api/hierarchyQueries"
import type { HierarchyContext, HierarchySelection } from "@/features/hierarchy/model/hierarchyTypes"
import { lookupApi } from "@/shared/api/lookupApi"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"
import { SearchableSelect } from "@/shared/ui/searchable-select"
import { toast } from "@/shared/ui/toast"
import { ApiError } from "@/shared/api/apiError"

type ActionPanelProps = {
  selection: HierarchySelection | null
  context?: HierarchyContext
}

export function ActionPanel({ selection, context }: ActionPanelProps) {
  const { t } = useTranslation("hierarchy")
  const navigate = useNavigate()
  const [assignOpen, setAssignOpen] = useState(false)
  const [commanderId, setCommanderId] = useState<number | null>(null)
  const [createOpen, setCreateOpen] = useState(false)
  const [createKind, setCreateKind] = useState<"formation" | "unit" | "subdivision">("subdivision")
  const [subdivisionName, setSubdivisionName] = useState("")
  const [subdivisionType, setSubdivisionType] = useState("")
  const assignCommander = useAssignCommanderMutation()
  const createSubdivision = useCreateSubdivisionMutation()
  const createFormation = useCreateFormationMutation()
  const createUnit = useCreateUnitMutation()
  const { data: personnelOptions = [], isLoading: personnelLoading } = useQuery({
    queryKey: ["lookups", "personnel", "assign-commander"],
    queryFn: () => lookupApi.personnel(),
    staleTime: 5 * 60_000,
    enabled: assignOpen,
  })
  const unitPassportEnabled = selection?.type === "MILITARY_UNIT"
  const assignAction = context?.actions.find((action) => action.code === "ASSIGN_COMMANDER")
  const createAction = context?.actions.find((action) => action.code === "CREATE_CHILD")
  const canAssignCommander = Boolean(selection && (assignAction?.enabled ?? true))
  const childTypes = selection ? childTypesFor(selection.type) : []
  const formationTypes = selection ? formationTypesFor(selection.type) : []
  const canCreateFormation = Boolean(selection && formationTypes.length && (createAction?.enabled ?? true))
  const canCreateUnit = Boolean(selection && ["CORPS", "DIVISION", "BRIGADE", "FORMATION"].includes(selection.type) && (createAction?.enabled ?? true))
  const canCreateSubdivision = Boolean(selection && childTypes.length && (createAction?.enabled ?? true))

  function reportType(type: string) {
    if (type === "CORPS" || type === "DIVISION") return "FORMATION"
    return type
  }

  return (
    <div className="space-y-4">
      <Card className="space-y-3">
        <div>
          <div className="text-xs uppercase text-emerald-300">{t("panel.actions")}</div>
          <h3 className="mt-1 text-lg font-semibold text-zinc-100">{t("panel.commandPanel")}</h3>
        </div>
        <div className="space-y-2">
          <Button
            type="button"
            variant="secondary"
            className="w-full justify-start"
            disabled={!canCreateFormation}
            onClick={() => {
              setCreateKind("formation")
              setSubdivisionName("")
              setSubdivisionType(formationTypes[0] ?? "Бригада")
              setCreateOpen(true)
            }}
          >
            <Plus className="h-4 w-4 shrink-0" />
            <span className="truncate">{t("panel.createFormation")}</span>
          </Button>
          <Button
            type="button"
            variant="secondary"
            className="w-full justify-start"
            disabled={!canCreateUnit}
            onClick={() => {
              setCreateKind("unit")
              setSubdivisionName("")
              setSubdivisionType("")
              setCreateOpen(true)
            }}
          >
            <Plus className="h-4 w-4 shrink-0" />
            <span className="truncate">{t("panel.createUnit")}</span>
          </Button>
          <Button
            type="button"
            variant="secondary"
            className="w-full justify-start"
            disabled={!canCreateSubdivision}
            onClick={() => {
              setCreateKind("subdivision")
              setSubdivisionName("")
              setSubdivisionType(childTypes[0] ?? "")
              setCreateOpen(true)
            }}
          >
            <Plus className="h-4 w-4 shrink-0" />
            <span className="truncate">{t("panel.createSubdivision")}</span>
          </Button>
          <Button
            type="button"
            variant="secondary"
            className="w-full justify-start"
            disabled={!canAssignCommander}
            onClick={() => {
              setCommanderId(null)
              setAssignOpen(true)
            }}
          >
            <ShieldPlus className="h-4 w-4 shrink-0" />
            <span className="truncate">{t("panel.assignCommander")}</span>
          </Button>
          <Button
            type="button"
            variant="secondary"
            className="w-full justify-start"
            disabled={!unitPassportEnabled}
            onClick={() => selection && navigate(`/units/${selection.id}`)}
          >
            <ExternalLink className="h-4 w-4 shrink-0" />
            <span className="truncate">{t("panel.unitPassport")}</span>
          </Button>
          <Button
            type="button"
            variant="ghost"
            className="w-full justify-start"
            disabled={!selection}
            onClick={() => selection && navigate(`/reports/smart-mission?objectType=${reportType(selection.type)}&objectId=${selection.id}`)}
          >
            <FileText className="h-4 w-4 shrink-0" />
            <span className="truncate">{t("panel.tacticalReport")}</span>
          </Button>
        </div>
      </Card>
      {assignOpen && selection ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="w-full max-w-lg rounded-md border border-zinc-800 bg-zinc-950 p-5 shadow-2xl">
            <div className="text-xs uppercase text-emerald-300">{t("panel.assignCommander")}</div>
            <h3 className="mt-1 text-lg font-semibold text-zinc-100">{t("panel.commandPanel")}</h3>
            <div className="mt-4">
              <SearchableSelect
                label={t("panel.commander")}
                value={commanderId}
                options={personnelOptions}
                placeholder={t("panel.selectCommander")}
                disabled={personnelLoading || assignCommander.isPending}
                onChange={setCommanderId}
              />
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => setAssignOpen(false)}>{t("panel.cancel")}</Button>
              <Button
                type="button"
                disabled={!commanderId || assignCommander.isPending}
                onClick={() => {
                  if (!commanderId) return
                  assignCommander.mutate({ selection, commanderId }, {
                    onSuccess: () => {
                      toast.success(t("panel.commanderAssigned"))
                      setAssignOpen(false)
                    },
                    onError: (error) => toast.error(errorMessage(error, t("panel.commanderAssignFailed"))),
                  })
                }}
              >
                <ShieldPlus className="h-4 w-4 shrink-0" />
                {t("panel.assignCommander")}
              </Button>
            </div>
          </div>
        </div>
      ) : null}
      {createOpen && selection ? (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="w-full max-w-lg rounded-md border border-zinc-800 bg-zinc-950 p-5 shadow-2xl">
            <div className="text-xs uppercase text-emerald-300">{createTitle(t, createKind)}</div>
            <h3 className="mt-1 text-lg font-semibold text-zinc-100">{t("panel.createObjectTitle")}</h3>
            <div className="mt-4 space-y-4">
              <label className="block space-y-2">
                <span className="text-xs uppercase text-zinc-500">{t("panel.subdivisionName")}</span>
                <input
                  value={subdivisionName}
                  onChange={(event) => setSubdivisionName(event.target.value)}
                  placeholder={createKind === "unit" ? t("panel.unitNamePlaceholder") : createKind === "formation" ? t("panel.formationNamePlaceholder") : t("panel.subdivisionNamePlaceholder")}
                  className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm text-zinc-100 outline-none placeholder:text-zinc-600 focus:border-emerald-500"
                />
              </label>
              {createKind !== "unit" ? <label className="block space-y-2">
                <span className="text-xs uppercase text-zinc-500">{t("panel.subdivisionType")}</span>
                <select
                  value={subdivisionType}
                  onChange={(event) => setSubdivisionType(event.target.value)}
                  className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
                >
                  {(createKind === "formation" ? formationTypes : childTypes).map((type) => <option key={type} value={type}>{type}</option>)}
                </select>
              </label> : null}
            </div>
            <div className="mt-5 flex justify-end gap-2">
              <Button type="button" variant="secondary" onClick={() => setCreateOpen(false)}>{t("panel.cancel")}</Button>
              <Button
                type="button"
                disabled={!subdivisionName.trim() || (createKind !== "unit" && !subdivisionType) || createSubdivision.isPending || createFormation.isPending || createUnit.isPending}
                onClick={() => {
                  if (createKind === "formation") {
                    createFormation.mutate({
                      name: subdivisionName.trim(),
                      formationType: subdivisionType,
                      parentId: selection.id,
                      formationDate: new Date().toISOString().slice(0, 10),
                      status: "Активна",
                      commanderId: null,
                    }, {
                      onSuccess: () => {
                        toast.success(t("panel.objectCreated"))
                        setCreateOpen(false)
                      },
                      onError: (error) => toast.error(errorMessage(error, t("panel.objectCreateFailed"))),
                    })
                    return
                  }
                  if (createKind === "unit") {
                    createUnit.mutate({
                      name: subdivisionName.trim(),
                      formationId: selection.id,
                      locationId: null,
                      commanderId: null,
                    }, {
                      onSuccess: () => {
                        toast.success(t("panel.objectCreated"))
                        setCreateOpen(false)
                      },
                      onError: (error) => toast.error(errorMessage(error, t("panel.objectCreateFailed"))),
                    })
                    return
                  }
                  createSubdivision.mutate({
                    name: subdivisionName.trim(),
                    type: subdivisionType,
                    unitId: selection.type === "MILITARY_UNIT" ? selection.id : 0,
                    parentId: selection.type === "MILITARY_UNIT" ? null : selection.id,
                    commanderId: null,
                  }, {
                    onSuccess: () => {
                      toast.success(t("panel.subdivisionCreated"))
                      setCreateOpen(false)
                    },
                    onError: (error) => toast.error(errorMessage(error, t("panel.subdivisionCreateFailed"))),
                  })
                }}
              >
                <Plus className="h-4 w-4 shrink-0" />
                {createTitle(t, createKind)}
              </Button>
            </div>
          </div>
        </div>
      ) : null}

      <Card className="space-y-3">
        <div className="text-xs uppercase text-emerald-300">{t("panel.statistics")}</div>
        <div className="grid grid-cols-2 gap-2 text-sm">
          <Stat label={t("panel.personnel")} value={context?.statistics.personnelCount ?? 0} />
          <Stat label={t("panel.readiness")} value={`${context?.statistics.readinessScore ?? 0}%`} />
          <Stat label={t("panel.units")} value={context?.statistics.unitCount ?? 0} />
          <Stat label={t("panel.subdivisions")} value={context?.statistics.subdivisionCount ?? 0} />
        </div>
      </Card>
    </div>
  )
}

function childTypesFor(type: string) {
  if (type === "MILITARY_UNIT") return ["Рота", "Батальон"]
  if (type === "BATTALION") return ["Рота"]
  if (type === "COMPANY") return ["Взвод"]
  if (type === "PLATOON") return ["Отделение"]
  return []
}

function formationTypesFor(type: string) {
  if (type === "DISTRICT") return ["Армия"]
  if (type === "ARMY") return ["Корпус", "Дивизия", "Бригада"]
  if (type === "CORPS") return ["Дивизия", "Бригада"]
  return []
}

function createTitle(t: (key: string) => string, kind: "formation" | "unit" | "subdivision") {
  if (kind === "formation") return t("panel.createFormation")
  if (kind === "unit") return t("panel.createUnit")
  return t("panel.createSubdivision")
}

function errorMessage(error: unknown, fallback: string) {
  return error instanceof ApiError ? error.message || fallback : fallback
}

function Stat({ label, value }: { label: string; value: string | number }) {
  return (
    <div className="rounded-md border border-zinc-800 bg-zinc-900/40 p-3">
      <div className="text-xs text-zinc-500">{label}</div>
      <div className="mt-1 text-lg font-semibold text-zinc-100">{value}</div>
    </div>
  )
}
