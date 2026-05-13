# TACTICAL DISTRICT COMMAND: Focus Tree

## Назначение

**Focus Tree** предоставляет интерактивную визуальную навигацию по структуре военного округа без отображения всей иерархии одновременно.

Иерархия:

```text
Округ
  -> Армия
    -> Корпус / Дивизия / Бригада
      -> Военная часть
        -> Рота
          -> Взвод
            -> Отделение
```

Текущая БД хранит иерархию в трех группах таблиц:

```text
military_formations
  Округ / Армия / Корпус / Дивизия / Бригада

military_units
  Военная часть

subdivisions
  Батальон / Рота / Взвод / Отделение
```

Focus Tree не загружает всю структуру сразу. Он показывает только нужный уровень, выбранную ветку или цепочку подчиненности.

## Режимы

### Strategic Tree

Показывает верхний уровень структуры:

```text
Округ
  -> Армии
    -> Корпуса / Дивизии / Бригады
```

Назначение:

- быстро понять верхнюю структуру округа;
- выбрать область для дальнейшего просмотра;
- перейти к Focus Mode для конкретной ветки.

### Focus Mode

Показывает только выбранную ветку.

Пример:

```text
Западный военный округ
  -> 1-я армия
    -> 2-я дивизия
      -> 7-я бригада
        -> 1-й танковый полк
          -> 1-я рота
            -> 1-й взвод
              -> 1-е отделение
```

Назначение:

- не перегружать интерфейс большим деревом;
- работать с конкретной областью командования;
- быстро переходить между parent, siblings и children.

### Chain Mode

Показывает путь подчиненности для конкретного военнослужащего.

Пример:

```text
Иванов Петр Сергеевич
  -> 1-е отделение
    -> 1-й взвод
      -> 1-я рота
        -> 1-й танковый полк
          -> 7-я бригада
            -> 2-я дивизия
              -> 1-я армия
                -> Западный военный округ
```

Назначение:

- показать место военнослужащего в структуре;
- быстро открыть карточки командиров и подразделений;
- использовать результаты запроса `FIND_PERSONNEL_COMMAND_CHAIN`.

## UI layout

Основной layout:

```text
┌───────────────────┬──────────────────────────────┬──────────────────────┐
│ Tree Panel        │ Passport Card                 │ Context Panel        │
│                   │                              │                      │
│ Strategic / Focus │ selected object details       │ actions              │
│ Search            │ readiness / metadata          │ alerts               │
│ Tree nodes        │ linked resources              │ statistics           │
└───────────────────┴──────────────────────────────┴──────────────────────┘
```

Columns:

| Area | Purpose |
|---|---|
| Left | дерево, режимы, поиск, раскрытие nodes |
| Center | паспорт выбранного объекта |
| Right | действия, alert'ы, статистика, быстрые переходы |

## 1. API endpoints

### Tree roots

```http
GET /api/focus-tree/roots?mode=STRATEGIC
```

Возвращает доступные корневые nodes для текущего пользователя.

Для `ADMIN_DISTRICT` и `STAFF_ANALYST` это округ. Для командиров это назначенная область command scope.

### Children lazy-load

```http
GET /api/focus-tree/nodes/{nodeType}/{nodeId}/children
```

Примеры:

```http
GET /api/focus-tree/nodes/FORMATION/1/children
GET /api/focus-tree/nodes/MILITARY_UNIT/12/children
GET /api/focus-tree/nodes/COMPANY/45/children
```

### Focus branch

```http
GET /api/focus-tree/focus?nodeType=MILITARY_UNIT&nodeId=12
```

Возвращает parent path, выбранный node, siblings и первый уровень children.

### Chain for personnel

```http
GET /api/focus-tree/personnel/{personnelId}/chain
```

Возвращает цепочку подчиненности для военнослужащего.

### Node passport

```http
GET /api/focus-tree/nodes/{nodeType}/{nodeId}/passport
```

Возвращает данные для центральной Passport Card.

### Node side panel

```http
GET /api/focus-tree/nodes/{nodeType}/{nodeId}/context
```

Возвращает действия, alert'ы и статистику для правой панели.

### Search

```http
GET /api/focus-tree/search?query=танковый&types=FORMATION,MILITARY_UNIT,SUBDIVISION,PERSONNEL
```

Search возвращает scoped results.

## 2. DTO для tree node

### TreeNodeDto

```java
public record TreeNodeDto(
        String id,
        TreeNodeType type,
        Long objectId,
        String label,
        String subtitle,
        String status,
        Integer level,
        Boolean hasChildren,
        Boolean childrenLoaded,
        Integer childrenCount,
        TreeNodeMetricsDto metrics,
        List<TreeNodeDto> children
) {
}
```

`id` формируется как стабильный frontend key:

```text
FORMATION:1
MILITARY_UNIT:12
COMPANY:45
PERSONNEL:77
```

### TreeNodeType

```java
public enum TreeNodeType {
    DISTRICT,
    ARMY,
    CORPS,
    DIVISION,
    BRIGADE,
    MILITARY_UNIT,
    BATTALION,
    COMPANY,
    PLATOON,
    SQUAD,
    PERSONNEL
}
```

### Metrics DTO

```java
public record TreeNodeMetricsDto(
        Integer personnelCount,
        Integer unitCount,
        Integer equipmentCount,
        Integer weaponCount,
        Integer alertCount,
        Integer readinessScore
) {
}
```

### FocusTreeResponse

```java
public record FocusTreeResponse(
        TreeMode mode,
        TreeNodeDto selectedNode,
        List<TreeNodeDto> parentPath,
        List<TreeNodeDto> siblings,
        List<TreeNodeDto> children
) {
}
```

### PersonnelChainResponse

```java
public record PersonnelChainResponse(
        Long personnelId,
        String personnelName,
        List<TreeNodeDto> chain
) {
}
```

### Passport DTO

```java
public record NodePassportDto(
        String nodeId,
        TreeNodeType type,
        Long objectId,
        String title,
        String subtitle,
        String status,
        Map<String, Object> attributes,
        List<NodeRelationDto> relations,
        TreeNodeMetricsDto metrics
) {
}
```

### Context DTO

```java
public record NodeContextDto(
        String nodeId,
        List<NodeActionDto> actions,
        List<NodeAlertDto> alerts,
        List<NodeStatisticDto> statistics
) {
}
```

## 3. Selected node на frontend

Selected node хранится в Zustand, потому что он нужен разным компонентам: дереву, паспорту, actions panel и breadcrumbs.

```ts
import { create } from "zustand"

export type TreeMode = "STRATEGIC" | "FOCUS" | "CHAIN"

export type SelectedTreeNode = {
  id: string
  type: TreeNodeType
  objectId: number
  label: string
}

type FocusTreeState = {
  mode: TreeMode
  selectedNode: SelectedTreeNode | null
  expandedNodeIds: string[]
  setMode: (mode: TreeMode) => void
  selectNode: (node: SelectedTreeNode) => void
  toggleExpanded: (nodeId: string) => void
  reset: () => void
}

export const useFocusTreeStore = create<FocusTreeState>((set) => ({
  mode: "STRATEGIC",
  selectedNode: null,
  expandedNodeIds: [],
  setMode: (mode) => set({ mode }),
  selectNode: (node) => set({ selectedNode: node }),
  toggleExpanded: (nodeId) =>
    set((state) => ({
      expandedNodeIds: state.expandedNodeIds.includes(nodeId)
        ? state.expandedNodeIds.filter((id) => id !== nodeId)
        : [...state.expandedNodeIds, nodeId],
    })),
  reset: () => set({ mode: "STRATEGIC", selectedNode: null, expandedNodeIds: [] }),
}))
```

URL синхронизирует выбранный node:

```text
/hierarchy?mode=FOCUS&node=FORMATION:7
/hierarchy?mode=CHAIN&personnelId=44
```

Это позволяет открывать ссылку сразу на нужной ветке.

## 4. Lazy-load children

Дочерние nodes подгружаются по клику раскрытия.

TanStack Query key:

```ts
export const focusTreeKeys = {
  roots: (mode: TreeMode) => ["focus-tree", "roots", mode] as const,
  children: (nodeId: string) => ["focus-tree", "children", nodeId] as const,
  focus: (nodeId: string) => ["focus-tree", "focus", nodeId] as const,
  chain: (personnelId: number) => ["focus-tree", "chain", personnelId] as const,
  passport: (nodeId: string) => ["focus-tree", "passport", nodeId] as const,
  context: (nodeId: string) => ["focus-tree", "context", nodeId] as const,
}
```

Hook:

```ts
export function useTreeChildrenQuery(node: SelectedTreeNode | null, enabled: boolean) {
  return useQuery({
    queryKey: node ? focusTreeKeys.children(node.id) : ["focus-tree", "children", "none"],
    queryFn: () => focusTreeApi.children(node!.type, node!.objectId),
    enabled: Boolean(node && enabled),
    staleTime: 60_000,
  })
}
```

UI behavior:

```text
1. User clicks expand icon.
2. Node becomes expanded.
3. Children query starts.
4. Skeleton rows appear under node.
5. Children render with small slide/fade animation.
6. Subsequent expand uses cached query data.
```

Backend response for unloaded node:

```json
{
  "id": "MILITARY_UNIT:12",
  "type": "MILITARY_UNIT",
  "objectId": 12,
  "label": "1-й танковый полк",
  "subtitle": "Военная часть",
  "status": "ACTIVE",
  "level": 4,
  "hasChildren": true,
  "childrenLoaded": false,
  "childrenCount": 3,
  "metrics": {
    "personnelCount": 240,
    "unitCount": null,
    "equipmentCount": 54,
    "weaponCount": 380,
    "alertCount": 2,
    "readinessScore": 81
  },
  "children": []
}
```

## 5. Roles and scope

Backend фильтрует tree nodes по command scope.

Правила:

| Role | Root nodes |
|---|---|
| `ADMIN_DISTRICT` | округ |
| `STAFF_ANALYST` | округ |
| `ARMY_COMMANDER` | назначенная армия |
| `FORMATION_COMMANDER` | назначенное формирование |
| `UNIT_COMMANDER` | назначенная военная часть |
| `COMPANY_COMMANDER` | назначенная рота |
| `PLATOON_COMMANDER` | назначенный взвод |
| `SQUAD_COMMANDER` | назначенное отделение |
| `SOLDIER` | собственная карточка и chain mode |

Backend checks:

```text
GET roots -> returns only assigned roots.
GET children -> returns only children inside scope.
GET focus -> allowed only if selected node is in scope.
GET chain -> allowed if personnel is in scope or personnelId equals current soldierId.
GET passport -> allowed only if node is in scope.
GET context -> allowed only if node is in scope.
```

Frontend behavior:

- недоступные nodes не запрашиваются;
- недоступные actions не показываются;
- дерево не содержит скрытых объектов;
- Passport Card получает только разрешенные данные.

## 6. Visual design

Focus Tree оформляется простыми карточками и списками.

### Node card

Node card содержит:

- icon по типу node;
- label;
- subtitle;
- status badge;
- маленькие metrics;
- expand icon;
- alert marker.

Visual states:

| State | Style |
|---|---|
| default | `bg-zinc-950 border-zinc-800` |
| hover | `bg-zinc-900` |
| selected | `border-emerald-500 bg-emerald-500/10` |
| alert | right accent `border-r-red-500` |
| loading | skeleton shimmer |
| disabled | opacity 50 |

Icons:

| Node type | Lucide icon |
|---|---|
| District | `Shield` |
| Army / Formation | `Network` |
| Military unit | `Landmark` |
| Company / Platoon / Squad | `GitBranch` |
| Personnel | `User` |

### Strategic Tree visual

Strategic mode uses compact cards:

```text
[Округ]
  [Армия] [Армия] [Армия]
    [Корпус] [Дивизия] [Бригада]
```

Для реализации это остается вертикальным списком с indentation, без сложных canvas/SVG.

### Focus Mode visual

Focus Mode показывает:

- breadcrumb path сверху;
- selected branch;
- siblings as small cards;
- children as expandable rows.

```text
Западный военный округ / 1-я армия / 7-я бригада

Selected:
[7-я бригада] readiness 82 alert 1

Children:
  [1-й танковый полк]
  [2-я мотострелковая часть]
```

### Chain Mode visual

Chain Mode показывает вертикальную timeline:

```text
Иванов Петр Сергеевич
  │
1-е отделение
  │
1-й взвод
  │
1-я рота
  │
1-й танковый полк
```

Timeline можно реализовать обычными div-линиями и Framer Motion.

## 7. Passport Card integration

Выбор node запускает две загрузки:

```text
selectedNode changes
  -> GET /api/focus-tree/nodes/{type}/{id}/passport
  -> GET /api/focus-tree/nodes/{type}/{id}/context
```

Hook:

```ts
export function useSelectedNodeData() {
  const selectedNode = useFocusTreeStore((state) => state.selectedNode)

  const passport = useQuery({
    queryKey: selectedNode
      ? focusTreeKeys.passport(selectedNode.id)
      : ["focus-tree", "passport", "none"],
    queryFn: () => focusTreeApi.passport(selectedNode!.type, selectedNode!.objectId),
    enabled: Boolean(selectedNode),
  })

  const context = useQuery({
    queryKey: selectedNode
      ? focusTreeKeys.context(selectedNode.id)
      : ["focus-tree", "context", "none"],
    queryFn: () => focusTreeApi.context(selectedNode!.type, selectedNode!.objectId),
    enabled: Boolean(selectedNode),
  })

  return { selectedNode, passport, context }
}
```

Passport Card structure:

```text
Header
  title
  type
  status
  readiness

Attributes
  commander
  location
  parent
  created date

Metrics
  personnel
  equipment
  weapons
  alerts

Relations
  parent
  children
  commander
```

Component:

```tsx
export function NodePassportCard() {
  const { selectedNode, passport } = useSelectedNodeData()

  if (!selectedNode) {
    return <EmptyState title="Select structure node" />
  }

  if (passport.isLoading) {
    return <PassportSkeleton />
  }

  const data = passport.data

  return (
    <section className="rounded-md border border-zinc-800 bg-zinc-950 p-5">
      <div className="flex items-start justify-between">
        <div>
          <div className="text-xs uppercase text-zinc-500">{data.type}</div>
          <h2 className="mt-1 text-xl font-semibold text-zinc-100">{data.title}</h2>
          <p className="text-sm text-zinc-400">{data.subtitle}</p>
        </div>
        <StatusBadge status={data.status} />
      </div>

      <MetricsGrid metrics={data.metrics} />
      <AttributesList attributes={data.attributes} />
    </section>
  )
}
```

## 8. Fallback для больших данных

Large-data safeguards:

### Lazy-load

Дети node загружаются только при раскрытии.

### Children limit

Backend возвращает первые N children и признак `hasMore`.

```http
GET /api/focus-tree/nodes/MILITARY_UNIT/12/children?limit=50&cursor=...
```

Response:

```json
{
  "items": [],
  "nextCursor": "eyJvZmZzZXQiOjUwfQ==",
  "hasMore": true
}
```

### Search instead of full expansion

Если у node слишком много children, UI показывает:

```text
This node contains 240 personnel records.
Use search or filters to narrow results.
```

### Virtualized list

Для списков больше 200 nodes используется virtualization:

```text
@tanstack/react-virtual
```

### Focus Mode default

При открытии большой области UI автоматически показывает:

- selected node;
- parent path;
- first-level children;
- search input;
- filters by type/status.

Он не раскрывает все уровни сразу.

### Server-side search

Search endpoint всегда scoped и paginated:

```http
GET /api/focus-tree/search?query=иванов&page=0&size=20
```

## 9. React components

### Page

```text
HierarchyPage
```

### Widgets

```text
FocusTreeLayout
TreeModeTabs
StrategicTree
FocusBranchView
PersonnelChainView
NodePassportCard
NodeContextPanel
```

### Features

```text
FocusTreeSearch
TreeNodeCard
TreeNodeChildren
TreeBreadcrumbs
NodeActions
NodeAlerts
NodeStatistics
```

### Shared

```text
StatusBadge
ScopeBadge
MetricPill
SkeletonRow
EmptyState
```

## Component composition

```tsx
export function HierarchyPage() {
  return (
    <FocusTreeLayout
      left={<FocusTreePanel />}
      center={<NodePassportCard />}
      right={<NodeContextPanel />}
    />
  )
}
```

```tsx
export function FocusTreePanel() {
  const mode = useFocusTreeStore((state) => state.mode)

  return (
    <div className="space-y-4">
      <TreeModeTabs />
      <FocusTreeSearch />
      {mode === "STRATEGIC" && <StrategicTree />}
      {mode === "FOCUS" && <FocusBranchView />}
      {mode === "CHAIN" && <PersonnelChainView />}
    </div>
  )
}
```

Tree node:

```tsx
export function TreeNodeCard({ node }: { node: TreeNode }) {
  const selectedNode = useFocusTreeStore((state) => state.selectedNode)
  const selectNode = useFocusTreeStore((state) => state.selectNode)
  const isSelected = selectedNode?.id === node.id

  return (
    <motion.button
      type="button"
      onClick={() => selectNode(node)}
      className={cn(
        "flex w-full items-center gap-3 rounded-md border p-3 text-left",
        "border-zinc-800 bg-zinc-950 hover:bg-zinc-900",
        isSelected && "border-emerald-500 bg-emerald-500/10",
      )}
      initial={{ opacity: 0, y: 4 }}
      animate={{ opacity: 1, y: 0 }}
    >
      <NodeTypeIcon type={node.type} />
      <div className="min-w-0 flex-1">
        <div className="truncate text-sm font-medium text-zinc-100">{node.label}</div>
        <div className="truncate text-xs text-zinc-500">{node.subtitle}</div>
      </div>
      {node.metrics.alertCount > 0 && (
        <Badge variant="destructive">{node.metrics.alertCount}</Badge>
      )}
    </motion.button>
  )
}
```

Context panel:

```tsx
export function NodeContextPanel() {
  const { selectedNode, context } = useSelectedNodeData()

  if (!selectedNode) {
    return <EmptyState title="No node selected" />
  }

  return (
    <aside className="space-y-4">
      <NodeActions actions={context.data?.actions ?? []} />
      <NodeAlerts alerts={context.data?.alerts ?? []} />
      <NodeStatistics statistics={context.data?.statistics ?? []} />
    </aside>
  )
}
```

## Backend service outline

```java
public interface FocusTreeService {

    List<TreeNodeDto> roots(TreeMode mode);

    CursorPage<TreeNodeDto> children(TreeNodeType nodeType, Long nodeId, CursorRequest cursor);

    FocusTreeResponse focus(TreeNodeType nodeType, Long nodeId);

    PersonnelChainResponse chain(Long personnelId);

    NodePassportDto passport(TreeNodeType nodeType, Long nodeId);

    NodeContextDto context(TreeNodeType nodeType, Long nodeId);

    Page<TreeNodeDto> search(FocusTreeSearchRequest request, Pageable pageable);
}
```

Controller:

```java
@RestController
@RequestMapping("/api/focus-tree")
public class FocusTreeController {

    private final FocusTreeService focusTreeService;

    @GetMapping("/roots")
    public List<TreeNodeDto> roots(@RequestParam TreeMode mode) {
        return focusTreeService.roots(mode);
    }

    @GetMapping("/nodes/{type}/{id}/children")
    public CursorPage<TreeNodeDto> children(
            @PathVariable TreeNodeType type,
            @PathVariable Long id,
            CursorRequest cursor
    ) {
        return focusTreeService.children(type, id, cursor);
    }

    @GetMapping("/focus")
    public FocusTreeResponse focus(
            @RequestParam TreeNodeType nodeType,
            @RequestParam Long nodeId
    ) {
        return focusTreeService.focus(nodeType, nodeId);
    }

    @GetMapping("/personnel/{personnelId}/chain")
    public PersonnelChainResponse chain(@PathVariable Long personnelId) {
        return focusTreeService.chain(personnelId);
    }

    @GetMapping("/nodes/{type}/{id}/passport")
    public NodePassportDto passport(@PathVariable TreeNodeType type, @PathVariable Long id) {
        return focusTreeService.passport(type, id);
    }

    @GetMapping("/nodes/{type}/{id}/context")
    public NodeContextDto context(@PathVariable TreeNodeType type, @PathVariable Long id) {
        return focusTreeService.context(type, id);
    }
}
```

## SQL strategy

Strategic roots:

```sql
SELECT
    mf.formation_id,
    mf.name,
    mf.formation_type,
    mf.status
FROM military_formations mf
WHERE mf.parent_id IS NULL;
```

Formation children:

```sql
SELECT
    formation_id,
    name,
    formation_type,
    status
FROM military_formations
WHERE parent_id = :formationId
ORDER BY formation_type, name;
```

Units under formation:

```sql
SELECT
    unit_id,
    name,
    commander_id
FROM military_units
WHERE formation_id = :formationId
ORDER BY name;
```

Subdivisions under unit:

```sql
SELECT
    subdivision_id,
    name,
    type,
    commander_id
FROM subdivisions
WHERE unit_id = :unitId
  AND parent_id IS NULL
ORDER BY type, name;
```

Subdivisions under subdivision:

```sql
SELECT
    subdivision_id,
    name,
    type,
    commander_id
FROM subdivisions
WHERE parent_id = :subdivisionId
ORDER BY type, name;
```

Personnel chain uses the same logic as `selects/task4.sql`, with `personnel_id` passed as a named parameter.

## Interaction rules

```text
Single click node -> select node and load passport/context.
Expand icon -> lazy-load children.
Double click node -> switch to Focus Mode for node.
Search result click -> select node and open Focus Mode.
Personnel search result -> open Chain Mode.
Breadcrumb click -> focus selected ancestor.
```

## Summary

Focus Tree uses simple building blocks:

- scoped REST endpoints;
- stable node DTO;
- lazy-loaded children;
- Zustand selected node;
- TanStack Query caching;
- three visual modes;
- Passport Card in the center;
- actions, alerts and statistics on the right.

The result is an effective command navigation surface without rendering an oversized full tree.

