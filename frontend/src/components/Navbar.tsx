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
      bg-ucsg-crimson/95 backdrop-blur-md text-white h-[72px]
      flex items-center justify-between px-[6%]
      sticky top-0 z-50
      border-b border-ucsg-crimson-700/50
      shadow-[0_4px_20px_rgba(155,14,62,0.15)]
      transition-all duration-300
    ">
      {/* Logo */}
      <Link href="/" className="flex items-center gap-[12px] no-underline group">
        <div className="
          w-[38px] h-[38px] bg-white/10 border border-white/20
          text-white rounded-xl flex items-center justify-center text-[1.2rem] font-bold
          shadow-md group-hover:scale-105 transition-transform duration-200
        ">
          ✛
        </div>
        <div className="flex flex-col leading-[1.05]">
          <span className="font-extrabold text-[1.22rem] tracking-[-0.2px] text-white">UCSG</span>
          <span className="font-bold text-[0.68rem] tracking-[2.5px] uppercase text-ucsg-pink">
            Eventos
          </span>
        </div>
      </Link>

      {/* Auth Controls */}
      <div className="flex items-center gap-3">
        {user ? (
          <div className="flex items-center gap-3">
            <span className="text-white/80 text-[0.82rem] font-medium hidden sm:block">
              Hola, <strong className="text-white font-bold">{displayName}</strong>
            </span>
            {user.roles?.includes('ROLE_ADMIN') && (
              <Link
                href="/admin/dashboard"
                className="
                  bg-white/10 text-white border border-white/20
                  px-4 py-[7px] rounded-full
                  text-[0.72rem] font-bold uppercase tracking-[0.8px]
                  no-underline whitespace-nowrap
                  hover:bg-white/20 hover:scale-[1.02] transition-all duration-200
                "
              >
                Panel Admin
              </Link>
            )}
            {user.roles?.includes('ROLE_SUPERVISOR') && !user.roles?.includes('ROLE_ADMIN') && (
              <Link
                href="/supervisor/logs"
                className="
                  bg-white/10 text-white border border-white/20
                  px-4 py-[7px] rounded-full
                  text-[0.72rem] font-bold uppercase tracking-[0.8px]
                  no-underline whitespace-nowrap
                  hover:bg-white/20 hover:scale-[1.02] transition-all duration-200
                "
              >
                Panel Supervisor
              </Link>
            )}
            <button
              onClick={handleLogout}
              className="
                bg-white/10 text-white border border-white/20
                px-4 py-[7px] rounded-full
                text-[0.72rem] font-bold uppercase tracking-[0.8px]
                cursor-pointer font-sans whitespace-nowrap
                hover:bg-white/20 hover:scale-[1.02] transition-all duration-200
              "
            >
              Cerrar sesión
            </button>
          </div>
        ) : (
          <Link
            href="/login"
            className="
              bg-white text-ucsg-crimson
              px-5 py-[9px] rounded-full
              uppercase font-extrabold text-[0.72rem] tracking-[0.8px]
              no-underline whitespace-nowrap shadow-sm
              hover:bg-white/95 hover:scale-[1.02] transition-all duration-200
            "
          >
            Iniciar sesión
          </Link>
        )}
      </div>
    </header>
  )
}

