import { AuthResponse } from '@/types'

const ACCESS_TOKEN_KEY  = 'ucsg_access_token'
const REFRESH_TOKEN_KEY = 'ucsg_refresh_token'
const USER_KEY          = 'ucsg_user'
const ROLE_COOKIE       = 'ucsg_role'

// Igual a la duración del refresh token en el backend (7 días).
// Sin max-age, la cookie es "de sesión" y el navegador la borra al cerrarse,
// mientras el JWT en localStorage sigue vivo — eso deja al usuario con una
// sesión "a medias": autenticado según localStorage, pero el middleware
// (que solo lee la cookie) lo manda a /login como si hubiera cerrado sesión.
const ROLE_COOKIE_MAX_AGE = 60 * 60 * 24 * 7

function deriveRoleCookieValue(roles: string[]): string {
  return roles.find(r => r === 'ROLE_ADMIN')
    ?? roles.find(r => r === 'ROLE_SUPERVISOR')
    ?? 'ROLE_USER'
}

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
  if (user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user))
    // Cookie legible por el middleware de Next.js (Edge Runtime no accede a localStorage)
    // No es una barrera de seguridad — Spring Security verifica el JWT en cada request
    const role = deriveRoleCookieValue(user.roles)
    document.cookie = `${ROLE_COOKIE}=${role}; path=/; max-age=${ROLE_COOKIE_MAX_AGE}; SameSite=Lax`
  }
}

/**
 * Vuelve a escribir la cookie de rol a partir del usuario en localStorage.
 * Se invoca al montar páginas autenticadas para autocurar el caso en el que
 * la cookie se perdió (o expiró antes que el JWT) pero la sesión sigue
 * siendo válida — evita el "cierre de sesión" fantasma hacia /login.
 */
export function ensureRoleCookie(): void {
  if (typeof window === 'undefined') return
  const user = getStoredUser()
  if (!user) return
  const role = deriveRoleCookieValue(user.roles)
  document.cookie = `${ROLE_COOKIE}=${role}; path=/; max-age=${ROLE_COOKIE_MAX_AGE}; SameSite=Lax`
}

export function clearTokens(): void {
  localStorage.removeItem(ACCESS_TOKEN_KEY)
  localStorage.removeItem(REFRESH_TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  document.cookie = `${ROLE_COOKIE}=; path=/; max-age=0`
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

export function authHeader(): Record<string, string> {
  const token = getAccessToken()
  return token ? { Authorization: `Bearer ${token}` } : {}
}
