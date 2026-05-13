export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string,
    public readonly validationErrors: Record<string, string> = {},
  ) {
    super(message)
  }

  static fromResponse(status: number, payload: unknown) {
    if (payload && typeof payload === "object") {
      const error = payload as {
        code?: string
        message?: string
        validationErrors?: Record<string, string>
      }

      return new ApiError(
        status,
        error.code ?? "API_ERROR",
        error.message ?? "Request failed",
        error.validationErrors ?? {},
      )
    }

    return new ApiError(status, "API_ERROR", "Request failed")
  }
}

