'use client'

interface Props {
  countdown:     number
  onStay:        () => void
  onLogout:      () => void
}

/**
 * Modal de confirmación de inactividad.
 * Muestra una cuenta regresiva de 10 segundos y dos botones: Sí / No.
 */
export default function IdleWarningModal({ countdown, onStay, onLogout }: Props) {
  const pct          = (countdown / 10) * 100
  const radius       = 28
  const circumference = 2 * Math.PI * radius
  const dashOffset   = circumference * (1 - pct / 100)

  return (
    <div
      className="fixed inset-0 z-[9999] flex items-center justify-center"
      style={{ backdropFilter: 'blur(6px)', backgroundColor: 'rgba(0,0,0,0.50)' }}
      role="alertdialog"
      aria-modal="true"
      aria-labelledby="idle-title"
    >
      <div
        className="bg-white rounded-2xl shadow-2xl p-8 max-w-sm w-full mx-4 flex flex-col items-center gap-6"
        style={{ animation: 'idleFadeIn 0.18s ease-out' }}
      >
        {/* Anillo de cuenta regresiva */}
        <div className="relative w-20 h-20">
          <svg className="w-full h-full -rotate-90" viewBox="0 0 64 64">
            {/* Pista */}
            <circle cx="32" cy="32" r={radius} fill="none" stroke="#f3e8e8" strokeWidth="6" />
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
            className="absolute inset-0 flex items-center justify-center text-xl font-extrabold text-[#931934]"
            aria-live="polite"
            aria-atomic="true"
          >
            {countdown}
          </span>
        </div>

        {/* Texto */}
        <div className="text-center space-y-2">
          <h2 id="idle-title" className="text-2xl font-bold text-[#2d1b0e]">
            ¿Sigues ahí?
          </h2>
          <p className="text-sm text-[#7a6652] leading-relaxed">
            Tu sesión cerrará en{' '}
            <strong className="text-[#931934]">{countdown} segundo{countdown !== 1 ? 's' : ''}</strong>
            {' '}por inactividad.
          </p>
        </div>

        {/* Botones */}
        <div className="flex gap-3 w-full">
          <button
            id="idle-no-btn"
            onClick={onLogout}
            className="
              flex-1 px-4 py-3 rounded-xl border-2 border-[#e8ddd4]
              text-[#7a6652] font-semibold text-sm
              hover:border-[#931934] hover:text-[#931934]
              transition-colors
            "
          >
            No, cerrar sesión
          </button>
          <button
            id="idle-yes-btn"
            onClick={onStay}
            autoFocus
            className="
              flex-1 px-4 py-3 rounded-xl
              bg-[#931934] text-white font-semibold text-sm
              hover:bg-[#7a1528]
              transition-colors
            "
          >
            Sí, seguir
          </button>
        </div>
      </div>

      <style>{`
        @keyframes idleFadeIn {
          from { opacity: 0; transform: scale(0.90); }
          to   { opacity: 1; transform: scale(1);    }
        }
      `}</style>
    </div>
  )
}
