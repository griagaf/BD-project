import type { ReactNode } from "react"

export function ContentArea({ children }: { children: ReactNode }) {
  return <main className="mx-auto w-full max-w-[1600px] px-6 py-6">{children}</main>
}

