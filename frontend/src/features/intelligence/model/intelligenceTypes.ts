export type QueryParameterMetadata = {
  name: string
  label: string
  type: "text" | "number" | "enum"
  required: boolean
  placeholder: string
  options: string[]
}

export type QueryTemplateMetadata = {
  code: string
  label: string
  description: string
  target: string
  parameters: QueryParameterMetadata[]
  requiredPermissions: string[]
  exampleCommand: string
}

export type QueryScope = {
  type: string
  id: number
  name?: string
}

export type ExecuteQueryRequest = {
  scope?: QueryScope
  parameters: Record<string, string | number>
  previewCommand: string
}

export type QueryResult = {
  code: string
  previewCommand: string
  columns: string[]
  rows: Record<string, unknown>[]
  rowCount: number
  executedAt: string
}
