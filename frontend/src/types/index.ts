// ── Dominio ─────────────────────────────────────────────────────────────────

export type EventStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface Zone {
  id: number
  name: string
  description: string | null
  latitude: number | null
  longitude: number | null
}

export interface EventImage {
  id: number
  mediaUrl: string
  displayOrder: number
}

/** Refleja EventSummaryResponse del backend (listado paginado público) */
export interface EventSummary {
  id: string
  title: string
  caption: string | null
  locationText: string | null
  // Forma anidada (mock data)
  zone?: Zone | null
  // Forma plana (respuesta real del backend)
  zoneName?: string | null
  latitude?: number | null
  longitude?: number | null
  thumbnailUrl?: string | null
  eventDate: string | null      // ISO-8601 desde Spring
  status: EventStatus
  images?: EventImage[]
  createdAt: string
}

/** Refleja EventResponse del backend (detalle completo) */
export interface Event extends EventSummary {
  instagramPostId: string | null
  updatedAt: string | null
}

/** Wrapper de paginación — refleja PageResponse<T> del backend */
export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

// ── Recordatorios ────────────────────────────────────────────────────────────

/** Refleja ReminderRequest */
export interface ReminderRequest {
  eventId: string
  minutesBefore: number
}

/** Refleja ReminderResponse */
export interface ReminderRemoteResponse {
  id: string
  eventId: string
  eventTitle: string
  eventDate: string | null
  minutesBefore: number
  sent: boolean
  createdAt: string
}

// ── Autenticación ────────────────────────────────────────────────────────────

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
  fullName: string
}

/** Refleja AuthResponse del backend — el objeto user está anidado */
export interface AuthResponse {
  accessToken:  string
  refreshToken: string
  tokenType:    string          // siempre "Bearer"
  expiresIn:    number          // segundos hasta que vence el access token
  user: {
    id:       string
    email:    string
    fullName: string | null
    roles:    string[]
  }
}

// ── UI helpers (no vienen del backend) ──────────────────────────────────────

/** Representación de un evento enriquecida para la UI */
export interface UIEvent {
  id: string
  title: string
  tag: string          // zona o categoría display
  month: string        // 'Ago', 'Sep', ...
  day: string          // '15', '22', ...
  time: string         // '09:00 – 14:00'
  fullDate: string     // 'Sábado 15 de Agosto, 2026 · 09:00'
  location: string
  short: string        // resumen del caption (2 líneas)
  full: string         // caption completo
  imageUrl: string | null
  eventDate: Date | null
  reminded: boolean
  coordinates?: { lat: number; lng: number }
}

export type ReminderMinutes = 15 | 30 | 60 | 1440
