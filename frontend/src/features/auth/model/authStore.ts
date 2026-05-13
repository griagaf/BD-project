import { create } from "zustand"
import { persist } from "zustand/middleware"

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  setTokens: (tokens: { accessToken: string; refreshToken: string }) => void
  clearTokens: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      setTokens: ({ accessToken, refreshToken }) => set({ accessToken, refreshToken }),
      clearTokens: () => set({ accessToken: null, refreshToken: null }),
    }),
    {
      name: "tdc-auth",
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
      }),
    },
  ),
)

