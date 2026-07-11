'use client'

interface Props {
  secondsRemaining: number
  onStayLoggedIn: () => void
  onLogout: () => void
}

/**
 * Modal que se muestra cuando el usuario lleva ~4 minutos inactivo.
 * Muestra una cuenta regresiva y dos acciones:
 *   - "Seguir conectado" → reinicia el timer de inactividad
 *   - "Cerrar sesión"   → cierra la sesión de inmediato
 */
export default function IdleWarningModal({ secondsRemaining, onStayLoggedIn, onLogout }: Props) {
  const minutes = Math.floor(secondsRemaining / 60)
  const seconds = secondsRemaining % 60
  const timeStr = minutes > 0
    ? `${minutes}:${String(seconds).padStart(2, '0')} min`
    : `${seconds} seg`

  // Porcentaje para el anillo de progreso (60 seg → 100 %)
  const pct = Math.min(100, (secondsRemaining / 60) * 100)
  const radius = 28
  const circumference = 2 * Math.PI * radius
  const dashOffset = circumference * (1 - pct / 100)

  return (
    <div
      className="fixed inset-0 z-[9999] flex items-center justify-center"
      style={{ backdropFilter: 'blur(4px)', backgroundColor: 'rgba(0,0,0,0.45)' }}
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="idle-title"
      aria-describedby="idle-desc"
    >
      <div
        className="bg-white rounded-2xl shadow-2xl p-8 max-w-sm w-full mx-4 flex flex-col items-center gap-6"
        style={{ animation: 'fadeScaleIn 0.2s ease-out' }}
      >
        {/* Anillo de cuenta regresiva */}
        <div className="relative w-20 h-20">
          <svg className="w-full h-full -rotate-90" viewBox="0 0 64 64">
            {/* Pista */}
            <circle
              cx="32" cy="32" r={radius}
              fill="none"
              stroke="#f3e8e8"
              strokeWidth="6"
            />
            {/* Progreso */}
            <circle
              cx="32" cy="32" r={radius}
              fill="none"
              stroke="#931934"
              strokeWidth="6"
              strokeLinecap="round"
              strokeDasharray={circumference}
              strokeDashoffset={dashOffset}
              style={{ transition: 'stroke-dashoffset 1s linear' }}
            />
          </svg>
          <span
            className="absolute inset-0 flex items-center justify-center text-lg font-bold text-[#931934]"
            aria-live="polite"
            aria-atomic="true"
          >
            {String(seconds).padStart(2, '0')}
          </span>
        </div>

        {/* Texto */}
        <div className="text-center space-y-1">
          <h2 id="idle-title" className="text-xl font-bold text-[#2d1b0e]">
            ¿Sigues ahí?
          </h2>
          <p id="idle-desc" className="text-sm text-[#7a6652] leading-relaxed">
            Tu sesión cerrará automáticamente en{' '}
            <strong className="text-[#931934]">{timeStr}</strong>{' '}
            por inactividad.
          </p>
        </div>

        {/* Acciones */}
        <div className="flex gap-3 w-full">
          <button
            id="idle-logout-btn"
            onClick={onLogout}
            className="flex-1 px-4 py-2.5 rounded-xl border border-[#e8ddd4] text-[#7a6652] text-sm font-semibold hover:border-[#931934] hover:text-[#931934] transition-colors"
          >
            Cerrar sesión
          </button>
          <button
            id="idle-stay-btn"
            onClick={onStayLoggedIn}
            autoFocus
            className="flex-1 px-4 py-2.5 rounded-xl bg-[#931934] text-white text-sm font-semibold hover:bg-[#7a1528] transition-colors"
          >
            Seguir conectado
          </button>
        </div>
      </div>

      <style>{`
        @keyframes fadeScaleIn {
          from { opacity: 0; transform: scale(0.92); }
          to   { opacity: 1; transform: scale(1);    }
        }
      `}</style>
    </div>
  )
}
