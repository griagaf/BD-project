import type { ReactNode } from "react"
import { motion } from "framer-motion"

export function ContentArea({ children }: { children: ReactNode }) {
  return (
    <main className="mx-auto w-full max-w-[1600px] px-4 py-5 sm:px-6 lg:py-6">
      <motion.div initial={{ opacity: 0, y: 8 }} animate={{ opacity: 1, y: 0 }} transition={{ duration: 0.18 }}>
        {children}
      </motion.div>
    </main>
  )
}
