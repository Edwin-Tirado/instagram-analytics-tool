import { AuthResponse } from '@/types'

const ACCESS_TOKEN_KEY  = 'ucsg_access_token'
const REFRESH_TOKEN_KEY = 'ucsg_refresh_token'
const USER_KEY          = 'ucsg_user'

// ── Persistencia de tokens ───────────────────────────────────────────────────

export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null
  return localStorage.getItem(ACCESS_TOKEN_KEY)
}

export function setTokens(
  access:  string,
  refresh: string,
  user?:   AuthResponse['user'],
): void {
  localStorage.setItem(ACCESS_TOKEN_KEY,  access)
  localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
  if (user) localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

// ── Estado del usuario ───────────────────────────────────────────────────────

export function isAuthenticated(): boolean {
  return Boolean(getAccessToken())
}

export function getStoredUser(): AuthResponse['user'] | null {
  if (typeof window === 'undefined') return null
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try { return JSON.parse(raw) as AuthResponse['user'] }
  catch { return null }
}

export function hasRole(role: string): boolean {
  const user = getStoredUser()
  return user?.roles.includes(role) ?? false
}

// ── Cabecera Authorization ───────────────────────────────────────────────────

/** Listo para usar en fetch({ headers: authHeader() }) */
export function authHeader(): Record<string, string> {
  const token = getAccessToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}
