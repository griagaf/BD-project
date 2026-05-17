import { useTranslation } from "react-i18next"
import { Card } from "@/shared/ui/card"

export function AdminUsersPage() {
  const { t } = useTranslation("admin")
  return <Card>{t("page.placeholder")}</Card>
}
