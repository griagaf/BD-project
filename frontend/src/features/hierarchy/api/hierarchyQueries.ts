import { useQuery } from "@tanstack/react-query"
import { hierarchyApi } from "@/features/hierarchy/api/hierarchyApi"
import type { HierarchySelection, TreeMode } from "@/features/hierarchy/model/hierarchyTypes"

export function useHierarchyRootsQuery(mode: TreeMode) {
  return useQuery({
    queryKey: ["hierarchy", "roots", mode],
    queryFn: () => hierarchyApi.roots(mode),
    staleTime: 2 * 60_000,
  })
}

export function useHierarchyChildrenQuery(selection: HierarchySelection | null, enabled: boolean) {
  return useQuery({
    queryKey: ["hierarchy", "children", selection?.type, selection?.id],
    queryFn: () => hierarchyApi.children(selection!.type, selection!.id),
    enabled: Boolean(selection && enabled),
    staleTime: 2 * 60_000,
    placeholderData: (previous) => previous,
  })
}

export function useFocusTreeQuery(selection: HierarchySelection | null) {
  return useQuery({
    queryKey: ["hierarchy", "focus", selection?.type, selection?.id],
    queryFn: () => hierarchyApi.focus(selection!.type, selection!.id),
    enabled: Boolean(selection),
    staleTime: 60_000,
    placeholderData: (previous) => previous,
  })
}

export function useObjectPassportQuery(selection: HierarchySelection | null) {
  return useQuery({
    queryKey: ["hierarchy", "passport", selection?.type, selection?.id],
    queryFn: () => hierarchyApi.passport(selection!.type, selection!.id),
    enabled: Boolean(selection),
    staleTime: 30_000,
    placeholderData: (previous) => previous,
  })
}

export function useHierarchyContextQuery(selection: HierarchySelection | null) {
  return useQuery({
    queryKey: ["hierarchy", "context", selection?.type, selection?.id],
    queryFn: () => hierarchyApi.context(selection!.type, selection!.id),
    enabled: Boolean(selection),
    staleTime: 30_000,
    placeholderData: (previous) => previous,
  })
}

export function useUnitPassportQuery(unitId: number) {
  return useQuery({
    queryKey: ["units", unitId, "passport"],
    queryFn: () => hierarchyApi.unitPassport(unitId),
    enabled: Number.isFinite(unitId) && unitId > 0,
    staleTime: 30_000,
  })
}
