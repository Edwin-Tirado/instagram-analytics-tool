'use client'
import { useCallback, useEffect, useState } from 'react'
import { CheckCircle2, LucideIcon, Pencil, Trash2, XCircle } from 'lucide-react'
import AdminLayout from '@/components/admin/AdminLayout'
import { adminGetAuditLog } from '@/lib/api'
import { AuditLogEntry } from '@/types'

const ACTION_LABELS: Record<string, { label: string; color: string; icon: LucideIcon }> = {
  EDITED:   { label: 'Editado',   color: 'bg-blue-100 text-blue-800',   icon: Pencil },
  DELETED:  { label: 'Eliminado', color: 'bg-red-100 text-red-800',     icon: Trash2 },
  APPROVED: { label: 'Aprobado',  color: 'bg-green-100 text-green-800', icon: CheckCircle2 },
  REJECTED: { label: 'Rechazado', color: 'bg-yellow-100 text-yellow-800', icon: XCircle },
}

function formatDate(iso: string) {
  return new Date(iso).toLocaleString('es-EC', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

export default function AdminAuditPage() {
  const [entries, setEntries]   = useState<AuditLogEntry[]>([])
  const [total, setTotal]       = useState(0)
  const [page, setPage]         = useState(0)
  const [loading, setLoading]   = useState(false)
  const [error, setError]       = useState<string | null>(null)

  const PAGE_SIZE = 50

  const load = useCallback(async (p: number) => {
    setLoading(true); setError(null)
    try {
      const data = await adminGetAuditLog(p, PAGE_SIZE)
      setEntries(data.content)
      setTotal(data.totalElements)
    } catch (e: any) {
      setError(e.message ?? 'Error al cargar el registro')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load(page) }, [page, load])

  const totalPages = Math.ceil(total / PAGE_SIZE)

  // Resumen rápido
  const edited  = entries.filter(e => e.action === 'EDITED').length
  const deleted = entries.filter(e => e.action === 'DELETED').length

  return (
    <AdminLayout>
      <div className="space-y-6">

        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold text-[#2d1b0e]">Registro de Ediciones</h1>
          <p className="text-sm text-[#7a6652] mt-0.5">
            Historial de eventos editados o eliminados por supervisores y administradores
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
            { label: 'Total de registros', value: total,   color: 'text-[#2d1b0e]' },
            { label: 'Ediciones (esta página)',  value: edited,   color: 'text-blue-700'  },
            { label: 'Eliminaciones (esta página)', value: deleted, color: 'text-red-600'  },
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
          ) : entries.length === 0 ? (
            <div className="py-16 text-center text-[#7a6652] text-sm">
              No hay ediciones ni eliminaciones registradas aún.
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead className="bg-[#f9f6f1] border-b border-[#e8ddd4]">
                <tr>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Fecha y hora</th>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Acción</th>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Evento</th>
                  <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Realizado por</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#f0e8df]">
                {entries.map(entry => {
                  const meta = ACTION_LABELS[entry.action] ?? {
                    label: entry.action, color: 'bg-gray-100 text-gray-700', icon: null,
                  }
                  const Icon = meta.icon
                  return (
                    <tr key={entry.id} className="hover:bg-[#fdf9f6] transition-colors">
                      <td className="px-4 py-3 text-[#7a6652] whitespace-nowrap">
                        {formatDate(entry.createdAt)}
                      </td>
                      <td className="px-4 py-3">
                        <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-semibold ${meta.color}`}>
                          {Icon && <Icon size={12} strokeWidth={2.5} />} {meta.label}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-[#2d1b0e] font-medium max-w-[260px]">
                        <span className="block truncate" title={entry.eventTitle}>
                          {entry.eventTitle}
                        </span>
                        {entry.eventId && (
                          <span className="text-[10px] text-[#b89f90] font-mono block">
                            {entry.eventId}
                          </span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <div className="text-[#2d1b0e] font-medium">
                          {entry.supervisorName ?? '—'}
                        </div>
                        <div className="text-xs text-[#7a6652]">
                          {entry.supervisorEmail ?? ''}
                        </div>
                      </td>
                    </tr>
                  )
                })}
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
