import type { QueryTemplateMetadata } from "@/features/intelligence/model/intelligenceTypes"
import { Card } from "@/shared/ui/card"
import { useTranslation } from "react-i18next"

type QueryTemplateSelectorProps = {
  templates: QueryTemplateMetadata[]
  selectedCode: string
  onSelect: (code: string) => void
}

export function QueryTemplateSelector({ templates, selectedCode, onSelect }: QueryTemplateSelectorProps) {
  const { t } = useTranslation("intelligence")

  return (
    <Card className="space-y-2">
      <div className="text-xs uppercase text-emerald-300">{t("builder.templates")}</div>
      <div className="space-y-2">
        {templates.map((template) => (
          <button
            key={template.code}
            type="button"
            className={`w-full rounded-md border px-3 py-2 text-left transition-colors ${
              selectedCode === template.code
                ? "border-emerald-500/60 bg-emerald-500/10"
                : "border-zinc-800 bg-zinc-950 hover:bg-zinc-900"
            }`}
            onClick={() => onSelect(template.code)}
          >
            <div className="text-sm font-medium text-zinc-100">{template.label}</div>
            <div className="mt-1 line-clamp-2 text-xs text-zinc-500">{template.description}</div>
          </button>
        ))}
      </div>
    </Card>
  )
}
