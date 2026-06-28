import {
  AuthResponse,
  Event,
  EventSummary,
  LoginRequest,
  Page,
  RegisterRequest,
  ReminderRemoteResponse,
  ReminderRequest,
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

// ── Autenticación ────────────────────────────────────────────────────────────

export async function login(body: LoginRequest): Promise<AuthResponse> {
  const data = await apiFetch<AuthResponse>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify(body),
  })
  setTokens(data.accessToken, data.refreshToken, data.user)
  return data
}

export async function register(body: RegisterRequest): Promise<AuthResponse> {
  const data = await apiFetch<AuthResponse>('/api/auth/register', {
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
