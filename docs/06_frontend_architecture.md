# TACTICAL DISTRICT COMMAND: frontend architecture

## Назначение

Документ описывает frontend architecture для **TACTICAL DISTRICT COMMAND** на React 19+, TypeScript и Vite.

Frontend предоставляет tactical command center UI для:

- аутентификации;
- dashboard;
- иерархии округа;
- паспортов военных частей;
- личного состава;
- Intelligence Query Terminal;
- alert center;
- отчетов;
- администрирования пользователей и ролей.

Backend остается единственным enforcement layer для прав доступа. Frontend использует роли и permissions для маршрутизации, отображения разделов и скрытия недоступных действий.

## Stack

- React 19+
- TypeScript
- Vite
- Tailwind CSS
- shadcn/ui
- TanStack Query
- Zustand
- TanStack Table
- React Hook Form
- Zod
- Recharts
- Framer Motion
- Lucide Icons

## 1. Структура папок

Используется Feature-Sliced Design с практичным упрощением под dashboard-приложение.

```text
frontend/src/
  app/
    App.tsx
    router.tsx
    providers.tsx
    queryClient.ts
    styles.css

  pages/
    login/
      LoginPage.tsx
    dashboard/
      DashboardPage.tsx
    hierarchy/
      HierarchyPage.tsx
    unit-passport/
      UnitPassportPage.tsx
    personnel/
      PersonnelPage.tsx
    intelligence-terminal/
      IntelligenceTerminalPage.tsx
    alerts/
      AlertsPage.tsx
    reports/
      ReportsPage.tsx
    admin-users/
      AdminUsersPage.tsx

  widgets/
    layout/
      TacticalLayout.tsx
      Sidebar.tsx
      Topbar.tsx
      ContentArea.tsx
    dashboard/
      ReadinessRadar.tsx
      ResourceStatusChart.tsx
      AlertSummaryPanel.tsx
      CommandMetrics.tsx
    hierarchy/
      HierarchyTree.tsx
      StructureDetailsPanel.tsx
    intelligence/
      QueryBuilder.tsx
      QueryTemplateSelector.tsx
      QueryParamsForm.tsx
      QueryPreviewTerminal.tsx
      QueryResultTable.tsx

  features/
    auth/
      api/
        authApi.ts
        authQueries.ts
      model/
        authStore.ts
        authTypes.ts
      ui/
        LoginForm.tsx
    permissions/
      model/
        permissionUtils.ts
        permissionTypes.ts
      ui/
        PermissionGuard.tsx
    personnel/
      api/
        personnelApi.ts
        personnelQueries.ts
      model/
        personnelSchema.ts
        personnelTypes.ts
      ui/
        PersonnelForm.tsx
        PersonnelTable.tsx
    units/
      api/
        unitApi.ts
        unitQueries.ts
      model/
        unitTypes.ts
      ui/
        UnitSummaryCard.tsx
        UnitEquipmentPanel.tsx
    hierarchy/
      api/
        hierarchyApi.ts
        hierarchyQueries.ts
      model/
        hierarchyTypes.ts
    intelligence/
      api/
        intelligenceApi.ts
        intelligenceQueries.ts
      model/
        intelligenceTypes.ts
        queryBuilderStore.ts
    alerts/
      api/
        alertApi.ts
        alertQueries.ts
      model/
        alertTypes.ts
    reports/
      api/
        reportApi.ts
        reportQueries.ts
      model/
        reportTypes.ts
    admin-users/
      api/
        userApi.ts
        userQueries.ts
      model/
        userTypes.ts
        userSchema.ts

  entities/
    user/
      types.ts
    personnel/
      types.ts
    unit/
      types.ts
    hierarchy/
      types.ts
    equipment/
      types.ts
    weapon/
      types.ts
    alert/
      types.ts

  shared/
    api/
      apiClient.ts
      apiError.ts
      endpoints.ts
    config/
      routes.ts
      navigation.ts
    lib/
      cn.ts
      date.ts
      format.ts
      storage.ts
    ui/
      badge.tsx
      button.tsx
      card.tsx
      dialog.tsx
      dropdown-menu.tsx
      form.tsx
      input.tsx
      select.tsx
      table.tsx
      tabs.tsx
      toast.tsx
    types/
      api.ts
      pagination.ts
```

## 2. Feature-Sliced Design

Слои:

| Layer | Назначение |
|---|---|
| `app` | providers, router, глобальные стили |
| `pages` | route-level страницы |
| `widgets` | крупные самостоятельные блоки интерфейса |
| `features` | пользовательские действия и бизнес-функции |
| `entities` | доменные типы и простые entity-компоненты |
| `shared` | UI primitives, API client, утилиты, базовые типы |

Правила зависимостей:

```text
app -> pages -> widgets -> features -> entities -> shared
```

Нижние слои не импортируют верхние. Например, `shared/ui/button.tsx` не знает о `features/auth`.

## 3. Routing

Используется `react-router`.

```tsx
import { createBrowserRouter, Navigate } from "react-router"
import { TacticalLayout } from "@/widgets/layout/TacticalLayout"
import { ProtectedRoute } from "@/features/auth/ui/ProtectedRoute"
import { LoginPage } from "@/pages/login/LoginPage"
import { DashboardPage } from "@/pages/dashboard/DashboardPage"
import { HierarchyPage } from "@/pages/hierarchy/HierarchyPage"
import { UnitPassportPage } from "@/pages/unit-passport/UnitPassportPage"
import { PersonnelPage } from "@/pages/personnel/PersonnelPage"
import { IntelligenceTerminalPage } from "@/pages/intelligence-terminal/IntelligenceTerminalPage"
import { AlertsPage } from "@/pages/alerts/AlertsPage"
import { ReportsPage } from "@/pages/reports/ReportsPage"
import { AdminUsersPage } from "@/pages/admin-users/AdminUsersPage"

export const router = createBrowserRouter([
  {
    path: "/login",
    element: <LoginPage />,
  },
  {
    path: "/",
    element: (
      <ProtectedRoute>
        <TacticalLayout />
      </ProtectedRoute>
    ),
    children: [
      { index: true, element: <Navigate to="/dashboard" replace /> },
      { path: "dashboard", element: <DashboardPage /> },
      { path: "hierarchy", element: <HierarchyPage /> },
      { path: "units/:unitId", element: <UnitPassportPage /> },
      { path: "personnel", element: <PersonnelPage /> },
      { path: "intelligence", element: <IntelligenceTerminalPage /> },
      { path: "alerts", element: <AlertsPage /> },
      { path: "reports", element: <ReportsPage /> },
      { path: "admin/users", element: <AdminUsersPage /> },
    ],
  },
])
```

Route metadata хранится отдельно:

```ts
export const routes = {
  dashboard: "/dashboard",
  hierarchy: "/hierarchy",
  personnel: "/personnel",
  intelligence: "/intelligence",
  alerts: "/alerts",
  reports: "/reports",
  adminUsers: "/admin/users",
} as const
```

Navigation config:

```ts
import {
  Activity,
  AlertTriangle,
  Binary,
  FileText,
  Network,
  Shield,
  Users,
} from "lucide-react"

export const navigationItems = [
  {
    label: "Dashboard",
    path: routes.dashboard,
    icon: Activity,
    permissions: ["dashboard:read"],
  },
  {
    label: "Hierarchy",
    path: routes.hierarchy,
    icon: Network,
    permissions: ["structure:read"],
  },
  {
    label: "Personnel",
    path: routes.personnel,
    icon: Users,
    permissions: ["personnel:read"],
  },
  {
    label: "Intelligence",
    path: routes.intelligence,
    icon: Binary,
    permissions: ["query:execute"],
  },
  {
    label: "Alerts",
    path: routes.alerts,
    icon: AlertTriangle,
    permissions: ["alert:read"],
  },
  {
    label: "Reports",
    path: routes.reports,
    icon: FileText,
    permissions: ["report:read"],
  },
  {
    label: "Users",
    path: routes.adminUsers,
    icon: Shield,
    permissions: ["user:manage"],
  },
]
```

## 4. Auth store

Zustand хранит только session state. Серверная информация загружается через TanStack Query.

```ts
import { create } from "zustand"
import { persist } from "zustand/middleware"

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  setTokens: (tokens: { accessToken: string; refreshToken: string }) => void
  clearTokens: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      setTokens: ({ accessToken, refreshToken }) =>
        set({ accessToken, refreshToken }),
      clearTokens: () => set({ accessToken: null, refreshToken: null }),
    }),
    {
      name: "tdc-auth",
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
      }),
    },
  ),
)
```

Current user берется из API:

```ts
export function useCurrentUserQuery() {
  return useQuery({
    queryKey: ["auth", "me"],
    queryFn: authApi.me,
    enabled: Boolean(useAuthStore.getState().accessToken),
  })
}
```

## 5. API client

API client отвечает за:

- base URL;
- JWT header;
- JSON parsing;
- refresh flow;
- нормализацию ошибок.

```ts
import { useAuthStore } from "@/features/auth/model/authStore"

const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080"

type RequestOptions = RequestInit & {
  skipAuth?: boolean
}

export async function apiClient<T>(
  path: string,
  options: RequestOptions = {},
): Promise<T> {
  const { accessToken } = useAuthStore.getState()

  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(accessToken && !options.skipAuth
        ? { Authorization: `Bearer ${accessToken}` }
        : {}),
      ...options.headers,
    },
  })

  if (response.status === 401) {
    useAuthStore.getState().clearTokens()
    throw new ApiError(401, "UNAUTHORIZED", "Authentication is required")
  }

  if (!response.ok) {
    const error = await response.json().catch(() => null)
    throw ApiError.fromResponse(response.status, error)
  }

  if (response.status === 204) {
    return undefined as T
  }

  return response.json() as Promise<T>
}
```

Module API:

```ts
export const personnelApi = {
  search: (params: PersonnelSearchParams) =>
    apiClient<PageResponse<Personnel>>(`/api/personnel?${toQueryString(params)}`),

  getById: (id: number) =>
    apiClient<Personnel>(`/api/personnel/${id}`),

  create: (payload: CreatePersonnelRequest) =>
    apiClient<Personnel>("/api/personnel", {
      method: "POST",
      body: JSON.stringify(payload),
    }),

  update: (id: number, payload: UpdatePersonnelRequest) =>
    apiClient<Personnel>(`/api/personnel/${id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    }),
}
```

## 6. TanStack Query hooks

Query keys выносятся рядом с API модулями.

```ts
export const personnelKeys = {
  all: ["personnel"] as const,
  list: (params: PersonnelSearchParams) =>
    [...personnelKeys.all, "list", params] as const,
  detail: (id: number) => [...personnelKeys.all, "detail", id] as const,
}
```

List query:

```ts
export function usePersonnelQuery(params: PersonnelSearchParams) {
  return useQuery({
    queryKey: personnelKeys.list(params),
    queryFn: () => personnelApi.search(params),
    placeholderData: keepPreviousData,
  })
}
```

Mutation:

```ts
export function useCreatePersonnelMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: personnelApi.create,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: personnelKeys.all })
    },
  })
}
```

Intelligence query:

```ts
export function useExecuteQueryMutation() {
  return useMutation({
    mutationFn: ({
      code,
      payload,
    }: {
      code: string
      payload: QueryExecutionRequest
    }) => intelligenceApi.execute(code, payload),
  })
}
```

## 7. PermissionGuard

`PermissionGuard` скрывает элементы UI, если у пользователя нет required permissions.

```tsx
type PermissionGuardProps = {
  permissions?: string[]
  roles?: RoleCode[]
  mode?: "all" | "any"
  fallback?: React.ReactNode
  children: React.ReactNode
}

export function PermissionGuard({
  permissions = [],
  roles = [],
  mode = "all",
  fallback = null,
  children,
}: PermissionGuardProps) {
  const { data: user } = useCurrentUserQuery()

  if (!user) {
    return fallback
  }

  const hasPermissions =
    mode === "all"
      ? permissions.every((permission) => user.permissions.includes(permission))
      : permissions.some((permission) => user.permissions.includes(permission))

  const hasRoles =
    roles.length === 0 || roles.some((role) => user.roles.includes(role))

  if (!hasPermissions || !hasRoles) {
    return fallback
  }

  return children
}
```

Usage:

```tsx
<PermissionGuard permissions={["personnel:create"]}>
  <Button>
    <Plus className="size-4" />
    Add personnel
  </Button>
</PermissionGuard>
```

## 8. Layout

### TacticalLayout

Root layout for authenticated pages.

```tsx
import { Outlet } from "react-router"
import { Sidebar } from "./Sidebar"
import { Topbar } from "./Topbar"
import { ContentArea } from "./ContentArea"

export function TacticalLayout() {
  return (
    <div className="min-h-screen bg-zinc-950 text-zinc-100">
      <Sidebar />
      <div className="min-h-screen pl-72">
        <Topbar />
        <ContentArea>
          <Outlet />
        </ContentArea>
      </div>
    </div>
  )
}
```

### Sidebar

Sidebar содержит:

- product mark;
- navigation;
- текущую роль;
- compact status block;
- logout action.

```tsx
export function Sidebar() {
  const { data: user } = useCurrentUserQuery()

  const visibleItems = navigationItems.filter((item) =>
    item.permissions.every((permission) => user?.permissions.includes(permission)),
  )

  return (
    <aside className="fixed inset-y-0 left-0 w-72 border-r border-zinc-800 bg-zinc-950">
      <div className="flex h-16 items-center border-b border-zinc-800 px-5">
        <Shield className="mr-3 size-5 text-emerald-400" />
        <div>
          <div className="text-sm font-semibold uppercase tracking-wide">
            Tactical District
          </div>
          <div className="text-xs text-zinc-500">Command Center</div>
        </div>
      </div>

      <nav className="space-y-1 p-3">
        {visibleItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              cn(
                "flex items-center gap-3 rounded-md px-3 py-2 text-sm text-zinc-400",
                "hover:bg-zinc-900 hover:text-zinc-100",
                isActive && "bg-zinc-900 text-emerald-300",
              )
            }
          >
            <item.icon className="size-4" />
            {item.label}
          </NavLink>
        ))}
      </nav>
    </aside>
  )
}
```

### Topbar

Topbar содержит:

- page title;
- global search;
- active scope;
- alert indicator;
- user menu.

```tsx
export function Topbar() {
  const { data: user } = useCurrentUserQuery()

  return (
    <header className="sticky top-0 z-20 flex h-16 items-center justify-between border-b border-zinc-800 bg-zinc-950/90 px-6 backdrop-blur">
      <div>
        <div className="text-xs uppercase text-zinc-500">Active command scope</div>
        <div className="text-sm font-medium text-zinc-200">
          {user?.assignments[0]?.objectType ?? "No scope"}
        </div>
      </div>

      <div className="flex items-center gap-3">
        <Button variant="ghost" size="icon">
          <Bell className="size-4" />
        </Button>
        <UserMenu user={user} />
      </div>
    </header>
  )
}
```

### Content area

```tsx
export function ContentArea({ children }: { children: React.ReactNode }) {
  return (
    <main className="mx-auto w-full max-w-[1600px] px-6 py-6">
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.18 }}
      >
        {children}
      </motion.div>
    </main>
  )
}
```

## 9. Основные страницы

### LoginPage

Состав:

- `LoginForm`;
- status panel;
- role hint после успешного входа;
- background в стиле command interface.

Функции:

- username/password login;
- validation через Zod;
- redirect на `/dashboard`;
- отображение ошибок `401`.

### DashboardPage

Состав:

- `CommandMetrics`;
- `ReadinessRadar`;
- `ResourceStatusChart`;
- `AlertSummaryPanel`;
- quick links to reports and intelligence.

Данные:

```http
GET /api/dashboard/summary
GET /api/dashboard/readiness
GET /api/alerts?status=OPEN&limit=5
```

### HierarchyPage

Состав:

- `HierarchyTree`;
- `StructureDetailsPanel`;
- search/filter;
- context actions based on permissions.

Данные:

```http
GET /api/hierarchy/tree
GET /api/formations
GET /api/subdivisions
```

### UnitPassportPage

Состав:

- unit identity header;
- commander block;
- location block;
- equipment panel;
- weapon panel;
- buildings panel;
- personnel summary.

Route:

```text
/units/:unitId
```

### PersonnelPage

Состав:

- `PersonnelTable`;
- filters;
- create/edit dialog;
- rank and specialty chips;
- scoped data only.

CRUD pattern:

```text
Table -> Toolbar -> Filters -> Row actions -> Dialog form -> Mutation -> Invalidate query
```

### IntelligenceTerminalPage

Состав:

- `QueryBuilder`;
- `QueryTemplateSelector`;
- `QueryParamsForm`;
- `QueryPreviewTerminal`;
- `QueryResultTable`;
- CSV export.

Данные:

```http
GET  /api/intelligence/queries
POST /api/intelligence/queries/{code}/execute
POST /api/intelligence/queries/{code}/export.csv
```

### AlertsPage

Состав:

- severity tabs;
- alert feed;
- detail drawer;
- acknowledge/resolve actions.

Данные:

```http
GET  /api/alerts
POST /api/alerts/{id}/acknowledge
POST /api/alerts/{id}/resolve
```

### ReportsPage

Состав:

- report catalog;
- report preview;
- filters;
- charts via Recharts;
- export actions.

### AdminUsersPage

Состав:

- users table;
- role assignment dialog;
- command assignment editor;
- active/inactive toggle.

Guard:

```tsx
<PermissionGuard permissions={["user:manage"]}>
  <AdminUsersPage />
</PermissionGuard>
```

## 10. Основные компоненты

### Data components

```text
DataTable
DataTableToolbar
DataTablePagination
ColumnVisibilityMenu
FilterBar
SearchInput
EmptyState
LoadingState
ErrorState
```

### Tactical components

```text
CommandMetricCard
ReadinessRadar
AlertSeverityBadge
StatusBadge
ScopeBadge
CommandBreadcrumbs
TerminalBlock
```

### Form components

```text
PersonnelForm
UnitForm
EquipmentQuantityForm
WeaponQuantityForm
UserRoleForm
CommandAssignmentForm
```

### Dialog / drawer components

```text
CreateEntityDialog
EditEntityDialog
ConfirmDeleteDialog
DetailsDrawer
AuditTimelineDrawer
```

## 11. TypeScript types

### Common API

```ts
export type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

export type ApiErrorResponse = {
  timestamp: string
  status: number
  code: string
  message: string
  path: string
  validationErrors: Record<string, string>
}
```

### Auth

```ts
export type RoleCode =
  | "ADMIN_DISTRICT"
  | "STAFF_ANALYST"
  | "ARMY_COMMANDER"
  | "FORMATION_COMMANDER"
  | "UNIT_COMMANDER"
  | "COMPANY_COMMANDER"
  | "PLATOON_COMMANDER"
  | "SQUAD_COMMANDER"
  | "SOLDIER"

export type CommandAssignment = {
  objectType: string
  objectId: number
}

export type CurrentUser = {
  userId: number
  soldierId: number | null
  username: string
  displayName: string
  roles: RoleCode[]
  assignments: CommandAssignment[]
  permissions: string[]
}
```

### Personnel

```ts
export type Personnel = {
  id: number
  lastName: string
  firstName: string
  middleName: string | null
  personalNumber: string
  birthDate: string
  serviceStart: string
  subdivisionId: number
  subdivisionName: string
  rankName: string | null
}

export type PersonnelSearchParams = {
  page?: number
  size?: number
  sort?: string
  search?: string
  unitId?: number
  subdivisionId?: number
  rank?: string
  specialty?: string
}
```

### Intelligence

```ts
export type QueryTemplate = {
  code: string
  label: string
  description: string
  parameters: QueryParameter[]
  requiredPermissions: string[]
  exampleCommand: string
}

export type QueryParameter = {
  name: string
  label: string
  type: "string" | "number" | "enum" | "scope" | "boolean"
  required: boolean
  optionsEndpoint?: string
}

export type QueryExecutionRequest = {
  scope: {
    type: string
    id: number
    name: string
  }
  parameters: Record<string, unknown>
  previewCommand: string
}

export type QueryResult = {
  code: string
  label: string
  previewCommand: string
  columns: string[]
  rows: Record<string, unknown>[]
  rowCount: number
}
```

## 12. UI patterns для CRUD

### List page pattern

```text
PageHeader
  title
  scope badge
  primary action

FilterBar
  search
  select filters
  reset filters

DataTable
  columns
  row actions
  pagination
```

### Create / edit pattern

```tsx
const form = useForm<CreatePersonnelInput>({
  resolver: zodResolver(createPersonnelSchema),
  defaultValues: {
    lastName: "",
    firstName: "",
    middleName: "",
    personalNumber: "",
  },
})
```

Zod schema:

```ts
export const createPersonnelSchema = z.object({
  lastName: z.string().min(1),
  firstName: z.string().min(1),
  middleName: z.string().optional(),
  personalNumber: z.string().min(1),
  birthDate: z.string().min(1),
  serviceStart: z.string().min(1),
  subdivisionId: z.number(),
})

export type CreatePersonnelInput = z.infer<typeof createPersonnelSchema>
```

Mutation submit:

```tsx
function PersonnelCreateDialog() {
  const createPersonnel = useCreatePersonnelMutation()
  const form = useForm<CreatePersonnelInput>({
    resolver: zodResolver(createPersonnelSchema),
  })

  function onSubmit(values: CreatePersonnelInput) {
    createPersonnel.mutate(values)
  }

  return (
    <Dialog>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Add personnel</DialogTitle>
        </DialogHeader>
        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
            <FormField
              control={form.control}
              name="lastName"
              render={({ field }) => (
                <FormItem>
                  <FormLabel>Last name</FormLabel>
                  <FormControl>
                    <Input {...field} />
                  </FormControl>
                  <FormMessage />
                </FormItem>
              )}
            />
            <Button type="submit" disabled={createPersonnel.isPending}>
              Save
            </Button>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}
```

### Delete pattern

Delete always uses confirmation dialog:

```tsx
<ConfirmDeleteDialog
  title="Delete personnel record"
  description="This action removes the selected personnel record."
  onConfirm={() => deletePersonnel.mutate(personnel.id)}
/>
```

### Table actions pattern

```tsx
const columns: ColumnDef<Personnel>[] = [
  {
    accessorKey: "lastName",
    header: "Last name",
  },
  {
    accessorKey: "rankName",
    header: "Rank",
    cell: ({ row }) => <Badge variant="secondary">{row.original.rankName}</Badge>,
  },
  {
    id: "actions",
    cell: ({ row }) => (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" size="icon">
            <MoreHorizontal className="size-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end">
          <DropdownMenuItem>Open</DropdownMenuItem>
          <PermissionGuard permissions={["personnel:update"]}>
            <DropdownMenuItem>Edit</DropdownMenuItem>
          </PermissionGuard>
        </DropdownMenuContent>
      </DropdownMenu>
    ),
  },
]
```

## Visual style

The interface uses a restrained tactical command center style:

- dark zinc background;
- emerald/cyan/yellow status accents;
- compact panels;
- high-density tables;
- clear severity badges;
- radar and resource charts;
- Lucide icons for navigation and actions;
- Framer Motion for small transitions only.

Color usage:

```text
Background: zinc-950 / zinc-900
Borders: zinc-800
Primary action: emerald-500
Information: cyan-400
Warning: amber-400
Critical: red-500
Muted text: zinc-500
Main text: zinc-100
```

The layout prioritizes scanning, comparison and repeated operational actions over decorative composition.

