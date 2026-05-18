import { Terminal } from "lucide-react"
import { Card } from "@/shared/ui/card"

type QueryPreviewTerminalProps = {
  command: string
}

export function QueryPreviewTerminal({ command }: QueryPreviewTerminalProps) {
  return (
    <Card className="border-emerald-950 bg-black">
      <div className="flex items-center gap-2 text-xs uppercase text-emerald-300">
        <Terminal className="size-4" />
        Query Preview
      </div>
      <pre className="mt-3 overflow-x-auto rounded-md bg-emerald-950/20 p-4 text-sm text-emerald-200">{command}</pre>
    </Card>
  )
}
