'use client'
import { useCallback, useEffect, useState } from 'react'
import AdminLayout from '@/components/admin/AdminLayout'
import { getIngestionRuns } from '@/lib/api'
import { IngestionRun } from '@/types'

const STATUS_COLORS: Record<string, string> = {
  RUNNING: 'bg-blue-100 text-blue-800',
  SUCCESS: 'bg-green-100 text-green-800',
  FAILED:  'bg-red-100 text-red-800',
}

const STATUS_ICONS: Record<string, string> = {
  RUNNING: '⏳',
  SUCCESS: '✅',
  FAILED:  '❌',
}

function formatDate(iso: string | null) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('es-EC', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

function formatDuration(secs: number | null) {
  if (secs === null) return '—'
  if (secs < 60) return `${secs}s`
  return `${Math.floor(secs / 60)}m ${secs % 60}s`
}

export default function SupervisorLogsPage() {
  const [runs, setRuns]       = useState<IngestionRun[]>([])
  const [total, setTotal]     = useState(0)
  const [page, setPage]       = useState(0)
  const [loading, setLoading] = useState(false)
  const [error, setError]     = useState<string | null>(null)

  const PAGE_SIZE = 20

  const load = useCallback(async (p: number) => {
    setLoading(true)
    setError(null)
    try {
      const data = await getIngestionRuns(p, PAGE_SIZE)
      setRuns(data.content)
      setTotal(data.totalElements)
    } catch (e: any) {
      setError(e.message ?? 'Error al cargar historial')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load(page) }, [page, load])

  const totalPages = Math.ceil(total / PAGE_SIZE)

  // Resumen de errores
  const failed  = runs.filter(r => r.status === 'FAILED')
  const created = runs.reduce((s, r) => s + r.createdCount, 0)
  const merged  = runs.reduce((s, r) => s + r.mergedCount, 0)

  return (
    <AdminLayout>
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-[#2d1b0e]">Historial de Ingesta</h1>
          <p className="text-sm text-[#7a6652] mt-0.5">
            Registro de ejecuciones del scraper de Instagram
          </p>
        </div>

        {error && (
          <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-lg">
            {error}
          </div>
        )}

        {/* Tarjetas de resumen */}
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { label: 'Ejecuciones', value: total, color: 'text-[#2d1b0e]' },
            { label: 'Con errores', value: failed.length, color: 'text-red-600' },
            { label: 'Eventos creados', value: created, color: 'text-green-700' },
            { label: 'Imágenes fusionadas', value: merged, color: 'text-blue-700' },
          ].map(card => (
            <div key={card.label} className="bg-white rounded-xl border border-[#e8ddd4] p-4">
              <div className={`text-2xl font-bold ${card.color}`}>{card.value}</div>
              <div className="text-xs text-[#7a6652] mt-1">{card.label}</div>
            </div>
          ))}
        </div>

        {/* Tabla */}
        <div className="bg-white rounded-xl border border-[#e8ddd4] overflow-hidden">
          {loading ? (
            <div className="py-16 text-center text-[#7a6652] text-sm">Cargando…</div>
          ) : runs.length === 0 ? (
            <div className="py-16 text-center text-[#7a6652] text-sm">
              Sin ejecuciones registradas aún.
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-[#f9f6f1] border-b border-[#e8ddd4]">
                <tr>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Inicio</th>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Tipo</th>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Duración</th>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Resultados</th>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Estado</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#f0e8df]">
                {runs.map(run => (
                  <tr key={run.id} className="hover:bg-[#fdf9f6] transition-colors">
                    <td className="px-4 py-3 text-[#7a6652]">{formatDate(run.startedAt)}</td>
                    <td className="px-4 py-3">
                      <span className={`text-xs font-medium ${run.triggerType === 'MANUAL' ? 'text-[#931934]' : 'text-[#7a6652]'}`}>
                        {run.triggerType === 'MANUAL' ? 'Manual' : 'Automático'}
                      </span>
                    </td>
                    <td className="px-4 py-3 text-[#7a6652]">{formatDuration(run.durationSeconds)}</td>
                    <td className="px-4 py-3">
                      <div className="flex gap-3 text-xs text-[#7a6652]">
                        <span className="text-green-700">+{run.createdCount} nuevos</span>
                        <span className="text-blue-700">↔ {run.mergedCount} fusionados</span>
                        <span className="text-red-600">✗ {run.rejectedCount} rechazados</span>
                      </div>
                      {run.errorMessage && (
                        <div className="text-xs text-red-600 mt-1 line-clamp-1" title={run.errorMessage}>
                          {run.errorMessage}
                        </div>
                      )}
                    </td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${STATUS_COLORS[run.status]}`}>
                        {STATUS_ICONS[run.status]} {run.status}
                      </span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>

        {/* Paginación */}
        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2">
            <button
              onClick={() => setPage(p => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-3 py-1.5 rounded border border-[#e8ddd4] text-sm disabled:opacity-40 hover:border-[#931934] transition-colors"
            >
              ←
            </button>
            <span className="text-sm text-[#7a6652]">Página {page + 1} de {totalPages}</span>
            <button
              onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
              disabled={page >= totalPages - 1}
              className="px-3 py-1.5 rounded border border-[#e8ddd4] text-sm disabled:opacity-40 hover:border-[#931934] transition-colors"
            >
              →
            </button>
          </div>
        )}
      </div>
    </AdminLayout>
  )
}
