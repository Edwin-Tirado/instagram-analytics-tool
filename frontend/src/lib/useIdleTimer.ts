'use client'

import { useEffect, useRef, useCallback, useState } from 'react'

// Tiempo sin actividad antes de mostrar el modal (1 min para pruebas)
const IDLE_MS = 0.5 * 60 * 1000
// Segundos que tiene el usuario para responder antes del logout automático
const CONFIRM_SEC = 10

export function useIdleTimer(onLogout: () => void, enabled = true) {
  const [showModal, setShowModal] = useState(false)
  const [countdown, setCountdown] = useState(CONFIRM_SEC)

  const idleHandle = useRef<ReturnType<typeof setTimeout> | null>(null)
  const countHandle = useRef<ReturnType<typeof setInterval> | null>(null)
  const onLogoutRef = useRef(onLogout)
  const enabledRef = useRef(enabled)

  // Mantener refs actualizadas
  useEffect(() => { onLogoutRef.current = onLogout }, [onLogout])
  useEffect(() => { enabledRef.current = enabled }, [enabled])

  const stopAll = useCallback(() => {
    if (idleHandle.current) clearTimeout(idleHandle.current)
    if (countHandle.current) clearInterval(countHandle.current)
    idleHandle.current = null
    countHandle.current = null
  }, [])

  const doLogout = useCallback(() => {
    stopAll()
    setShowModal(false)
    onLogoutRef.current()
  }, [stopAll])

  /** Abre el modal y arranca la cuenta regresiva de 10 seg */
  const openModal = useCallback(() => {
    setShowModal(true)
    setCountdown(CONFIRM_SEC)
    if (countHandle.current) clearInterval(countHandle.current)
    countHandle.current = setInterval(() => {
      setCountdown(prev => {
        if (prev <= 1) {
          clearInterval(countHandle.current!)
          countHandle.current = null
          doLogout()
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }, [doLogout])

  /** Reinicia el timer de inactividad (llamado al detectar actividad) */
  const resetIdle = useCallback(() => {
    if (!enabledRef.current) return
    if (idleHandle.current) clearTimeout(idleHandle.current)
    idleHandle.current = setTimeout(openModal, IDLE_MS)
  }, [openModal])

  /** El usuario dice "Sí, sigo aquí" → cerrar modal y reiniciar */
  const stayLoggedIn = useCallback(() => {
    stopAll()
    setShowModal(false)
    // Reiniciar el timer de inactividad
    idleHandle.current = setTimeout(openModal, IDLE_MS)
  }, [stopAll, openModal])

  // Suscribir eventos de actividad — una sola vez al montar
  useEffect(() => {
    const EVENTS = ['mousemove', 'mousedown', 'keydown', 'touchstart', 'scroll', 'click'] as const

    const onActivity = () => {
      if (!enabledRef.current) return
      if (idleHandle.current) clearTimeout(idleHandle.current)
      idleHandle.current = setTimeout(openModal, IDLE_MS)
    }

    EVENTS.forEach(ev => window.addEventListener(ev, onActivity, { passive: true }))

    // Arrancar al montar
    if (enabledRef.current) {
      idleHandle.current = setTimeout(openModal, IDLE_MS)
    }

    return () => {
      EVENTS.forEach(ev => window.removeEventListener(ev, onActivity))
      stopAll()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []) // Solo al montar — enabled y openModal se leen por ref/closure estable

  // Cuando enabled cambia (ej: user cargado desde localStorage)
  useEffect(() => {
    if (enabled) {
      // Arrancar el timer si aún no está corriendo
      if (!idleHandle.current) {
        idleHandle.current = setTimeout(openModal, IDLE_MS)
      }
    } else {
      stopAll()
      setShowModal(false)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled])

  return { showModal, countdown, stayLoggedIn, doLogout }
}
