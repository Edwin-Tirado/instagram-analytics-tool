import { useState } from 'react'
import { adminGetIngestionRun, adminTriggerSync } from '@/lib/api'

// El backend responde de inmediato con el run en estado RUNNING (procesa la
// ingesta en segundo plano) — hacemos polling hasta que termine para poder
// mostrar el resultado final y refrescar lo que dependa de él.
const POLL_INTERVAL_MS = 2000

/**
 * Dispara la sincronización manual con Instagram y hace polling hasta que
 * termine. Compartido entre "Gestión de Eventos" y "Historial de Ingesta".
 */
export function useInstagramSync(onFinished?: () => void) {
  const [syncing, setSyncing] = useState(false)
  const [syncMsg, setSyncMsg] = useState<string | null>(null)
  const [syncError, setSyncError] = useState<string | null>(null)

  async function triggerSync() {
    setSyncing(true); setSyncMsg(null); setSyncError(null)
    try {
      const started = await adminTriggerSync()

      let run = started
      while (run.status === 'RUNNING') {
        await new Promise(resolve => setTimeout(resolve, POLL_INTERVAL_MS))
        run = await adminGetIngestionRun(run.id)
      }

      if (run.status === 'FAILED') {
        setSyncError(run.errorMessage ?? 'La sincronización con Instagram falló.')
      } else {
        setSyncMsg(`Sincronización completada: ${run.createdCount} creados, ${run.mergedCount} fusionados, ${run.rejectedCount} rechazados.`)
      }
      onFinished?.()
    } catch (e: any) {
      setSyncError(e.message)
    } finally {
      setSyncing(false)
    }
  }

  return { syncing, syncMsg, syncError, triggerSync }
}
