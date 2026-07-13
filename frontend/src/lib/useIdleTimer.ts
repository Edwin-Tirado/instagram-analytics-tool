'use client'

import { useEffect, useRef, useCallback, useState } from 'react'

// Tiempo sin actividad antes de mostrar el modal
// ⚠️ PRUEBA: 30 segundos — en producción usar: 5 * 60 * 1000
const IDLE_MS     = 30 * 1000
// Segundos para responder antes del logout automático
const CONFIRM_SEC = 10

export function useIdleTimer(onLogout: () => void, enabled = true) {
  const [showModal, setShowModal] = useState(false)
  const [countdown, setCountdown] = useState(CONFIRM_SEC)

  // ── Refs (siempre actualizadas, sin stale closures) ──────────────────────
  const idleHandle   = useRef<ReturnType<typeof setTimeout>  | null>(null)
  const countHandle  = useRef<ReturnType<typeof setInterval> | null>(null)
  const onLogoutRef  = useRef(onLogout)
  const enabledRef   = useRef(enabled)
  const openModalRef = useRef<() => void>(() => {})   // se rellena más abajo

  useEffect(() => { onLogoutRef.current = onLogout }, [onLogout])
  useEffect(() => { enabledRef.current  = enabled  }, [enabled])

  // ── Detener todos los timers ──────────────────────────────────────────────
  const stopAll = useCallback(() => {
    if (idleHandle.current)  clearTimeout(idleHandle.current)
    if (countHandle.current) clearInterval(countHandle.current)
    idleHandle.current  = null
    countHandle.current = null
  }, [])

  // ── Logout inmediato ──────────────────────────────────────────────────────
  const doLogout = useCallback(() => {
    stopAll()
    setShowModal(false)
    onLogoutRef.current()
  }, [stopAll])

  // ── Abrir modal + arrancar cuenta atrás ───────────────────────────────────
  const openModal = useCallback(() => {
    setShowModal(true)
    setCountdown(CONFIRM_SEC)
    if (countHandle.current) clearInterval(countHandle.current)

    let secs = CONFIRM_SEC
    countHandle.current = setInterval(() => {
      secs -= 1
      setCountdown(secs)
      if (secs <= 0) {
        clearInterval(countHandle.current!)
        countHandle.current = null
        // Leer onLogoutRef en tiempo de ejecución — nunca stale
        stopAll()
        setShowModal(false)
        onLogoutRef.current()
      }
    }, 1000)
  }, [stopAll])

  // Mantener la ref de openModal siempre actualizada
  useEffect(() => { openModalRef.current = openModal }, [openModal])

  // ── "Sí, sigo aquí" ───────────────────────────────────────────────────────
  const stayLoggedIn = useCallback(() => {
    stopAll()
    setShowModal(false)
    idleHandle.current = setTimeout(() => openModalRef.current(), IDLE_MS)
  }, [stopAll])

  // ── Suscripción de eventos — una sola vez al montar ───────────────────────
  useEffect(() => {
    const EVENTS = ['mousemove', 'mousedown', 'keydown', 'touchstart', 'scroll', 'click'] as const

    const onActivity = () => {
      // Si el modal ya está abierto, no resetear (el usuario debe elegir Sí/No)
      if (!enabledRef.current) return
      if (idleHandle.current) clearTimeout(idleHandle.current)
      // Usar siempre la versión más fresca de openModal a través de la ref
      idleHandle.current = setTimeout(() => openModalRef.current(), IDLE_MS)
    }

    EVENTS.forEach(ev => window.addEventListener(ev, onActivity, { passive: true }))

    return () => {
      EVENTS.forEach(ev => window.removeEventListener(ev, onActivity))
      stopAll()
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []) // Solo al montar. openModal se lee siempre por openModalRef.

  // ── Arrancar / detener cuando enabled cambia ──────────────────────────────
  useEffect(() => {
    if (enabled) {
      // Arrancar solo si no hay ya un timer corriendo
      if (!idleHandle.current) {
        idleHandle.current = setTimeout(() => openModalRef.current(), IDLE_MS)
      }
    } else {
      stopAll()
      setShowModal(false)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [enabled])

  return { showModal, countdown, stayLoggedIn, doLogout }
}
