'use client'
import { useCallback, useEffect, useState } from 'react'
import AdminLayout from '@/components/admin/AdminLayout'
import {
  adminApproveEvent, adminDeleteEvent, adminGetEvents,
  adminRejectEvent, adminTriggerSync,
} from '@/lib/api'
import { AdminEvent } from '@/types'

const STATUS_LABELS: Record<string, string> = {
  PENDING:  'Pendiente',
  APPROVED: 'Publicado',
  REJECTED: 'Rechazado',
}

const STATUS_COLORS: Record<string, string> = {
  PENDING:  'bg-yellow-100 text-yellow-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
}

type Filter = '' | 'PENDING' | 'APPROVED' | 'REJECTED'

function formatDate(iso: string | null) {
  if (!iso) return '—'
  return new Date(iso).toLocaleDateString('es-EC', {
    day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

export default function AdminDashboardPage() {
  const [events, setEvents]       = useState<AdminEvent[]>([])
  const [total, setTotal]         = useState(0)
  const [page, setPage]           = useState(0)
  const [filter, setFilter]       = useState<Filter>('')
  const [loading, setLoading]     = useState(false)
  const [syncing, setSyncing]     = useState(false)
  const [syncMsg, setSyncMsg]     = useState<string | null>(null)
  const [error, setError]         = useState<string | null>(null)
  const [confirmDelete, setConfirmDelete] = useState<AdminEvent | null>(null)
  const [rejectTarget, setRejectTarget]   = useState<AdminEvent | null>(null)
  const [rejectReason, setRejectReason]   = useState('')

  const PAGE_SIZE = 20

  const load = useCallback(async (p: number, f: Filter) => {
    setLoading(true)
    setError(null)
    try {
      const data = await adminGetEvents(p, PAGE_SIZE, f || undefined)
      setEvents(data.content)
      setTotal(data.totalElements)
    } catch (e: any) {
      setError(e.message ?? 'Error al cargar eventos')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load(page, filter) }, [page, filter, load])

  async function handleApprove(ev: AdminEvent) {
    try {
      const updated = await adminApproveEvent(ev.id)
      setEvents(prev => prev.map(e => e.id === updated.id ? updated : e))
    } catch (e: any) { setError(e.message) }
  }

  async function handleRejectConfirm() {
    if (!rejectTarget) return
    try {
      const updated = await adminRejectEvent(rejectTarget.id, rejectReason || undefined)
      setEvents(prev => prev.map(e => e.id === updated.id ? updated : e))
    } catch (e: any) { setError(e.message) }
    finally { setRejectTarget(null); setRejectReason('') }
  }

  async function handleDelete() {
    if (!confirmDelete) return
    try {
      await adminDeleteEvent(confirmDelete.id)
      setEvents(prev => prev.filter(e => e.id !== confirmDelete.id))
      setTotal(t => t - 1)
    } catch (e: any) { setError(e.message) }
    finally { setConfirmDelete(null) }
  }

  async function handleSync() {
    setSyncing(true)
    setSyncMsg(null)
    setError(null)
    try {
      const run = await adminTriggerSync()
      setSyncMsg(`Sincronización completada: ${run.createdCount} creados, ${run.mergedCount} fusionados, ${run.rejectedCount} rechazados.`)
      load(0, filter)
      setPage(0)
    } catch (e: any) { setError(e.message) }
    finally { setSyncing(false) }
  }

  const totalPages = Math.ceil(total / PAGE_SIZE)

  return (
    <AdminLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-[#2d1b0e]">Gestión de Eventos</h1>
            <p className="text-sm text-[#7a6652] mt-0.5">{total} evento{total !== 1 ? 's' : ''} en total</p>
          </div>
          <button
            onClick={handleSync}
            disabled={syncing}
            className="flex items-center gap-2 bg-[#931934] text-white px-4 py-2 rounded-lg text-sm font-semibold hover:bg-[#7a1528] disabled:opacity-60 transition-colors"
          >
            {syncing ? (
              <span className="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
            ) : '🔄'}
            {syncing ? 'Sincronizando…' : 'Sincronizar Instagram'}
          </button>
        </div>

        {/* Mensajes */}
        {syncMsg && (
          <div className="bg-green-50 border border-green-200 text-green-800 text-sm px-4 py-3 rounded-lg">
            ✅ {syncMsg}
          </div>
        )}
        {error && (
          <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-lg">
            {error}
          </div>
        )}

        {/* Filtros */}
        <div className="flex gap-2">
          {(['', 'PENDING', 'APPROVED', 'REJECTED'] as Filter[]).map(f => (
            <button
              key={f}
              onClick={() => { setFilter(f); setPage(0) }}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-colors ${
                filter === f
                  ? 'bg-[#931934] text-white'
                  : 'bg-white border border-[#e8ddd4] text-[#7a6652] hover:border-[#931934] hover:text-[#931934]'
              }`}
            >
              {f === '' ? 'Todos' : STATUS_LABELS[f]}
            </button>
          ))}
        </div>

        {/* Tabla */}
        <div className="bg-white rounded-xl border border-[#e8ddd4] overflow-hidden">
          {loading ? (
            <div className="py-16 text-center text-[#7a6652] text-sm">Cargando…</div>
          ) : events.length === 0 ? (
            <div className="py-16 text-center text-[#7a6652] text-sm">No hay eventos con este filtro.</div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-[#f9f6f1] border-b border-[#e8ddd4]">
                <tr>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Título</th>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Zona</th>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Fecha evento</th>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Estado</th>
                  <th className="text-right px-4 py-3 font-semibold text-[#2d1b0e]">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#f0e8df]">
                {events.map(ev => (
                  <tr key={ev.id} className="hover:bg-[#fdf9f6] transition-colors">
                    <td className="px-4 py-3">
                      <div className="font-medium text-[#2d1b0e] line-clamp-1 max-w-[280px]">{ev.title}</div>
                      <div className="text-xs text-[#7a6652] mt-0.5">{formatDate(ev.createdAt)}</div>
                    </td>
                    <td className="px-4 py-3 text-[#7a6652]">{ev.zone?.name ?? ev.locationText ?? '—'}</td>
                    <td className="px-4 py-3 text-[#7a6652]">{formatDate(ev.eventDate)}</td>
                    <td className="px-4 py-3">
                      <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${STATUS_COLORS[ev.status]}`}>
                        {STATUS_LABELS[ev.status]}
                      </span>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2">
                        {ev.status === 'PENDING' && (
                          <>
                            <button
                              onClick={() => handleApprove(ev)}
                              className="text-green-700 hover:text-green-900 font-medium text-xs"
                            >
                              Aprobar
                            </button>
                            <button
                              onClick={() => setRejectTarget(ev)}
                              className="text-yellow-700 hover:text-yellow-900 font-medium text-xs"
                            >
                              Rechazar
                            </button>
                          </>
                        )}
                        <button
                          onClick={() => setConfirmDelete(ev)}
                          className="text-red-600 hover:text-red-800 font-medium text-xs"
                        >
                          Eliminar
                        </button>
                      </div>
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

      {/* Modal: confirmar eliminación */}
      {confirmDelete && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl p-6 max-w-sm w-full shadow-xl">
            <h3 className="font-bold text-[#2d1b0e] text-lg mb-2">¿Eliminar evento?</h3>
            <p className="text-sm text-[#7a6652] mb-6">
              <strong>"{confirmDelete.title}"</strong> se eliminará permanentemente junto con sus imágenes.
            </p>
            <div className="flex gap-3">
              <button
                onClick={() => setConfirmDelete(null)}
                className="flex-1 border border-[#e8ddd4] rounded-lg py-2 text-sm font-semibold text-[#7a6652] hover:border-[#931934] transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleDelete}
                className="flex-1 bg-red-600 text-white rounded-lg py-2 text-sm font-semibold hover:bg-red-700 transition-colors"
              >
                Eliminar
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: rechazar con motivo */}
      {rejectTarget && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl p-6 max-w-sm w-full shadow-xl">
            <h3 className="font-bold text-[#2d1b0e] text-lg mb-2">Rechazar evento</h3>
            <p className="text-sm text-[#7a6652] mb-3">
              <strong>"{rejectTarget.title}"</strong>
            </p>
            <textarea
              value={rejectReason}
              onChange={e => setRejectReason(e.target.value)}
              placeholder="Motivo del rechazo (opcional)"
              rows={3}
              className="w-full border border-[#e8ddd4] rounded-lg px-3 py-2 text-sm resize-none focus:outline-none focus:border-[#931934] mb-4"
            />
            <div className="flex gap-3">
              <button
                onClick={() => { setRejectTarget(null); setRejectReason('') }}
                className="flex-1 border border-[#e8ddd4] rounded-lg py-2 text-sm font-semibold text-[#7a6652] hover:border-[#931934] transition-colors"
              >
                Cancelar
              </button>
              <button
                onClick={handleRejectConfirm}
                className="flex-1 bg-[#931934] text-white rounded-lg py-2 text-sm font-semibold hover:bg-[#7a1528] transition-colors"
              >
                Rechazar
              </button>
            </div>
          </div>
        </div>
      )}
    </AdminLayout>
  )
}
