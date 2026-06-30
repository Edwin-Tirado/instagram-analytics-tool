'use client'
import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'next/navigation'
import AdminLayout from '@/components/admin/AdminLayout'
import DataTable from '@/components/admin/DataTable'
import {
  adminApproveEvent, adminDeleteEvent, adminGetEvents,
  adminRejectEvent, adminTriggerSync, adminUpdateEvent,
  adminGetUsers, adminToggleLock, adminToggleEnabled, adminChangeRole,
} from '@/lib/api'
import { AdminEvent, AdminUser } from '@/types'

// ── Eventos ──────────────────────────────────────────────────────────────────

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

// ── Tab Eventos ───────────────────────────────────────────────────────────────

function EventsTab() {
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
  const [editTarget, setEditTarget]       = useState<AdminEvent | null>(null)
  const [editForm, setEditForm]           = useState({ title: '', eventDate: '', locationText: '' })

  const PAGE_SIZE = 20

  const load = useCallback(async (p: number, f: Filter) => {
    setLoading(true); setError(null)
    try {
      const data = await adminGetEvents(p, PAGE_SIZE, f || undefined)
      setEvents(data.content); setTotal(data.totalElements)
    } catch (e: any) { setError(e.message ?? 'Error al cargar eventos') }
    finally { setLoading(false) }
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

  function openEdit(ev: AdminEvent) {
    setEditTarget(ev)
    setEditForm({
      title:        ev.title,
      eventDate:    ev.eventDate ? ev.eventDate.slice(0, 16) : '',
      locationText: ev.locationText ?? '',
    })
  }

  async function handleEditSave() {
    if (!editTarget) return
    try {
      const updated = await adminUpdateEvent(editTarget.id, {
        title:        editForm.title,
        eventDate:    editForm.eventDate || null,
        locationText: editForm.locationText || null,
      })
      setEvents(prev => prev.map(e => e.id === updated.id ? updated : e))
    } catch (e: any) { setError(e.message) }
    finally { setEditTarget(null) }
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
    setSyncing(true); setSyncMsg(null); setError(null)
    try {
      const run = await adminTriggerSync()
      setSyncMsg(`Sincronización completada: ${run.createdCount} creados, ${run.mergedCount} fusionados, ${run.rejectedCount} rechazados.`)
      load(0, filter); setPage(0)
    } catch (e: any) { setError(e.message) }
    finally { setSyncing(false) }
  }

  const totalPages = Math.ceil(total / PAGE_SIZE)

  return (
    <>
      <div className="space-y-6">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-2xl font-bold text-[#2d1b0e]">Gestión de Eventos</h1>
            <p className="text-sm text-[#7a6652] mt-0.5">{total} evento{total !== 1 ? 's' : ''} en total</p>
          </div>
          <button
            onClick={handleSync} disabled={syncing}
            className="flex items-center gap-2 bg-[#931934] text-white px-4 py-2 rounded-lg text-sm font-semibold hover:bg-[#7a1528] disabled:opacity-60 transition-colors"
          >
            {syncing ? <span className="inline-block w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" /> : '🔄'}
            {syncing ? 'Sincronizando…' : 'Sincronizar Instagram'}
          </button>
        </div>

        {syncMsg && <div className="bg-green-50 border border-green-200 text-green-800 text-sm px-4 py-3 rounded-lg">✅ {syncMsg}</div>}
        {error   && <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-lg">{error}</div>}

        <div className="flex gap-2">
          {(['', 'PENDING', 'APPROVED', 'REJECTED'] as Filter[]).map(f => (
            <button key={f} onClick={() => { setFilter(f); setPage(0) }}
              className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-colors ${
                filter === f ? 'bg-[#931934] text-white' : 'bg-white border border-[#e8ddd4] text-[#7a6652] hover:border-[#931934] hover:text-[#931934]'
              }`}
            >
              {f === '' ? 'Todos' : STATUS_LABELS[f]}
            </button>
          ))}
        </div>

        <DataTable<AdminEvent>
          data={events}
          loading={loading}
          pageSize={PAGE_SIZE}
          onPageChange={() => {
            // Paginación manejada por el backend — los botones del DataTable son decorativos aquí.
            // El control real de página está en los botones Anterior/Siguiente de abajo.
          }}
          columns={[
            {
              header: 'Título',
              accessor: (ev) => (
                <div>
                  <div className="font-medium text-[#2d1b0e] line-clamp-1 max-w-[280px]">
                    {ev.title}
                  </div>
                  <div className="text-xs text-[#7a6652] mt-0.5">
                    {formatDate(ev.createdAt)}
                  </div>
                </div>
              ),
            },
            {
              header: 'Fecha',
              accessor: (ev) => (
                <span className="text-[#7a6652]">{formatDate(ev.eventDate)}</span>
              ),
            },
            {
              header: 'Estado',
              accessor: (ev) => (
                <span
                  className={`px-2 py-0.5 rounded-full text-xs font-semibold ${
                    STATUS_COLORS[ev.status] ?? 'bg-gray-100 text-gray-600'
                  }`}
                >
                  {STATUS_LABELS[ev.status] ?? ev.status}
                </span>
              ),
            },
            {
              header: 'Acciones',
              accessor: (ev) => (
                <div className="flex items-center gap-2">
                  {ev.status === 'PENDING' && (
                    <>
                      <button
                        onClick={() => handleApprove(ev)}
                        className="text-green-700 hover:text-green-900 font-medium text-xs transition-colors"
                      >
                        Aprobar
                      </button>
                      <button
                        onClick={() => setRejectTarget(ev)}
                        className="text-yellow-700 hover:text-yellow-900 font-medium text-xs transition-colors"
                      >
                        Rechazar
                      </button>
                    </>
                  )}
                  <button
                    onClick={() => {
                      console.log('Editar evento ID:', ev.id)
                      openEdit(ev)
                    }}
                    className="text-blue-600 hover:text-blue-800 font-medium text-xs transition-colors"
                  >
                    Editar
                  </button>
                  <button
                    onClick={() => {
                      console.log('Eliminar evento ID:', ev.id)
                      setConfirmDelete(ev)
                    }}
                    className="text-red-600 hover:text-red-800 font-medium text-xs transition-colors"
                  >
                    Eliminar
                  </button>
                </div>
              ),
            },
          ]}
        />

        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2">
            <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
              className="px-3 py-1.5 rounded border border-[#e8ddd4] text-sm disabled:opacity-40 hover:border-[#931934] transition-colors">←</button>
            <span className="text-sm text-[#7a6652]">Página {page + 1} de {totalPages}</span>
            <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
              className="px-3 py-1.5 rounded border border-[#e8ddd4] text-sm disabled:opacity-40 hover:border-[#931934] transition-colors">→</button>
          </div>
        )}
      </div>

      {confirmDelete && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl p-6 max-w-sm w-full shadow-xl">
            <h3 className="font-bold text-[#2d1b0e] text-lg mb-2">¿Eliminar evento?</h3>
            <p className="text-sm text-[#7a6652] mb-6"><strong>"{confirmDelete.title}"</strong> se eliminará permanentemente.</p>
            <div className="flex gap-3">
              <button onClick={() => setConfirmDelete(null)} className="flex-1 border border-[#e8ddd4] rounded-lg py-2 text-sm font-semibold text-[#7a6652] hover:border-[#931934] transition-colors">Cancelar</button>
              <button onClick={handleDelete} className="flex-1 bg-red-600 text-white rounded-lg py-2 text-sm font-semibold hover:bg-red-700 transition-colors">Eliminar</button>
            </div>
          </div>
        </div>
      )}

      {rejectTarget && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl p-6 max-w-sm w-full shadow-xl">
            <h3 className="font-bold text-[#2d1b0e] text-lg mb-2">Rechazar evento</h3>
            <p className="text-sm text-[#7a6652] mb-3"><strong>"{rejectTarget.title}"</strong></p>
            <textarea value={rejectReason} onChange={e => setRejectReason(e.target.value)}
              placeholder="Motivo del rechazo (opcional)" rows={3}
              className="w-full border border-[#e8ddd4] rounded-lg px-3 py-2 text-sm resize-none focus:outline-none focus:border-[#931934] mb-4" />
            <div className="flex gap-3">
              <button onClick={() => { setRejectTarget(null); setRejectReason('') }}
                className="flex-1 border border-[#e8ddd4] rounded-lg py-2 text-sm font-semibold text-[#7a6652] hover:border-[#931934] transition-colors">Cancelar</button>
              <button onClick={handleRejectConfirm}
                className="flex-1 bg-[#931934] text-white rounded-lg py-2 text-sm font-semibold hover:bg-[#7a1528] transition-colors">Rechazar</button>
            </div>
          </div>
        </div>
      )}

      {editTarget && (
        <div className="fixed inset-0 bg-black/40 flex items-center justify-center z-50 p-4">
          <div className="bg-white rounded-xl p-6 max-w-md w-full shadow-xl">
            <h3 className="font-bold text-[#2d1b0e] text-lg mb-4">Editar evento</h3>
            <div className="space-y-3">
              <div>
                <label className="text-xs font-semibold text-[#7a6652] uppercase tracking-wide">Título</label>
                <input
                  type="text"
                  value={editForm.title}
                  onChange={e => setEditForm(f => ({ ...f, title: e.target.value }))}
                  className="w-full mt-1 border border-[#e8ddd4] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[#931934]"
                />
              </div>
              <div>
                <label className="text-xs font-semibold text-[#7a6652] uppercase tracking-wide">Fecha del evento</label>
                <input
                  type="datetime-local"
                  value={editForm.eventDate}
                  onChange={e => setEditForm(f => ({ ...f, eventDate: e.target.value }))}
                  className="w-full mt-1 border border-[#e8ddd4] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[#931934]"
                />
              </div>
              <div>
                <label className="text-xs font-semibold text-[#7a6652] uppercase tracking-wide">Ubicación</label>
                <input
                  type="text"
                  value={editForm.locationText}
                  onChange={e => setEditForm(f => ({ ...f, locationText: e.target.value }))}
                  placeholder="Ej. Auditorio Principal"
                  className="w-full mt-1 border border-[#e8ddd4] rounded-lg px-3 py-2 text-sm focus:outline-none focus:border-[#931934]"
                />
              </div>
            </div>
            <div className="flex gap-3 mt-5">
              <button onClick={() => setEditTarget(null)}
                className="flex-1 border border-[#e8ddd4] rounded-lg py-2 text-sm font-semibold text-[#7a6652] hover:border-[#931934] transition-colors">Cancelar</button>
              <button onClick={handleEditSave}
                className="flex-1 bg-[#931934] text-white rounded-lg py-2 text-sm font-semibold hover:bg-[#7a1528] transition-colors">Guardar</button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}

// ── Tab Usuarios ──────────────────────────────────────────────────────────────

type UserFilter = 'all' | 'locked' | 'disabled'

function UsersTab() {
  const [users, setUsers]           = useState<AdminUser[]>([])
  const [total, setTotal]           = useState(0)
  const [page, setPage]             = useState(0)
  const [loading, setLoading]       = useState(false)
  const [error, setError]           = useState<string | null>(null)
  const [userFilter, setUserFilter] = useState<UserFilter>('all')

  const PAGE_SIZE = 20

  const load = useCallback(async (p: number) => {
    setLoading(true); setError(null)
    try {
      const data = await adminGetUsers(p, PAGE_SIZE)
      setUsers(data.content); setTotal(data.totalElements)
    } catch (e: any) { setError(e.message ?? 'Error al cargar usuarios') }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load(page) }, [page, load])

  async function handleToggleLock(u: AdminUser) {
    try {
      const updated = await adminToggleLock(u.id)
      setUsers(prev => prev.map(x => x.id === updated.id ? updated : x))
    } catch (e: any) { setError(e.message) }
  }

  async function handleToggleEnabled(u: AdminUser) {
    try {
      const updated = await adminToggleEnabled(u.id)
      setUsers(prev => prev.map(x => x.id === updated.id ? updated : x))
    } catch (e: any) { setError(e.message) }
  }

  async function handleChangeRole(u: AdminUser, newRole: string) {
    try {
      const updated = await adminChangeRole(u.id, newRole)
      setUsers(prev => prev.map(x => x.id === updated.id ? updated : x))
    } catch (e: any) { setError(e.message) }
  }

  // Filtro client-side (los datos ya están cargados)
  const filtered = users.filter(u => {
    if (userFilter === 'locked')   return u.locked
    if (userFilter === 'disabled') return !u.enabled
    return true
  })

  const lockedCount   = users.filter(u => u.locked).length
  const disabledCount = users.filter(u => !u.enabled).length

  const totalPages = Math.ceil(total / PAGE_SIZE)

  const roleLabel = (roles: string[]) => {
    if (roles.includes('ROLE_ADMIN')) return { label: 'Admin', color: 'bg-purple-100 text-purple-800' }
    if (roles.includes('ROLE_SUPERVISOR')) return { label: 'Supervisor', color: 'bg-blue-100 text-blue-800' }
    return { label: 'Usuario', color: 'bg-gray-100 text-gray-700' }
  }

  return (
    <div className="space-y-6">
      <div className="flex items-start justify-between">
        <div>
          <h1 className="text-2xl font-bold text-[#2d1b0e]">Gestión de Usuarios</h1>
          <p className="text-sm text-[#7a6652] mt-0.5">{total} usuario{total !== 1 ? 's' : ''} registrados</p>
        </div>
        {/* Resumen rápido de bloqueados */}
        {lockedCount > 0 && (
          <div className="flex items-center gap-2 bg-red-50 border border-red-200 text-red-700 text-sm font-semibold px-3 py-1.5 rounded-lg">
            <span>&#128274;</span>
            <span>{lockedCount} cuenta{lockedCount > 1 ? 's' : ''} bloqueada{lockedCount > 1 ? 's' : ''}</span>
          </div>
        )}
      </div>

      {/* Filtros */}
      <div className="flex gap-2">
        {([
          { key: 'all',      label: `Todos (${total})` },
          { key: 'locked',   label: `🔒 Bloqueados (${lockedCount})` },
          { key: 'disabled', label: `Desactivados (${disabledCount})` },
        ] as { key: UserFilter; label: string }[]).map(f => (
          <button
            key={f.key}
            onClick={() => setUserFilter(f.key)}
            className={`px-3 py-1.5 rounded-full text-xs font-semibold transition-colors ${
              userFilter === f.key
                ? f.key === 'locked'
                  ? 'bg-red-600 text-white'
                  : 'bg-[#931934] text-white'
                : 'bg-white border border-[#e8ddd4] text-[#7a6652] hover:border-[#931934] hover:text-[#931934]'
            }`}
          >
            {f.label}
          </button>
        ))}
      </div>

      {error && <div className="bg-red-50 border border-red-200 text-red-800 text-sm px-4 py-3 rounded-lg">{error}</div>}

      <div className="bg-white rounded-xl border border-[#e8ddd4] overflow-hidden">
        {loading ? (
          <div className="py-16 text-center text-[#7a6652] text-sm">Cargando…</div>
        ) : filtered.length === 0 ? (
          <div className="py-16 text-center text-[#7a6652] text-sm">
            {userFilter === 'locked' ? 'No hay cuentas bloqueadas. ✅' : 'Sin usuarios registrados.'}
          </div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-[#f9f6f1] border-b border-[#e8ddd4]">
              <tr>
                <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Usuario</th>
                <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Rol</th>
                <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Registro</th>
                <th className="text-left px-4 py-3 font-semibold text-[#2d1b0e]">Estado</th>
                <th className="text-right px-4 py-3 font-semibold text-[#2d1b0e]">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-[#f0e8df]">
              {filtered.map(u => {
                const role = roleLabel(u.roles)
                return (
                  <tr
                    key={u.id}
                    className={`transition-colors ${
                      u.locked
                        ? 'bg-red-50 hover:bg-red-100'
                        : 'hover:bg-[#fdf9f6]'
                    }`}
                  >
                    <td className="px-4 py-3">
                      <div className="flex items-center gap-2">
                        {u.locked && (
                          <span title={`Bloqueado tras ${u.failedLoginAttempts} intentos`} className="text-red-500 text-base">&#128274;</span>
                        )}
                        <div>
                          <div className="font-medium text-[#2d1b0e]">{u.fullName || '—'}</div>
                          <div className="text-xs text-[#7a6652]">{u.email}</div>
                        </div>
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      {u.roles.includes('ROLE_ADMIN') ? (
                        <span className={`px-2 py-0.5 rounded-full text-xs font-semibold ${role.color}`}>{role.label}</span>
                      ) : (
                        <select
                          value={u.roles.includes('ROLE_SUPERVISOR') ? 'ROLE_SUPERVISOR' : 'ROLE_USER'}
                          onChange={e => handleChangeRole(u, e.target.value)}
                          className="text-xs border border-[#e8ddd4] rounded-lg px-2 py-1 focus:outline-none focus:border-[#931934] bg-white text-[#2d1b0e]"
                        >
                          <option value="ROLE_USER">Usuario</option>
                          <option value="ROLE_SUPERVISOR">Supervisor</option>
                        </select>
                      )}
                    </td>
                    <td className="px-4 py-3 text-[#7a6652] text-xs">{formatDate(u.createdAt)}</td>
                    <td className="px-4 py-3">
                      <div className="flex flex-col gap-0.5">
                        <span className={`text-xs font-medium ${u.enabled ? 'text-green-700' : 'text-red-600'}`}>
                          {u.enabled ? 'Activo' : 'Desactivado'}
                        </span>
                        {u.locked && (
                          <span className="text-xs font-semibold text-red-600">
                            Bloqueado • {u.failedLoginAttempts} intentos
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-4 py-3">
                      <div className="flex items-center justify-end gap-2 flex-wrap">
                        {/* Desbloquear — destacado como acción principal cuando aplica */}
                        {u.locked && (
                          <button
                            onClick={() => handleToggleLock(u)}
                            className="flex items-center gap-1 bg-red-600 hover:bg-red-700 text-white text-xs font-semibold px-3 py-1.5 rounded-lg transition-colors"
                          >
                            <span>&#128275;</span>
                            Desbloquear
                          </button>
                        )}
                        <button
                          onClick={() => handleToggleEnabled(u)}
                          className={`text-xs font-medium ${
                            u.enabled
                              ? 'text-red-600 hover:text-red-800'
                              : 'text-green-700 hover:text-green-900'
                          }`}
                        >
                          {u.enabled ? 'Desactivar' : 'Activar'}
                        </button>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2">
          <button onClick={() => setPage(p => Math.max(0, p - 1))} disabled={page === 0}
            className="px-3 py-1.5 rounded border border-[#e8ddd4] text-sm disabled:opacity-40 hover:border-[#931934] transition-colors">←</button>
          <span className="text-sm text-[#7a6652]">Página {page + 1} de {totalPages}</span>
          <button onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
            className="px-3 py-1.5 rounded border border-[#e8ddd4] text-sm disabled:opacity-40 hover:border-[#931934] transition-colors">→</button>
        </div>
      )}
    </div>
  )
}

// ── Página principal ──────────────────────────────────────────────────────────

export default function AdminDashboardPage() {
  const searchParams = useSearchParams()
  const tab = searchParams.get('tab')

  return (
    <AdminLayout>
      {tab === 'users' ? <UsersTab /> : <EventsTab />}
    </AdminLayout>
  )
}
