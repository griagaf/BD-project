import { Terminal } from "lucide-react"
import { useTranslation } from "react-i18next"
import { Card } from "@/shared/ui/card"

type QueryPreviewTerminalProps = {
  command: string
}

export function QueryPreviewTerminal({ command }: QueryPreviewTerminalProps) {
  const { t } = useTranslation("intelligence")

  return (
    <Card className="border-emerald-950 bg-black">
      <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
        <Terminal className="h-4 w-4 shrink-0" />
        {t("builder.queryPreview")}
      </div>
      <pre className="mt-3 overflow-x-auto rounded-md bg-emerald-950/20 p-4 text-sm text-emerald-200">{command}</pre>
    </Card>
  )
}
