import i18n from "i18next"
import { initReactI18next } from "react-i18next"
import ruCommon from "@/shared/i18n/locales/ru/common.json"
import enCommon from "@/shared/i18n/locales/en/common.json"
import ruAuth from "@/shared/i18n/locales/ru/auth.json"
import enAuth from "@/shared/i18n/locales/en/auth.json"
import ruDashboard from "@/shared/i18n/locales/ru/dashboard.json"
import enDashboard from "@/shared/i18n/locales/en/dashboard.json"
import ruHierarchy from "@/shared/i18n/locales/ru/hierarchy.json"
import enHierarchy from "@/shared/i18n/locales/en/hierarchy.json"
import ruPersonnel from "@/shared/i18n/locales/ru/personnel.json"
import enPersonnel from "@/shared/i18n/locales/en/personnel.json"
import ruEquipment from "@/shared/i18n/locales/ru/equipment.json"
import enEquipment from "@/shared/i18n/locales/en/equipment.json"
import ruWeapons from "@/shared/i18n/locales/ru/weapons.json"
import enWeapons from "@/shared/i18n/locales/en/weapons.json"
import ruBuildings from "@/shared/i18n/locales/ru/buildings.json"
import enBuildings from "@/shared/i18n/locales/en/buildings.json"
import ruIntelligence from "@/shared/i18n/locales/ru/intelligence.json"
import enIntelligence from "@/shared/i18n/locales/en/intelligence.json"
import ruAlerts from "@/shared/i18n/locales/ru/alerts.json"
import enAlerts from "@/shared/i18n/locales/en/alerts.json"
import ruReports from "@/shared/i18n/locales/ru/reports.json"
import enReports from "@/shared/i18n/locales/en/reports.json"
import ruAdmin from "@/shared/i18n/locales/ru/admin.json"
import enAdmin from "@/shared/i18n/locales/en/admin.json"
import ruSecurity from "@/shared/i18n/locales/ru/security.json"
import enSecurity from "@/shared/i18n/locales/en/security.json"

export const supportedLanguages = ["ru", "en"] as const
export type SupportedLanguage = (typeof supportedLanguages)[number]

const savedLanguage = localStorage.getItem("tdc-language")
const initialLanguage: SupportedLanguage = savedLanguage === "en" || savedLanguage === "ru" ? savedLanguage : "ru"

void i18n
  .use(initReactI18next)
  .init({
    lng: initialLanguage,
    fallbackLng: "ru",
    defaultNS: "common",
    ns: [
      "common",
      "auth",
      "dashboard",
      "hierarchy",
      "personnel",
      "equipment",
      "weapons",
      "buildings",
      "intelligence",
      "alerts",
      "reports",
      "admin",
      "security",
    ],
    interpolation: {
      escapeValue: false,
    },
    resources: {
      ru: {
        common: ruCommon,
        auth: ruAuth,
        dashboard: ruDashboard,
        hierarchy: ruHierarchy,
        personnel: ruPersonnel,
        equipment: ruEquipment,
        weapons: ruWeapons,
        buildings: ruBuildings,
        intelligence: ruIntelligence,
        alerts: ruAlerts,
        reports: ruReports,
        admin: ruAdmin,
        security: ruSecurity,
      },
      en: {
        common: enCommon,
        auth: enAuth,
        dashboard: enDashboard,
        hierarchy: enHierarchy,
        personnel: enPersonnel,
        equipment: enEquipment,
        weapons: enWeapons,
        buildings: enBuildings,
        intelligence: enIntelligence,
        alerts: enAlerts,
        reports: enReports,
        admin: enAdmin,
        security: enSecurity,
      },
    },
  })

i18n.on("languageChanged", (language) => {
  localStorage.setItem("tdc-language", language)
})

export { i18n }
