'use client'

import { useEffect, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { logout } from '@/lib/api'
import { getStoredUser, isAuthenticated } from '@/lib/auth'
import { AuthResponse } from '@/types'

export default function Navbar() {
  const router = useRouter()
  const [user, setUser] = useState<AuthResponse['user'] | null>(null)

  // Leer usuario del localStorage al montar (solo en cliente)
  useEffect(() => {
    if (isAuthenticated()) setUser(getStoredUser())
  }, [])

  function handleLogout() {
    logout()
    setUser(null)
    router.push('/login')
  }

  const displayName = user?.fullName?.split(' ')[0] ?? user?.email?.split('@')[0] ?? null

  return (
    <header className="
      bg-ucsg-crimson text-white h-[72px]
      flex items-center justify-between px-[6%]
      sticky top-0 z-50
      shadow-[0_1px_0_rgba(0,0,0,0.08)]
    ">
      {/* Logo */}
      <Link href="/" className="flex items-center gap-[13px] no-underline">
        <div className="
          w-[38px] h-[38px] border border-white/85 rounded-full
          flex items-center justify-center text-[1.15rem]
        ">
          ✛
        </div>
        <div className="flex flex-col leading-[1.05] text-white">
          <span className="font-extrabold text-[1.18rem] tracking-[0.5px]">UCSG</span>
          <span className="font-normal text-[0.7rem] tracking-[2.5px] uppercase opacity-[0.82]">
            Eventos
          </span>
        </div>
      </Link>

      {/* Nav links */}
      <nav className="flex gap-[30px] items-center">
        {['Cartelera', 'Facultades', 'Deportes'].map((link) => (
          <a
            key={link}
            href="#"
            className="
              text-white/90 no-underline text-[0.78rem] font-semibold
              uppercase tracking-[1px] hover:text-white transition-colors
            "
          >
            {link}
          </a>
        ))}

        {user ? (
          /* Usuario autenticado */
          <div className="flex items-center gap-3">
            <span className="text-white/80 text-[0.78rem] font-medium hidden sm:block">
              Hola, <strong className="text-white">{displayName}</strong>
            </span>
            {user.roles?.includes('ROLE_ADMIN') && (
              <Link
                href="/admin/dashboard"
                className="
                  bg-white/15 text-white border border-white/30
                  px-4 py-[7px] rounded-[22px]
                  text-[0.72rem] font-bold uppercase tracking-[0.8px]
                  no-underline whitespace-nowrap
                  hover:bg-white/25 transition-colors
                "
              >
                Panel Admin
              </Link>
            )}
            {user.roles?.includes('ROLE_SUPERVISOR') && !user.roles?.includes('ROLE_ADMIN') && (
              <Link
                href="/supervisor/logs"
                className="
                  bg-white/15 text-white border border-white/30
                  px-4 py-[7px] rounded-[22px]
                  text-[0.72rem] font-bold uppercase tracking-[0.8px]
                  no-underline whitespace-nowrap
                  hover:bg-white/25 transition-colors
                "
              >
                Panel Supervisor
              </Link>
            )}
            <button
              onClick={handleLogout}
              className="
                bg-white/15 text-white border border-white/30
                px-4 py-[7px] rounded-[22px]
                text-[0.72rem] font-bold uppercase tracking-[0.8px]
                cursor-pointer font-sans whitespace-nowrap
                hover:bg-white/25 transition-colors
              "
            >
              Cerrar sesión
            </button>
          </div>
        ) : (
          /* No autenticado */
          <Link
            href="/login"
            className="
              bg-white text-ucsg-crimson
              px-5 py-[9px] rounded-[22px]
              uppercase font-extrabold text-[0.72rem] tracking-[0.8px]
              no-underline whitespace-nowrap
              hover:bg-ucsg-pink transition-colors
            "
          >
            Iniciar sesión
          </Link>
        )}
      </nav>
    </header>
  )
}
