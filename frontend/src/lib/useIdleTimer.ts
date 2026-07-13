'use client'

import { useCallback, useEffect, useRef, useState } from 'react'

// Tiempo total de inactividad antes del cierre (5 minutos)
const IDLE_TIMEOUT_MS   = 5 * 60 * 1000
// Cuándo mostrar el aviso previo al cierre (1 minuto antes)
const WARNING_BEFORE_MS = 60 * 1000

/**
 * Hook que detecta inactividad del usuario y expone señales para:
 * - mostrar un modal de advertencia (isWarning=true) cuando queda WARNING_BEFORE_MS
 * - cerrar la sesión (onIdle) cuando se agota IDLE_TIMEOUT_MS
 *
 * Los eventos que reinician el temporizador son: mousemove, mousedown,
 * keydown, touchstart y scroll.
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

  // Refs para poder cancelar/reiniciar sin recrear listeners
  const idleTimer    = useRef<ReturnType<typeof setTimeout> | null>(null)
  const warnTimer    = useRef<ReturnType<typeof setTimeout> | null>(null)
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const onIdleRef    = useRef(onIdle)
  // Rastrear si el modal de aviso está activo para ignorar eventos de actividad
  const isWarningRef = useRef(false)

  // Mantener la referencia a onIdle actualizada sin re-suscribir eventos
  useEffect(() => { onIdleRef.current = onIdle }, [onIdle])

  // Sincronizar isWarningRef con el estado
  useEffect(() => { isWarningRef.current = isWarning }, [isWarning])

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

  const resetTimer = useCallback(() => {
    if (!enabled) return
    clearAllTimers()
    setIsWarning(false)
    isWarningRef.current = false

    // Timer de advertencia
    warnTimer.current = setTimeout(() => {
      setIsWarning(true)
      isWarningRef.current = true
      startCountdown()
    }, IDLE_TIMEOUT_MS - WARNING_BEFORE_MS)

    // Timer de cierre de sesión
    idleTimer.current = setTimeout(() => {
      clearAllTimers()
      setIsWarning(false)
      isWarningRef.current = false
      onIdleRef.current()
    }, IDLE_TIMEOUT_MS)
  }, [enabled, clearAllTimers, startCountdown])

  // Suscribir eventos de actividad del usuario
  useEffect(() => {
    if (!enabled) return

    const EVENTS = [
      'mousemove', 'mousedown', 'keydown',
      'touchstart', 'scroll', 'click',
    ] as const

    // Si el modal de aviso está abierto, ignorar la actividad —
    // solo el botón "Seguir conectado" puede reiniciar el timer.
    const handler = () => {
      if (isWarningRef.current) return
      resetTimer()
    }

    EVENTS.forEach(ev => window.addEventListener(ev, handler, { passive: true }))
    resetTimer() // arrancar al montar

    return () => {
      EVENTS.forEach(ev => window.removeEventListener(ev, handler))
      clearAllTimers()
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled]) // Intencionalmente omitimos resetTimer/clearAllTimers para evitar re-suscripciones
                // innecesarias; resetTimer/clearAllTimers son estables gracias a useCallback([]).

  return { isWarning, secondsRemaining, resetTimer }
}
