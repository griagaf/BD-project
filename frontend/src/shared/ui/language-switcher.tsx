import { Languages } from "lucide-react"
import { useTranslation } from "react-i18next"
import { supportedLanguages, type SupportedLanguage } from "@/shared/i18n"

export function LanguageSwitcher() {
  const { i18n, t } = useTranslation("common")

  function changeLanguage(language: SupportedLanguage) {
    void i18n.changeLanguage(language)
  }

  return (
    <label className="inline-flex h-9 items-center gap-2 rounded-md border border-zinc-800 bg-zinc-900/70 px-2 text-xs text-zinc-300">
      <Languages className="h-4 w-4 shrink-0 text-emerald-300" />
      <span className="sr-only">{t("language.label")}</span>
      <select
        value={i18n.language}
        onChange={(event) => changeLanguage(event.target.value as SupportedLanguage)}
        className="h-7 max-w-28 rounded bg-transparent text-xs text-zinc-100 outline-none"
        aria-label={t("language.label")}
      >
        {supportedLanguages.map((language) => (
          <option key={language} value={language} className="bg-zinc-950">
            {t(`language.${language}`)}
          </option>
        ))}
      </select>
    </label>
  )
}
