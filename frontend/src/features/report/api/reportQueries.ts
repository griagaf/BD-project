import { useMutation } from "@tanstack/react-query"
import { reportApi } from "@/features/report/api/reportApi"
import type { SmartMissionReportRequest } from "@/features/report/model/reportTypes"

export function useGenerateSmartMissionReportMutation() {
  return useMutation({
    mutationFn: (request: SmartMissionReportRequest) => reportApi.generate(request),
  })
}

export function useExportSmartMissionReportCsvMutation() {
  return useMutation({
    mutationFn: (request: SmartMissionReportRequest) => reportApi.exportCsv(request),
  })
}
