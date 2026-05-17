import { TFunction } from "i18next"

export function roleLabel(t: TFunction, role: string) {
  return t(`common:roles.${role}`, { defaultValue: role })
}

export function statusLabel(t: TFunction, status?: string | null) {
  if (!status) {
    return ""
  }
  return t(`common:status.${status.toUpperCase()}`, { defaultValue: status })
}
