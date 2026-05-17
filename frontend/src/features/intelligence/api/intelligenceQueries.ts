import { useMutation, useQuery } from "@tanstack/react-query"
import { intelligenceApi } from "@/features/intelligence/api/intelligenceApi"
import type { ExecuteQueryRequest } from "@/features/intelligence/model/intelligenceTypes"

export function useQueryTemplatesQuery() {
  return useQuery({
    queryKey: ["intelligence", "templates"],
    queryFn: intelligenceApi.templates,
  })
}

export function useExecuteQueryMutation() {
  return useMutation({
    mutationFn: ({ code, request }: { code: string; request: ExecuteQueryRequest }) => intelligenceApi.execute(code, request),
  })
}

export function useExportQueryMutation() {
  return useMutation({
    mutationFn: ({ code, request }: { code: string; request: ExecuteQueryRequest }) => intelligenceApi.exportCsv(code, request),
  })
}
