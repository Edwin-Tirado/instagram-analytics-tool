'use client'
import Link from 'next/link'
import { useEffect, useState } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { clearTokens, getStoredUser } from '@/lib/auth'
import { AuthResponse } from '@/types'

interface Props { children: React.ReactNode }

const NAV_ADMIN = [
  { href: '/admin/dashboard', label: 'Eventos', icon: '📋' },
  { href: '/admin/dashboard?tab=users', label: 'Usuarios', icon: '👥' },
]

const NAV_SUPERVISOR = [
  { href: '/supervisor/logs', label: 'Historial Ingesta', icon: '📊' },
]

export default function AdminLayout({ children }: Props) {
  const pathname = usePathname()
  const router   = useRouter()
  const [user, setUser] = useState<AuthResponse['user'] | null>(null)

  useEffect(() => { setUser(getStoredUser()) }, [])

  const isAdmin = user?.roles.includes('ROLE_ADMIN') ?? false

  function handleLogout() {
    clearTokens()
    router.push('/login')
  }

  const navItems = isAdmin
    ? [...NAV_ADMIN, ...NAV_SUPERVISOR]
    : NAV_SUPERVISOR

  return (
    <div className="min-h-screen flex bg-[#f9f6f1]">
      {/* Sidebar */}
      <aside className="w-60 bg-[#931934] text-white flex flex-col">
        <div className="px-6 py-5 border-b border-white/20">
          <div className="text-xs font-semibold tracking-widest uppercase text-white/60 mb-0.5">UCSG</div>
          <div className="text-lg font-bold">Panel Admin</div>
        </div>

        <nav className="flex-1 px-3 py-4 space-y-1">
          {navItems.map((item) => {
            const active = pathname === item.href || pathname.startsWith(item.href.split('?')[0])
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                  active
                    ? 'bg-white/20 text-white'
                    : 'text-white/75 hover:bg-white/10 hover:text-white'
                }`}
              >
                <span>{item.icon}</span>
                {item.label}
              </Link>
            )
          })}
        </nav>

        <div className="px-4 py-4 border-t border-white/20">
          <div className="text-xs text-white/60 mb-1 truncate">{user?.email}</div>
          <div className="text-xs text-white/40 mb-3">
            {isAdmin ? 'Administrador' : 'Supervisor'}
          </div>
          <button
            onClick={handleLogout}
            className="w-full text-left text-xs text-white/70 hover:text-white transition-colors"
          >
            Cerrar sesión →
          </button>
        </div>
      </aside>

      {/* Contenido */}
      <div className="flex-1 flex flex-col min-w-0">
        <header className="bg-white border-b border-[#e8ddd4] px-8 py-4 flex items-center justify-between">
          <Link href="/" className="text-sm text-[#7a6652] hover:text-[#931934] transition-colors">
            ← Volver al sitio
          </Link>
          <div className="flex items-center gap-2 text-sm text-[#7a6652]">
            <span className="w-7 h-7 rounded-full bg-[#931934] text-white flex items-center justify-center text-xs font-bold">
              {user?.fullName?.[0] ?? user?.email?.[0] ?? '?'}
            </span>
            <span className="hidden sm:block">{user?.fullName ?? user?.email}</span>
          </div>
        </header>
        <main className="flex-1 p-8 overflow-auto">
          {children}
        </main>
      </div>
    </div>
  )
}
