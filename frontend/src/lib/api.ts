import {
  AdminEvent,
  AdminEventPage,
  AdminUser,
  AdminUserPage,
  AuditLogPage,
  AuthResponse,
  Event,
  EventSummary,
  IngestionRun,
  IngestionRunPage,
  LoginRequest,
  Page,
  RegisterRequest,
  ReminderRemoteResponse,
  ReminderRequest,
  UpdateEventBody,
  Zone,
} from '@/types'
import { authHeader, clearTokens, setTokens } from './auth'

const BASE_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'

// ── Utilidad central ─────────────────────────────────────────────────────────

async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...authHeader(),
      ...(init.headers as Record<string, string> | undefined),
    },
    ...init,
  })

  // Token expirado o inválido → limpiar sesión y redirigir al login
  if (res.status === 401) {
    clearTokens()
    if (typeof window !== 'undefined') {
      window.location.href = '/login'
    }
    throw new Error('Sesión expirada. Inicia sesión de nuevo.')
  }

  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    throw new Error(body?.message ?? `HTTP ${res.status}`)
  }

  // 204 No Content
  if (res.status === 204) return undefined as T
  return res.json() as Promise<T>
}

/**
 * Variante para endpoints de autenticación:
 * - NO redirige en 401 (deja que el catch del login muestre el error)
 * - Maneja 423 (cuenta bloqueada) con un error tipado
 * - Maneja 401 (credenciales incorrectas) con el mensaje del backend
 */
export class AccountLockedError extends Error {
  constructor(message?: string) {
    super(message ?? 'Tu cuenta ha sido bloqueada por múltiples intentos fallidos.')
    this.name = 'AccountLockedError'
  }
}

async function authFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<T> {
  const res = await fetch(`${BASE_URL}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...(init.headers as Record<string, string> | undefined),
    },
    ...init,
  })

  if (!res.ok) {
    const body = await res.json().catch(() => ({}))
    // 403 Forbidden = cuenta suspendida (nuevo comportamiento)
    // 423 Locked    = comportamiento anterior (compatibilidad hacia atrás)
    if (res.status === 403 || res.status === 423) {
      throw new AccountLockedError(body?.message)
    }
    throw new Error(body?.message ?? `HTTP ${res.status}`)
  }

  return res.json() as Promise<T>
}

// ── Autenticación ────────────────────────────────────────────────────────────

export async function login(body: LoginRequest): Promise<AuthResponse> {
  const data = await authFetch<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(body),
  })
  setTokens(data.accessToken, data.refreshToken, data.user)
  return data
}

export async function register(body: RegisterRequest): Promise<AuthResponse> {
  const data = await authFetch<AuthResponse>('/api/auth/register', {
    method: 'POST',
    body: JSON.stringify(body),
  })
  setTokens(data.accessToken, data.refreshToken, data.user)
  return data
}

export function logout(): void {
  clearTokens()
}

// ── Eventos públicos ─────────────────────────────────────────────────────────

export async function getEvents(
  page = 0,
  size = 20,
): Promise<Page<EventSummary>> {
  return apiFetch<Page<EventSummary>>(
    `/api/public/events?page=${page}&size=${size}`,
  )
}

export async function getEventById(id: string): Promise<Event> {
  return apiFetch<Event>(`/api/public/events/${id}`)
}

export async function getZones(): Promise<Zone[]> {
  return apiFetch<Zone[]>('/api/public/zones')
}

/** Crea una nueva ubicación (zona) del campus — SUPERVISOR o ADMIN. */
export async function createZone(body: { name: string; latitude: number; longitude: number }): Promise<Zone> {
  return apiFetch<Zone>('/api/supervisor/zones', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

// ── Recordatorios (requieren JWT) ────────────────────────────────────────────

export async function getMyReminders(): Promise<ReminderRemoteResponse[]> {
  return apiFetch<ReminderRemoteResponse[]>('/api/reminders')
}

export async function addReminder(
  body: ReminderRequest,
): Promise<ReminderRemoteResponse> {
  return apiFetch<ReminderRemoteResponse>('/api/reminders', {
    method: 'POST',
    body: JSON.stringify(body),
  })
}

export async function deleteReminder(reminderId: string): Promise<void> {
  return apiFetch<void>(`/api/reminders/${reminderId}`, { method: 'DELETE' })
}

// ── Admin: gestión de eventos ────────────────────────────────────────────────

export async function adminGetEvents(page = 0, size = 20, status?: string): Promise<AdminEventPage> {
  const qs = status ? `&status=${status}` : ''
  return apiFetch<AdminEventPage>(`/api/admin/events?page=${page}&size=${size}${qs}`)
}

export async function adminUpdateEvent(id: string, body: UpdateEventBody): Promise<AdminEvent> {
  return apiFetch<AdminEvent>(`/api/admin/events/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

export async function adminDeleteEvent(id: string): Promise<void> {
  return apiFetch<void>(`/api/admin/events/${id}`, { method: 'DELETE' })
}

export async function adminApproveEvent(id: string): Promise<AdminEvent> {
  return apiFetch<AdminEvent>(`/api/admin/events/${id}/approve`, { method: 'POST' })
}

export async function adminRejectEvent(id: string, reason?: string): Promise<AdminEvent> {
  return apiFetch<AdminEvent>(`/api/admin/events/${id}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason: reason ?? null }),
  })
}

/** Dispara la sincronización — el backend responde de inmediato con status RUNNING. */
export async function adminTriggerSync(): Promise<IngestionRun> {
  return apiFetch<IngestionRun>('/api/admin/ingestion/run', { method: 'POST' })
}

/** Usado para hacer polling del run hasta que termine (status SUCCESS o FAILED). */
export async function adminGetIngestionRun(id: string): Promise<IngestionRun> {
  return apiFetch<IngestionRun>(`/api/admin/ingestion/runs/${id}`)
}

/**
 * Dispara el refresco masivo de URLs de imágenes expiradas.
 * El backend acepta la petición de inmediato (202 Accepted) y ejecuta el
 * trabajo en segundo plano — las imágenes con 403 se corrigen en los logs.
 */
export async function adminRefreshImageUrls(): Promise<{ status: string; message: string }> {
  return apiFetch<{ status: string; message: string }>('/api/admin/ingestion/refresh-urls', {
    method: 'POST',
  })
}

/**
 * Reintenta asignar zona a los eventos sin ninguna, usando el caption ya
 * guardado — no vuelve a llamar a Instagram. Útil después de reemplazar
 * el catálogo de zonas.
 */
export async function adminRematchZones(): Promise<{ matched: number; candidates: number }> {
  return apiFetch<{ matched: number; candidates: number }>('/api/admin/events/rematch-zones', { method: 'POST' })
}

export interface InstagramTokenStatus {
  configured: boolean
  maskedToken: string | null
  updatedAt: string | null
  expiresAt: string | null
}

/** Estado (enmascarado) del access-token de Instagram vigente — ADMIN. */
export async function getInstagramTokenStatus(): Promise<InstagramTokenStatus> {
  return apiFetch<InstagramTokenStatus>('/api/admin/instagram/token')
}

/** Reemplaza manualmente el access-token de Instagram — ADMIN. */
export async function updateInstagramToken(accessToken: string): Promise<InstagramTokenStatus> {
  return apiFetch<InstagramTokenStatus>('/api/admin/instagram/token', {
    method: 'PUT',
    body: JSON.stringify({ accessToken }),
  })
}

// ── Supervisor: gestión y revisión de eventos ────────────────────────────────────

export async function getIngestionRuns(page = 0, size = 20): Promise<IngestionRunPage> {
  return apiFetch<IngestionRunPage>(`/api/supervisor/events/ingestion-runs?page=${page}&size=${size}`)
}

/**
 * Obtiene la lista paginada de eventos como SUPERVISOR o ADMIN, usando el
 * endpoint /api/supervisor/events que acepta ROLE_SUPERVISOR. Sin status,
 * trae todos los estados (para que el supervisor vea los PENDING); con
 * status, filtra igual que adminGetEvents.
 */
export async function supervisorGetEvents(page = 0, size = 20, status?: string): Promise<AdminEventPage> {
  const qs = status ? `&status=${status}` : ''
  return apiFetch<AdminEventPage>(`/api/supervisor/events?page=${page}&size=${size}${qs}`)
}

/**
 * Aprueba un evento PENDING como SUPERVISOR o ADMIN.
 * La acción queda auditada en el backend con el ID del supervisor.
 */
export async function supervisorApproveEvent(id: string): Promise<AdminEvent> {
  return apiFetch<AdminEvent>(`/api/supervisor/events/${id}/approve`, { method: 'POST' })
}

/**
 * Rechaza un evento PENDING como SUPERVISOR o ADMIN, con motivo opcional.
 * La acción queda auditada en el backend con el ID del supervisor.
 */
export async function supervisorRejectEvent(id: string, reason?: string): Promise<AdminEvent> {
  return apiFetch<AdminEvent>(`/api/supervisor/events/${id}/reject`, {
    method: 'POST',
    body: JSON.stringify({ reason: reason ?? null }),
  })
}

/**
 * Edita un evento como SUPERVISOR o ADMIN.
 * Usa el endpoint /api/supervisor/events/{id} que acepta ROLE_SUPERVISOR.
 */
export async function supervisorUpdateEvent(id: string, body: UpdateEventBody): Promise<AdminEvent> {
  return apiFetch<AdminEvent>(`/api/supervisor/events/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
}

/**
 * Elimina un evento como SUPERVISOR o ADMIN.
 * Usa el endpoint /api/supervisor/events/{id} que acepta ROLE_SUPERVISOR.
 */
export async function supervisorDeleteEvent(id: string): Promise<void> {
  return apiFetch<void>(`/api/supervisor/events/${id}`, { method: 'DELETE' })
}

// ── Admin: gestión de usuarios ────────────────────────────────────────────────

export async function adminGetUsers(page = 0, size = 20): Promise<AdminUserPage> {
  return apiFetch<AdminUserPage>(`/api/admin/users?page=${page}&size=${size}&sort=createdAt,desc`)
}

export async function adminToggleLock(id: string): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}/toggle-lock`, { method: 'PATCH' })
}

export async function adminToggleEnabled(id: string): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}/toggle-enabled`, { method: 'PATCH' })
}

export async function adminChangeRole(id: string, role: string): Promise<AdminUser> {
  return apiFetch<AdminUser>(`/api/admin/users/${id}/role`, {
    method: 'PATCH',
    body: JSON.stringify({ role }),
  })
}

export async function adminResetPassword(id: string): Promise<{ tempPassword: string; email: string }> {
  return apiFetch<{ tempPassword: string; email: string }>(`/api/admin/users/${id}/reset-password`, {
    method: 'PATCH',
  })
}

// ── Admin: registro de auditoría de ediciones/eliminaciones ──────────────────

/**
 * Historial paginado de ediciones y eliminaciones de eventos.
 * Solo accesible para ROLE_ADMIN.
 */
export async function adminGetAuditLog(page = 0, size = 50): Promise<AuditLogPage> {
  return apiFetch<AuditLogPage>(`/api/admin/events/audit-log?page=${page}&size=${size}`)
}

// ── Supervisor: conteo de recordatorios por evento ────────────────────────────

/**
 * Número de usuarios con recordatorio activo en el evento dado.
 * Accesible para ROLE_SUPERVISOR y ROLE_ADMIN.
 */
export async function supervisorGetReminderCount(eventId: string): Promise<number> {
  const data = await apiFetch<{ count: number }>(`/api/supervisor/events/${eventId}/reminder-count`)
  return data.count
}
