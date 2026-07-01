'use client'

import { useEffect } from 'react'
import { UIEvent, ReminderMinutes } from '@/types'
import EventMap from './EventMap'

interface EventModalProps {
  event: UIEvent
  onClose: () => void
  onToggleReminder: (minutes: ReminderMinutes) => void
}

export default function EventModal({ event, onClose, onToggleReminder }: EventModalProps) {

  // Cerrar con Escape
  useEffect(() => {
    const handler = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [onClose])

  return (
    <div
      onClick={onClose}
      className="
        fixed inset-0 bg-[rgba(20,15,13,0.7)] backdrop-blur-[6px]
        z-[100] flex items-center justify-center p-6 animate-ucsg-fade
      "
    >
      <div
        onClick={(e) => e.stopPropagation()}
        className="
          bg-white rounded-[18px] w-full max-w-[620px] max-h-[92vh] overflow-y-auto
          shadow-modal animate-ucsg-rise-fast
        "
      >
        {/* Hero image */}
        <div className="relative">
          {event.imageUrl ? (
            <div
              className="w-full h-[240px] bg-cover bg-center"
              style={{ backgroundImage: `url(${event.imageUrl})` }}
            />
          ) : (
            <div className="w-full h-[240px] bg-ucsg-warm-100 flex items-center justify-center">
              <span className="text-ucsg-muted text-6xl">📸</span>
            </div>
          )}
          <div className="absolute inset-0 bg-gradient-to-b from-transparent via-transparent to-[rgba(20,15,13,0.55)]" />

          {/* Close button */}
          <button
            onClick={onClose}
            className="
              absolute top-4 right-4 bg-black/55 text-white
              border-none rounded-full w-[38px] h-[38px]
              cursor-pointer text-[1.1rem] flex items-center justify-center
              hover:bg-black/75 transition-colors
            "
          >
            ✕
          </button>

          {/* Category tag */}
          <span className="
            absolute bottom-4 left-[26px] text-[0.7rem] font-bold text-white
            bg-ucsg-crimson px-[13px] py-[6px] rounded-[20px]
            uppercase tracking-[1px]
          ">
            {event.tag}
          </span>
        </div>

        {/* Body */}
        <div className="px-8 py-[30px] pb-[34px]">
          <h2 className="
            font-serif text-[1.85rem] font-semibold mb-[18px]
            text-ucsg-brown-900 leading-[1.15]
          ">
            {event.title}
          </h2>

          {/* Date / Location info box */}
          <div className="
            flex flex-col gap-[9px] bg-ucsg-warm p-[18px] rounded-[11px]
            mb-[22px] text-[0.92rem] font-semibold text-ucsg-brown
            border-l-4 border-ucsg-crimson
          ">
            <span className="flex items-center gap-[9px]">📅 {event.fullDate}</span>
            <span className="flex items-center gap-[9px]">📍 {event.location}</span>
          </div>

          {/* Description */}
          <p className="text-[1rem] leading-[1.65] text-ucsg-brown-600 mb-[26px]">
            {event.full}
          </p>

          {/* Mapa interactivo */}
          <p className="
            text-[0.74rem] font-bold mb-[11px] text-ucsg-brown-200
            uppercase tracking-[1px]
          ">
            Ubicación en el campus
          </p>
          <div className="
            w-full h-[180px] rounded-[11px] border border-ucsg-border mb-[26px]
            overflow-hidden
          ">
            {event.coordinates ? (
              <EventMap
                lat={event.coordinates.lat}
                lng={event.coordinates.lng}
                locationName={event.location}
              />
            ) : (
              <div className="
                w-full h-full bg-ucsg-warm-100 flex items-center justify-center
                [background-image:radial-gradient(#d8cfc8_1px,transparent_1px)]
                [background-size:22px_22px]
              ">
                <div className="
                  w-[30px] h-[30px] bg-ucsg-crimson rounded-[50%_50%_50%_0]
                  rotate-[-45deg] shadow-[0_0_0_7px_rgba(155,14,62,0.18)]
                  relative
                ">
                  <div className="absolute top-2 left-2 w-[14px] h-[14px] bg-white rounded-full" />
                </div>
              </div>
            )}
          </div>

          {/* ── Sección de recordatorio ─────────────────────────────── */}
          {event.reminded ? (
            /* Estado: recordatorio activo → botón para cancelar */
            <button
              onClick={() => onToggleReminder(15)}
              className="
                w-full py-4 text-white border-none rounded-[11px]
                text-[1.02rem] font-bold cursor-pointer font-sans
                bg-ucsg-success flex items-center justify-center gap-[9px]
                hover:bg-ucsg-success-800 transition-colors
              "
            >
              ✔️ Recordatorio activado — toca para cancelar
            </button>
          ) : (
            /* Estado: sin recordatorio → un clic directo crea el recordatorio */
            <button
              onClick={() => onToggleReminder(15)}
              className="
                w-full py-4 text-white border-none rounded-[11px]
                text-[1.02rem] font-bold cursor-pointer font-sans
                bg-ucsg-crimson flex items-center justify-center gap-[9px]
                hover:bg-ucsg-crimson-700 active:scale-[0.98]
                transition-all duration-150
              "
            >
              🔔 Recordarme este evento
            </button>
          )}
        </div>
      </div>
    </div>
  )
}
