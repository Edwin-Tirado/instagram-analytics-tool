'use client'

import { useState, FormEvent } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { login, AccountLockedError } from '@/lib/api'
import { getStoredUser } from '@/lib/auth'

// Debe coincidir con LoginAttemptService.MAX_ATTEMPTS en el backend
const MAX_ATTEMPTS = 5

export default function LoginPage() {
  const router = useRouter()

  const [email,    setEmail]    = useState('')
  const [password, setPassword] = useState('')
  const [showPass, setShowPass] = useState(false)
  const [loading,  setLoading]  = useState(false)
  const [error,    setError]    = useState<string | null>(null)

  // isLocked = verdad cuando el backend confirma 423 (persiste por sesión de navegación)
  const [isLocked, setIsLocked] = useState(false)
  // lockedEmail = el email que está bloqueado, para resetear si el usuario cambia de cuenta
  const [lockedEmail, setLockedEmail] = useState('')
  const [attempts, setAttempts] = useState(0)

  const remaining = MAX_ATTEMPTS - attempts

  // Si el usuario escribe un email distinto al bloqueado, resetea el bloqueo local
  function handleEmailChange(value: string) {
    setEmail(value)
    if (isLocked && value.trim().toLowerCase() !== lockedEmail) {
      setIsLocked(false)
      setLockedEmail('')
      setAttempts(0)
      setError(null)
    }
  }

  async function handleSubmit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    if (isLocked) return
    setError(null)
    setLoading(true)

    try {
      await login({ email: email.trim(), password })
      const user = getStoredUser()
      if (user?.roles.includes('ROLE_ADMIN')) {
        router.push('/admin/dashboard')
      } else if (user?.roles.includes('ROLE_SUPERVISOR')) {
        router.push('/supervisor/logs')
      } else {
        router.push('/')
      }
    } catch (err) {
      if (err instanceof AccountLockedError) {
        // El backend confirma que la cuenta esta bloqueada (423)
        // Esto ocurre tanto en el intento #5 como en recargas posteriores
        setIsLocked(true)
        setLockedEmail(email.trim().toLowerCase())
        setAttempts(MAX_ATTEMPTS)
        setError(null)
      } else {
        const newAttempts = attempts + 1
        setAttempts(newAttempts)

        if (newAttempts >= MAX_ATTEMPTS) {
          setIsLocked(true)
          setLockedEmail(email.trim().toLowerCase())
          setError(null)
        } else {
          const left = MAX_ATTEMPTS - newAttempts
          setError(
            `Correo electr\u00f3nico o contrase\u00f1a incorrectos. ` +
            `Te ${left === 1 ? 'queda' : 'quedan'} ${left} intento${left === 1 ? '' : 's'}.`
          )
        }
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex">

      {/* Panel izquierdo */}
      <div className="
        hidden lg:flex flex-col justify-between
        w-[45%] bg-ucsg-crimson text-white px-14 py-12
        relative overflow-hidden
      ">
        <div className="absolute -top-24 -left-24 w-80 h-80 rounded-full bg-white/5" />
        <div className="absolute -bottom-20 -right-20 w-96 h-96 rounded-full bg-white/5" />
        <div className="absolute top-1/2 -right-16 w-64 h-64 rounded-full bg-ucsg-crimson-700/60" />

        <div className="relative flex items-center gap-4">
          <div className="w-10 h-10 border-2 border-white/80 rounded-full flex items-center justify-center text-xl">
            &#10011;
          </div>
          <div className="leading-tight">
            <p className="font-extrabold text-xl tracking-wide">UCSG</p>
            <p className="text-[0.65rem] tracking-[3px] uppercase opacity-80">Eventos</p>
          </div>
        </div>

        <div className="relative">
          <h1 className="font-serif text-[2.8rem] font-semibold leading-[1.1] mb-5">
            Tu campus,<br />en un solo lugar.
          </h1>
          <p className="text-white/75 text-[1.05rem] leading-relaxed max-w-xs">
            Accede con tu cuenta institucional para guardar recordatorios y estar al tanto de todos los eventos universitarios.
          </p>
        </div>

        <p className="relative text-white/40 text-xs">
          &copy; {new Date().getFullYear()} Universidad Catolica de Santiago de Guayaquil
        </p>
      </div>

      {/* Panel derecho */}
      <div className="flex-1 flex flex-col items-center justify-center bg-ucsg-warm px-6 py-12">

        {/* Logo movil */}
        <div className="flex items-center gap-3 mb-10 lg:hidden">
          <div className="w-9 h-9 border-2 border-ucsg-crimson rounded-full flex items-center justify-center text-ucsg-crimson text-lg">
            &#10011;
          </div>
          <div className="leading-tight">
            <p className="font-extrabold text-ucsg-crimson text-lg tracking-wide">UCSG</p>
            <p className="text-[0.6rem] tracking-[2.5px] uppercase text-ucsg-brown-400">Eventos</p>
          </div>
        </div>

        <div className="w-full max-w-[420px]">

          {/* Encabezado */}
          <div className="mb-8">
            <h2 className="font-serif text-[2rem] font-semibold text-ucsg-brown-900 leading-tight mb-1">
              Iniciar sesion
            </h2>
            <p className="text-ucsg-brown-400 text-[0.95rem]">
              Ingresa con tu cuenta institucional.
            </p>
          </div>

          {/* Banner cuenta bloqueada */}
          {isLocked && (
            <div className="mb-5 px-4 py-4 rounded-xl bg-red-100 border-2 border-red-400 text-red-800 text-[0.9rem] flex flex-col gap-1 animate-ucsg-rise">
              <div className="flex items-center gap-2 font-bold text-[0.95rem]">
                <span>&#128274;</span>
                <span>Cuenta bloqueada</span>
              </div>
              <p className="text-red-700 text-[0.85rem] leading-snug">
                Tu cuenta ha sido bloqueada por demasiados intentos fallidos.
                Contacta al administrador para desbloquearla.
              </p>
            </div>
          )}

          {/* Banner error credenciales */}
          {error && !isLocked && (
            <div className="mb-4 px-4 py-3 rounded-xl bg-amber-50 border border-amber-300 text-amber-800 text-[0.9rem] font-medium flex items-start gap-3 animate-ucsg-rise">
              <span className="mt-px text-base">&#9888;&#65039;</span>
              <span>{error}</span>
            </div>
          )}

          {/* Barra de intentos */}
          {attempts > 0 && !isLocked && (
            <div className="mb-5 flex gap-1.5" aria-label={`Intentos restantes: ${remaining}`}>
              {Array.from({ length: MAX_ATTEMPTS }).map((_, i) => (
                <div
                  key={i}
                  className={`flex-1 h-1.5 rounded-full transition-colors duration-300 ${
                    i < attempts ? 'bg-red-400' : 'bg-ucsg-border'
                  }`}
                />
              ))}
            </div>
          )}

          {/* Formulario */}
          <form onSubmit={handleSubmit} noValidate className="flex flex-col gap-5">

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
                onChange={(e) => handleEmailChange(e.target.value)}
                placeholder="correo@cu.ucsg.edu.ec"
                disabled={loading || isLocked}
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

            <div>
              <label
                htmlFor="password"
                className="block text-[0.82rem] font-semibold text-ucsg-brown mb-[6px] uppercase tracking-[0.8px]"
              >
                Contrasena
              </label>
              <div className="relative">
                <input
                  id="password"
                  type={showPass ? 'text' : 'password'}
                  autoComplete="current-password"
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="&bull;&bull;&bull;&bull;&bull;&bull;&bull;&bull;"
                  disabled={loading || isLocked}
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
                  disabled={isLocked}
                  className="
                    absolute right-3 top-1/2 -translate-y-1/2
                    text-ucsg-brown-400 hover:text-ucsg-brown
                    text-lg transition-colors select-none
                    disabled:opacity-40
                  "
                  aria-label={showPass ? 'Ocultar contrasena' : 'Mostrar contrasena'}
                >
                  {showPass ? '\uD83D\uDE48' : '\uD83D\uDC41'}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading || isLocked || !email || !password}
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
                  Verificando...
                </>
              ) : isLocked ? (
                'Cuenta bloqueada'
              ) : (
                'Iniciar sesion'
              )}
            </button>
          </form>

          <p className="mt-8 text-center text-[0.88rem] text-ucsg-brown-400">
            No tienes cuenta?{' '}
            <Link href="/register" className="text-ucsg-crimson font-semibold hover:underline">
              Crear cuenta
            </Link>
          </p>
          <p className="mt-3 text-center text-[0.88rem] text-ucsg-brown-400">
            <Link
              href="/"
              className="inline-flex items-center gap-1 text-ucsg-brown-400 hover:text-ucsg-crimson transition-colors hover:underline"
            >
              &#8592; Ver cartelera p&#250;blica
            </Link>
          </p>
        </div>
      </div>
    </div>
  )
}

function SpinnerIcon() {
  return (
    <svg
      className="animate-spin h-5 w-5 text-white"
      xmlns="http://www.w3.org/2000/svg"
      fill="none"
      viewBox="0 0 24 24"
    >
      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
      <path
        className="opacity-75"
        fill="currentColor"
        d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"
      />
    </svg>
  )
}
