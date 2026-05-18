import { Plus, Search, ShieldCheck, UserCog } from "lucide-react"
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query"
import { useEffect, useState } from "react"
import { useTranslation } from "react-i18next"
import { apiClient } from "@/shared/api/apiClient"
import { roleLabel, statusLabel } from "@/shared/i18n/labels"
import { Badge } from "@/shared/ui/badge"
import { Button } from "@/shared/ui/button"
import { Card } from "@/shared/ui/card"
import { PageHeader } from "@/shared/ui/page"
import { Pagination } from "@/shared/ui/pagination"
import { Table, TableShell, tableCellClass, tableHeadClass, tableRowClass } from "@/shared/ui/table"
import { EmptyState, ErrorState, LoadingState } from "@/shared/ui/state"
import { toast } from "@/shared/ui/toast"
import { cn } from "@/shared/lib/cn"

type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  first: boolean
  last: boolean
}

type AdminUser = {
  id: number
  username: string
  displayName: string
  personnelId: number | null
  active: boolean
  roles: string[]
  createdAt: string
  updatedAt: string
}

type Role = {
  id: number
  code: string
  name: string
  description: string | null
}

type UserForm = {
  username: string
  displayName: string
  personnelId: string
  password: string
  roles: string[]
  active: boolean
}

const initialForm: UserForm = {
  username: "",
  displayName: "",
  personnelId: "",
  password: "",
  roles: ["SOLDIER"],
  active: true,
}

export function AdminUsersPage() {
  const { t } = useTranslation(["admin", "common"])
  const queryClient = useQueryClient()
  const [search, setSearch] = useState("")
  const [active, setActive] = useState<string>("")
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(10)
  const [editing, setEditing] = useState<AdminUser | null>(null)
  const [dialogOpen, setDialogOpen] = useState(false)

  const usersQuery = useQuery({
    queryKey: ["admin-users", search, active, page, size],
    queryFn: () => apiClient<PageResponse<AdminUser>>(`/api/users${queryString({ search, active, page, size })}`),
  })
  const rolesQuery = useQuery({
    queryKey: ["admin-roles"],
    queryFn: () => apiClient<Role[]>("/api/users/roles"),
  })
  const saveMutation = useMutation({
    mutationFn: (form: UserForm) => saveUser(editing, form),
    onSuccess: () => {
      toast.success(editing ? t("admin:toast.updated") : t("admin:toast.created"))
      setDialogOpen(false)
      setEditing(null)
      queryClient.invalidateQueries({ queryKey: ["admin-users"] })
    },
    onError: () => toast.error(t("admin:toast.saveFailed")),
  })
  const statusMutation = useMutation({
    mutationFn: (user: AdminUser) => apiClient<AdminUser>(`/api/users/${user.id}/${user.active ? "deactivate" : "activate"}`, { method: "POST" }),
    onSuccess: () => {
      toast.success(t("admin:toast.statusChanged"))
      queryClient.invalidateQueries({ queryKey: ["admin-users"] })
    },
    onError: () => toast.error(t("admin:toast.statusFailed")),
  })

  const users = usersQuery.data

  return (
    <div className="space-y-5">
      <PageHeader
        icon={UserCog}
        eyebrow={t("admin:page.eyebrow")}
        title={t("admin:page.title")}
        description={t("admin:page.description")}
        actions={
          <Button type="button" onClick={() => {
            setEditing(null)
            setDialogOpen(true)
          }}>
            <Plus className="h-4 w-4 shrink-0" />
            {t("actions.create")}
          </Button>
        }
      />

      <Card className="grid gap-3 md:grid-cols-[minmax(0,1fr)_220px_auto]">
        <label className="relative">
          <Search className="pointer-events-none absolute left-3 top-1/2 h-4 w-4 shrink-0 -translate-y-1/2 text-zinc-600" />
          <input
            value={search}
            onChange={(event) => {
              setSearch(event.target.value)
              setPage(0)
            }}
            placeholder={t("admin:filters.search")}
            className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-950 pl-9 pr-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
          />
        </label>
        <select
          value={active}
          onChange={(event) => {
            setActive(event.target.value)
            setPage(0)
          }}
          className="h-10 rounded-md border border-zinc-800 bg-zinc-950 px-3 text-sm text-zinc-100 outline-none focus:border-emerald-500"
        >
          <option value="">{t("admin:filters.allStatuses")}</option>
          <option value="true">{t("admin:filters.active")}</option>
          <option value="false">{t("admin:filters.inactive")}</option>
        </select>
        <Button type="button" variant="secondary" onClick={() => {
          setSearch("")
          setActive("")
          setPage(0)
        }}>
          {t("actions.reset")}
        </Button>
      </Card>

      {usersQuery.isLoading ? <LoadingState /> : null}
      {usersQuery.error ? <ErrorState title={t("admin:error")} /> : null}
      {users && !users.content.length ? <EmptyState title={t("admin:table.emptyTitle")} description={t("admin:table.emptyDescription")} /> : null}
      {users?.content.length ? (
        <>
          <TableShell>
            <Table>
              <thead className={tableHeadClass}>
                <tr>
                  <th className={tableCellClass}>{t("admin:table.user")}</th>
                  <th className={tableCellClass}>{t("admin:table.personnelId")}</th>
                  <th className={tableCellClass}>{t("admin:table.roles")}</th>
                  <th className={tableCellClass}>{t("table.status")}</th>
                  <th className={tableCellClass}>{t("admin:table.created")}</th>
                  <th className={cn(tableCellClass, "text-right")}>{t("table.actions")}</th>
                </tr>
              </thead>
              <tbody>
                {users.content.map((user) => (
                  <tr key={user.id} className={tableRowClass}>
                    <td className={tableCellClass}>
                      <div className="max-w-64 truncate font-medium text-zinc-100" title={user.displayName}>{user.displayName}</div>
                      <div className="max-w-64 truncate text-xs text-zinc-500" title={user.username}>{user.username}</div>
                    </td>
                    <td className={cn(tableCellClass, "text-zinc-400")}>{user.personnelId ?? t("states.notAvailable")}</td>
                    <td className={tableCellClass}>
                      <div className="flex max-w-80 flex-wrap gap-1">
                        {user.roles.map((role) => (
                          <Badge key={role} title={roleLabel(t, role)} className="max-w-full truncate">{roleLabel(t, role)}</Badge>
                        ))}
                      </div>
                    </td>
                    <td className={tableCellClass}>
                      <Badge variant={user.active ? "success" : "warning"}>{user.active ? statusLabel(t, "ACTIVE") : t("admin:filters.inactive")}</Badge>
                    </td>
                    <td className={cn(tableCellClass, "whitespace-nowrap text-zinc-500")}>{new Date(user.createdAt).toLocaleDateString()}</td>
                    <td className={tableCellClass}>
                      <div className="flex justify-end gap-2">
                        <Button type="button" variant="ghost" size="sm" onClick={() => {
                          setEditing(user)
                          setDialogOpen(true)
                        }}>
                          {t("actions.edit")}
                        </Button>
                        <Button type="button" variant={user.active ? "danger" : "secondary"} size="sm" disabled={statusMutation.isPending} onClick={() => statusMutation.mutate(user)}>
                          {user.active ? t("admin:actions.deactivate") : t("admin:actions.activate")}
                        </Button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </Table>
          </TableShell>
          <Pagination
            page={users.page}
            size={users.size}
            totalElements={users.totalElements}
            totalPages={users.totalPages}
            onPageChange={setPage}
            onSizeChange={(nextSize) => {
              setSize(nextSize)
              setPage(0)
            }}
          />
        </>
      ) : null}

      <UserDialog
        open={dialogOpen}
        user={editing}
        roles={rolesQuery.data ?? []}
        saving={saveMutation.isPending}
        onClose={() => {
          setDialogOpen(false)
          setEditing(null)
        }}
        onSubmit={(form) => saveMutation.mutate(form)}
      />
    </div>
  )
}

function UserDialog({ open, user, roles, saving, onClose, onSubmit }: {
  open: boolean
  user: AdminUser | null
  roles: Role[]
  saving: boolean
  onClose: () => void
  onSubmit: (form: UserForm) => void
}) {
  const { t } = useTranslation(["admin", "common"])
  const [form, setForm] = useState<UserForm>(initialForm)

  useEffect(() => {
    if (!open) {
      return
    }
    setForm(user ? {
      username: user.username,
      displayName: user.displayName,
      personnelId: user.personnelId ? String(user.personnelId) : "",
      password: "",
      roles: user.roles,
      active: user.active,
    } : initialForm)
  }, [open, user])

  if (!open) {
    return null
  }

  return (
    <div className="fixed inset-0 z-50 grid place-items-center bg-black/70 p-4">
      <form className="max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-md border border-zinc-800 bg-zinc-950 shadow-2xl" onSubmit={(event) => {
        event.preventDefault()
        onSubmit(form)
      }}>
        <div className="border-b border-zinc-800 px-5 py-4">
          <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
            <ShieldCheck className="h-4 w-4 shrink-0" />
            {user ? t("admin:form.edit") : t("admin:form.create")}
          </div>
        </div>
        <div className="grid gap-4 p-5 md:grid-cols-2">
          <Field label={t("admin:form.username")} value={form.username} disabled={Boolean(user)} onChange={(value) => setForm({ ...form, username: value })} />
          <Field label={t("admin:form.displayName")} value={form.displayName} onChange={(value) => setForm({ ...form, displayName: value })} />
          <Field label={t("admin:form.personnelId")} value={form.personnelId} type="number" optional onChange={(value) => setForm({ ...form, personnelId: value })} />
          <Field label={t("admin:form.password")} value={form.password} type="password" optional={Boolean(user)} hint={user ? t("admin:form.passwordHint") : undefined} onChange={(value) => setForm({ ...form, password: value })} />
          <label className="flex items-center gap-2 text-sm text-zinc-300 md:col-span-2">
            <input
              type="checkbox"
              checked={form.active}
              onChange={(event) => setForm({ ...form, active: event.target.checked })}
              className="h-4 w-4 shrink-0 accent-emerald-400"
            />
            {t("admin:form.active")}
          </label>
          <div className="space-y-2 md:col-span-2">
            <div className="text-xs uppercase text-zinc-500">{t("admin:form.roles")}</div>
            <div className="grid gap-2 rounded-md border border-zinc-800 bg-zinc-900 p-3 sm:grid-cols-2">
              {roles.map((role) => (
                <label key={role.code} className="flex min-w-0 items-center gap-2 text-sm text-zinc-300">
                  <input
                    type="checkbox"
                    checked={form.roles.includes(role.code)}
                    onChange={(event) => {
                      const nextRoles = event.target.checked
                        ? [...form.roles, role.code]
                        : form.roles.filter((item) => item !== role.code)
                      setForm({ ...form, roles: nextRoles })
                    }}
                    className="h-4 w-4 shrink-0 accent-emerald-400"
                  />
                  <span className="truncate" title={roleLabel(t, role.code)}>{roleLabel(t, role.code)}</span>
                </label>
              ))}
            </div>
          </div>
        </div>
        <div className="flex flex-col-reverse gap-2 border-t border-zinc-800 px-5 py-4 sm:flex-row sm:justify-end">
          <Button type="button" variant="ghost" onClick={onClose}>{t("actions.cancel")}</Button>
          <Button type="submit" disabled={saving || !form.username.trim() || !form.displayName.trim() || (!user && form.password.length < 8) || !form.roles.length}>
            {t("actions.save")}
          </Button>
        </div>
      </form>
    </div>
  )
}

function Field({ label, value, onChange, type = "text", disabled = false, optional = false, hint }: {
  label: string
  value: string
  onChange: (value: string) => void
  type?: string
  disabled?: boolean
  optional?: boolean
  hint?: string
}) {
  return (
    <label className="space-y-2">
      <span className="text-xs uppercase text-zinc-500">{label}</span>
      <input
        value={value}
        type={type}
        disabled={disabled}
        required={!optional}
        onChange={(event) => onChange(event.target.value)}
        className="h-10 w-full rounded-md border border-zinc-800 bg-zinc-900 px-3 text-sm text-zinc-100 outline-none disabled:cursor-not-allowed disabled:opacity-60"
      />
      {hint ? <span className="text-xs text-zinc-600">{hint}</span> : null}
    </label>
  )
}

function saveUser(user: AdminUser | null, form: UserForm) {
  const payload = {
    displayName: form.displayName.trim(),
    personnelId: form.personnelId ? Number(form.personnelId) : null,
    password: form.password || undefined,
    roles: form.roles,
    active: form.active,
  }
  if (user) {
    return apiClient<AdminUser>(`/api/users/${user.id}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    })
  }
  return apiClient<AdminUser>("/api/users", {
    method: "POST",
    body: JSON.stringify({
      username: form.username.trim(),
      ...payload,
      password: form.password,
    }),
  })
}

function queryString(filters: Record<string, string | number | boolean | undefined>) {
  const params = new URLSearchParams()
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && `${value}`.trim() !== "") {
      params.set(key, `${value}`)
    }
  })
  const query = params.toString()
  return query ? `?${query}` : ""
}
