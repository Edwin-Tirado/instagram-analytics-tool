'use client'

import { useCallback, useEffect, useRef, useState } from 'react'

// ⚠️ MODO PRUEBA: 1 minuto total, aviso 20 segundos antes
// Para producción cambiar a: IDLE_TIMEOUT_MS = 5 * 60 * 1000  y  WARNING_BEFORE_MS = 60 * 1000
const IDLE_TIMEOUT_MS   = 1 * 60 * 1000   // 1 minuto
const WARNING_BEFORE_MS = 20 * 1000        // aviso 20 seg antes del cierre

/**
 * Hook que detecta inactividad del usuario y expone señales para:
 * - mostrar un modal de advertencia (isWarning=true) cuando queda WARNING_BEFORE_MS
 * - cerrar la sesión (onIdle) cuando se agota IDLE_TIMEOUT_MS
 *
 * Arquitectura basada en refs puras: los event listeners se registran UNA sola
 * vez en el mount, y todas las funciones leen enabled/onIdle desde refs en lugar
 * de closures — eliminando completamente los bugs de stale closure.
 *
 * IMPORTANTE: mientras el modal de advertencia está visible, los eventos
 * de actividad NO reinician el timer — solo el botón "Seguir conectado"
 * puede hacerlo (llamando a resetTimer() directamente).
 *
 * @param onIdle  Función que se invoca al confirmar inactividad (cierra sesión).
 * @param enabled Permite deshabilitar el timer (ej.: usuario no autenticado).
 */
export function useIdleTimer(onIdle: () => void, enabled = true) {
  const [isWarning,        setIsWarning]        = useState(false)
  const [secondsRemaining, setSecondsRemaining] = useState(0)

  // ── Refs: siempre tienen el valor actual, sin stale closures ─────────────
  const enabledRef    = useRef(enabled)
  const onIdleRef     = useRef(onIdle)
  const isWarningRef  = useRef(false)
  const idleTimer     = useRef<ReturnType<typeof setTimeout>  | null>(null)
  const warnTimer     = useRef<ReturnType<typeof setTimeout>  | null>(null)
  const countdownRef  = useRef<ReturnType<typeof setInterval> | null>(null)

  // Mantener las refs sincronizadas con los props en cada render
  useEffect(() => { enabledRef.current = enabled }, [enabled])
  useEffect(() => { onIdleRef.current  = onIdle  }, [onIdle])

  // ── Helpers internos (no cambian nunca — deps vacías) ────────────────────

  const clearAllTimers = useCallback(() => {
    if (idleTimer.current)    clearTimeout(idleTimer.current)
    if (warnTimer.current)    clearTimeout(warnTimer.current)
    if (countdownRef.current) clearInterval(countdownRef.current)
    idleTimer.current    = null
    warnTimer.current    = null
    countdownRef.current = null
  }, [])

  const startCountdown = useCallback(() => {
    setSecondsRemaining(Math.round(WARNING_BEFORE_MS / 1000))
    if (countdownRef.current) clearInterval(countdownRef.current)
    countdownRef.current = setInterval(() => {
      setSecondsRemaining(prev => {
        if (prev <= 1) {
          if (countdownRef.current) clearInterval(countdownRef.current)
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }, [])

  /**
   * Reinicia los timers de inactividad.
   * Lee `enabledRef` en tiempo de ejecución → nunca tiene stale closure.
   */
  const resetTimer = useCallback(() => {
    if (!enabledRef.current) return

    clearAllTimers()
    setIsWarning(false)
    isWarningRef.current = false

    // Timer de advertencia (aparece WARNING_BEFORE_MS antes del cierre)
    warnTimer.current = setTimeout(() => {
      setIsWarning(true)
      isWarningRef.current = true
      startCountdown()
    }, IDLE_TIMEOUT_MS - WARNING_BEFORE_MS)

    // Timer de cierre de sesión definitivo
    idleTimer.current = setTimeout(() => {
      clearAllTimers()
      setIsWarning(false)
      isWarningRef.current = false
      onIdleRef.current()   // lee onIdle en tiempo de ejecución → siempre fresco
    }, IDLE_TIMEOUT_MS)
  }, [clearAllTimers, startCountdown]) // ← NO depende de enabled ni onIdle: los lee por ref

  // ── Suscripción de eventos de actividad (una sola vez al montar) ─────────
  useEffect(() => {
    const EVENTS = [
      'mousemove', 'mousedown', 'keydown',
      'touchstart', 'scroll', 'click',
    ] as const

    const handler = () => {
      // Ignorar actividad si el modal de aviso está abierto —
      // solo el botón "Seguir conectado" puede reiniciar el timer.
      if (isWarningRef.current) return
      // Ignorar si el timer está deshabilitado (usuario no autenticado)
      if (!enabledRef.current) return
      resetTimer()
    }

    EVENTS.forEach(ev => window.addEventListener(ev, handler, { passive: true }))

    // Arrancar el timer en el primer render
    // Si enabled aún es false (user no cargado), resetTimer lo ignorará
    // y cuando enabled pase a true, el siguiente efecto lo arrancará.
    resetTimer()

    return () => {
      EVENTS.forEach(ev => window.removeEventListener(ev, handler))
      clearAllTimers()
    }
  // Solo corre UNA vez al montar — resetTimer y clearAllTimers son estables (deps: [])
  // enabled y onIdle se leen por ref, no por closure.
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // ── Arrancar/detener el timer cuando enabled cambia ──────────────────────
  useEffect(() => {
    if (enabled) {
      resetTimer()   // usuario acaba de autenticarse → arrancar
    } else {
      clearAllTimers()          // sesión cerrada → parar todo
      setIsWarning(false)
      isWarningRef.current = false
    }
  }, [enabled, resetTimer, clearAllTimers])

  return { isWarning, secondsRemaining, resetTimer }
}
