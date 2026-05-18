import { create } from "zustand"
import { persist } from "zustand/middleware"

type AuthState = {
  accessToken: string | null
  refreshToken: string | null
  simulationRole: string | null
  simulationObjectType: string | null
  simulationObjectId: number | null
  setTokens: (tokens: { accessToken: string; refreshToken: string }) => void
  clearTokens: () => void
  setSimulation: (simulation: { role: string; objectType: string; objectId: number }) => void
  clearSimulation: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      simulationRole: null,
      simulationObjectType: null,
      simulationObjectId: null,
      setTokens: ({ accessToken, refreshToken }) => set({ accessToken, refreshToken }),
      clearTokens: () => set({ accessToken: null, refreshToken: null, simulationRole: null, simulationObjectType: null, simulationObjectId: null }),
      setSimulation: ({ role, objectType, objectId }) => set({ simulationRole: role, simulationObjectType: objectType, simulationObjectId: objectId }),
      clearSimulation: () => set({ simulationRole: null, simulationObjectType: null, simulationObjectId: null }),
    }),
    {
      name: "tdc-auth",
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        simulationRole: state.simulationRole,
        simulationObjectType: state.simulationObjectType,
        simulationObjectId: state.simulationObjectId,
      }),
    },
  ),
)
