'use client'

import { useState, FormEvent } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { login } from '@/lib/api'

// ── Mapeo de mensajes de error del backend ────────────────────────────────────

function friendlyError(raw: string): string {
  const msg = raw.toLowerCase()
  if (msg.includes('credencial') || msg.includes('bad credential') || msg.includes('401'))
    return 'Correo electrónico o contraseña incorrectos.'
  if (msg.includes('bloquea') || msg.includes('locked') || msg.includes('423'))
    return 'Tu cuenta ha sido bloqueada por múltiples intentos fallidos. Contacta a soporte.'
  if (msg.includes('429') || msg.includes('rate') || msg.includes('limit'))
    return 'Demasiados intentos. Espera unos minutos antes de volver a intentarlo.'
  if (msg.includes('network') || msg.includes('fetch') || msg.includes('failed'))
    return 'No se pudo conectar con el servidor. Verifica tu conexión.'
  return 'Ocurrió un error inesperado. Inténtalo de nuevo.'
}

// ── Componente ────────────────────────────────────────────────────────────────

export default function LoginPage() {
  const router = useRouter()

  const [email,       setEmail]       = useState('')
  const [password,    setPassword]    = useState('')
  const [showPass,    setShowPass]    = useState(false)
  const [loading,     setLoading]     = useState(false)
  const [error,       setError]       = useState<string | null>(null)

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    setError(null)
    setLoading(true)

    try {
      await login({ email: email.trim(), password })
      router.push('/')
    } catch (err) {
      const raw = err instanceof Error ? err.message : String(err)
      setError(friendlyError(raw))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex">

      {/* ── Panel izquierdo — identidad institucional ─────────────────────── */}
      <div className="
        hidden lg:flex flex-col justify-between
        w-[45%] bg-ucsg-crimson text-white px-14 py-12
        relative overflow-hidden
      ">
        {/* Círculos decorativos */}
        <div className="absolute -top-24 -left-24 w-80 h-80 rounded-full bg-white/5" />
        <div className="absolute -bottom-20 -right-20 w-96 h-96 rounded-full bg-white/5" />
        <div className="absolute top-1/2 -right-16 w-64 h-64 rounded-full bg-ucsg-crimson-700/60" />

        {/* Logo */}
        <div className="relative flex items-center gap-4">
          <div className="
            w-10 h-10 border-2 border-white/80 rounded-full
            flex items-center justify-center text-xl
          ">
            ✛
          </div>
          <div className="leading-tight">
            <p className="font-extrabold text-xl tracking-wide">UCSG</p>
            <p className="text-[0.65rem] tracking-[3px] uppercase opacity-80">Eventos</p>
          </div>
        </div>

        {/* Cuerpo central */}
        <div className="relative">
          <h1 className="font-serif text-[2.8rem] font-semibold leading-[1.1] mb-5">
            Tu campus,<br />en un solo lugar.
          </h1>
          <p className="text-white/75 text-[1.05rem] leading-relaxed max-w-xs">
            Accede con tu cuenta institucional para guardar recordatorios y estar al tanto de todos los eventos universitarios.
          </p>
        </div>

        {/* Footer del panel */}
        <p className="relative text-white/40 text-xs">
          © {new Date().getFullYear()} Universidad Católica de Santiago de Guayaquil
        </p>
      </div>

      {/* ── Panel derecho — formulario ────────────────────────────────────── */}
      <div className="
        flex-1 flex flex-col items-center justify-center
        bg-ucsg-warm px-6 py-12
      ">
        {/* Logo móvil (solo visible en < lg) */}
        <div className="flex items-center gap-3 mb-10 lg:hidden">
          <div className="
            w-9 h-9 border-2 border-ucsg-crimson rounded-full
            flex items-center justify-center text-ucsg-crimson text-lg
          ">
            ✛
          </div>
          <div className="leading-tight">
            <p className="font-extrabold text-ucsg-crimson text-lg tracking-wide">UCSG</p>
            <p className="text-[0.6rem] tracking-[2.5px] uppercase text-ucsg-brown-400">Eventos</p>
          </div>
        </div>

        <div className="w-full max-w-[420px]">
          {/* Cabecera del formulario */}
          <div className="mb-8">
            <h2 className="font-serif text-[2rem] font-semibold text-ucsg-brown-900 leading-tight mb-1">
              Iniciar sesión
            </h2>
            <p className="text-ucsg-brown-400 text-[0.95rem]">
              Acceso exclusivo para la comunidad UCSG.
            </p>
          </div>

          {/* Bloque de error */}
          {error && (
            <div className="
              mb-5 px-4 py-3 rounded-xl
              bg-red-50 border border-red-200
              text-red-700 text-[0.9rem] font-medium
              flex items-start gap-3 animate-ucsg-rise
            ">
              <span className="mt-px text-base">⚠️</span>
              <span>{error}</span>
            </div>
          )}

          {/* Formulario */}
          <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">

            {/* Campo email */}
            <div>
              <label
                htmlFor="email"
                className="block text-[0.82rem] font-semibold text-ucsg-brown mb-[6px] uppercase tracking-[0.8px]"
              >
                Correo institucional
              </label>
              <input
                id="email"
                type="email"
                autoComplete="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="marcelo.acuna@cu.ucsg.edu.ec"
                disabled={loading}
                className="
                  w-full px-4 py-[13px] rounded-xl
                  border border-ucsg-border bg-white
                  text-ucsg-brown-900 text-[0.97rem]
                  placeholder:text-ucsg-brown-400/60
                  outline-none transition-all
                  focus:border-ucsg-crimson focus:ring-2 focus:ring-ucsg-crimson/15
                  disabled:opacity-50 disabled:cursor-not-allowed
                "
              />
            </div>

            {/* Campo contraseña */}
            <div>
              <label
                htmlFor="password"
                className="block text-[0.82rem] font-semibold text-ucsg-brown mb-[6px] uppercase tracking-[0.8px]"
              >
                Contraseña
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPass ? 'text' : 'password'}
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  disabled={loading}
                  className="
                    w-full px-4 py-[13px] pr-12 rounded-xl
                    border border-ucsg-border bg-white
                    text-ucsg-brown-900 text-[0.97rem]
                    placeholder:text-ucsg-brown-400/60
                    outline-none transition-all
                    focus:border-ucsg-crimson focus:ring-2 focus:ring-ucsg-crimson/15
                    disabled:opacity-50 disabled:cursor-not-allowed
                  "
                />
                <button
                  type="button"
                  onClick={() => setShowPass((v) => !v)}
                  className="
                    absolute right-3 top-1/2 -translate-y-1/2
                    text-ucsg-brown-400 hover:text-ucsg-brown
                    text-lg transition-colors select-none
                  "
                  aria-label={showPass ? 'Ocultar contraseña' : 'Mostrar contraseña'}
                >
                  {showPass ? '🙈' : '👁'}
                </button>
              </div>
            </div>

            {/* Botón de submit */}
            <button
              type="submit"
              disabled={loading || !email || !password}
              className="
                mt-1 w-full py-[14px] rounded-xl
                bg-ucsg-crimson text-white
                font-bold text-[1rem] tracking-[0.3px]
                border-none cursor-pointer font-sans
                transition-all duration-200
                hover:bg-ucsg-crimson-700
                disabled:opacity-50 disabled:cursor-not-allowed
                flex items-center justify-center gap-3
              "
            >
              {loading ? (
                <>
                  <SpinnerIcon />
                  Verificando…
                </>
              ) : (
                'Iniciar sesión'
              )}
            </button>
          </form>

          {/* Enlace de retorno */}
          <p className="mt-8 text-center text-[0.88rem] text-ucsg-brown-400">
            ¿Solo quieres explorar?{' '}
            <Link
              href="/"
              className="text-ucsg-crimson font-semibold hover:underline"
            >
              Ver cartelera pública →
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}

// ── Spinner inline ────────────────────────────────────────────────────────────

function SpinnerIcon() {
  return (
    <svg
      className="animate-spin h-5 w-5 text-white"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle
        className="opacity-25"
        cx="12" cy="12" r="10"
        stroke="currentColor" strokeWidth="4"
      />
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
      />
    </svg>
  )
}
