import { AuthResponse } from '@/types'

const ACCESS_TOKEN_KEY  = 'ucsg_access_token'
const REFRESH_TOKEN_KEY = 'ucsg_refresh_token'
const USER_KEY          = 'ucsg_user'
const ROLE_COOKIE       = 'ucsg_role'

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
    const role = user.roles.find(r => r === 'ROLE_ADMIN')
      ?? user.roles.find(r => r === 'ROLE_SUPERVISOR')
      ?? 'ROLE_USER'
    document.cookie = `${ROLE_COOKIE}=${role}; path=/; SameSite=Lax`
  }
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
