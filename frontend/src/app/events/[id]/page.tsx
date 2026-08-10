'use client'

import { useCallback, useEffect, useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import dynamic from 'next/dynamic'
import { Bell, Calendar, CheckCircle2, ImageOff, MapPin, SearchX } from 'lucide-react'
import EventImage from '@/components/EventImage'
import Navbar from '@/components/Navbar'
import { addReminder, deleteReminder, getEventById, getMyReminders } from '@/lib/api'
import { isAuthenticated } from '@/lib/auth'
import { Event } from '@/types'

// Leaflet toca el DOM directamente — se carga solo en cliente, igual que en EventModal/MapModal.
const EventMap = dynamic(() => import('@/components/EventMap'), { ssr: false })

function formatFullDate(iso: string | null): string {
  if (!iso) return 'Fecha por confirmar'
  return new Date(iso).toLocaleDateString('es-EC', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  })
}

export default function EventDetailPage() {
  const params = useParams<{ id: string }>()
  const router = useRouter()
  const eventId = params.id

  const [event, setEvent]     = useState<Event | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState<string | null>(null)

  const [reminderId, setReminderId] = useState<string | null>(null)
  const [reminderBusy, setReminderBusy] = useState(false)
  const [toast, setToast] = useState<string | null>(null)
  const [activeImage, setActiveImage] = useState(0)

  useEffect(() => {
    let cancelled = false
    setLoading(true); setError(null)

    getEventById(eventId)
      .then(ev => { if (!cancelled) { setEvent(ev); setActiveImage(0) } })
      .catch(e => { if (!cancelled) setError(e.message ?? 'No se pudo cargar el evento') })
      .finally(() => { if (!cancelled) setLoading(false) })

    // Si el usuario está autenticado, revisa si ya tiene un recordatorio para este evento
    if (isAuthenticated()) {
      getMyReminders()
        .then(reminders => {
          if (cancelled) return
          const existing = reminders.find(r => r.eventId === eventId)
          setReminderId(existing?.id ?? null)
        })
        .catch(() => {})
    }

    return () => { cancelled = true }
  }, [eventId])

  const handleToggleReminder = useCallback(async () => {
    if (!isAuthenticated()) { router.push('/login'); return }
    setReminderBusy(true)
    try {
      if (reminderId) {
        await deleteReminder(reminderId)
        setReminderId(null)
        setToast('Recordatorio eliminado')
      } else {
        const res = await addReminder({ eventId, minutesBefore: 15 })
        setReminderId(res.id)
        setToast('Guardado en tus recordatorios')
      }
    } catch (e: any) {
      setToast(e.message ?? 'No se pudo actualizar el recordatorio')
    } finally {
      setReminderBusy(false)
      setTimeout(() => setToast(null), 3000)
    }
  }, [eventId, reminderId, router])

  return (
    <div className="min-h-screen flex flex-col bg-ucsg-warm">
      <Navbar />

      <main className="flex-1 px-[6%] py-10">
        <div className="max-w-[720px] mx-auto">
          <Link
            href="/"
            className="inline-flex items-center gap-1 text-ucsg-brown-400 hover:text-ucsg-crimson transition-colors text-[0.9rem] mb-6"
          >
            ← Volver a la cartelera
          </Link>

          {loading && (
            <div className="bg-white rounded-[18px] p-10 text-center text-ucsg-brown-400 shadow-sm">
              Cargando evento…
            </div>
          )}

          {!loading && error && (
            <div className="bg-white rounded-[18px] p-10 text-center shadow-sm">
              <SearchX size={48} strokeWidth={1.5} className="mx-auto mb-3 text-ucsg-muted" />
              <h1 className="font-serif text-[1.4rem] font-semibold text-ucsg-brown-900 mb-2">
                Evento no encontrado
              </h1>
              <p className="text-ucsg-brown-400 text-sm">
                Puede que el evento aún no esté aprobado o el enlace ya no sea válido.
              </p>
            </div>
          )}

          {!loading && !error && event && (
            <div className="bg-white rounded-[18px] overflow-hidden shadow-modal">
              {/* Carrusel de imágenes — event.imageUrls trae TODAS las del carrusel de Instagram */}
              {(() => {
                const images = event.imageUrls?.length ? event.imageUrls : (event.thumbnailUrl ? [event.thumbnailUrl] : [])
                if (images.length === 0) {
                  return (
                    <div className="w-full h-[280px] bg-ucsg-warm-100 flex items-center justify-center">
                      <ImageOff size={56} strokeWidth={1.5} className="text-ucsg-muted" />
                    </div>
                  )
                }
                return (
                  <div className="relative w-full h-[280px] bg-ucsg-warm-100 overflow-hidden group">
                    <EventImage src={images[activeImage]} alt={event.title} className="w-full h-full" />
                    {images.length > 1 && (
                      <>
                        <button
                          onClick={() => setActiveImage(i => (i - 1 + images.length) % images.length)}
                          aria-label="Imagen anterior"
                          className="absolute left-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-black/45 text-white flex items-center justify-center hover:bg-black/65 transition-colors"
                        >
                          ‹
                        </button>
                        <button
                          onClick={() => setActiveImage(i => (i + 1) % images.length)}
                          aria-label="Siguiente imagen"
                          className="absolute right-3 top-1/2 -translate-y-1/2 w-9 h-9 rounded-full bg-black/45 text-white flex items-center justify-center hover:bg-black/65 transition-colors"
                        >
                          ›
                        </button>
                        <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5">
                          {images.map((_, i) => (
                            <button
                              key={i}
                              onClick={() => setActiveImage(i)}
                              aria-label={`Ir a la imagen ${i + 1}`}
                              className={`w-2 h-2 rounded-full transition-colors ${i === activeImage ? 'bg-white' : 'bg-white/50'}`}
                            />
                          ))}
                        </div>
                        <span className="absolute top-3 right-3 bg-black/45 text-white text-xs font-semibold px-2 py-1 rounded-full">
                          {activeImage + 1}/{images.length}
                        </span>
                      </>
                    )}
                  </div>
                )
              })()}

              <div className="px-8 py-[30px] pb-[34px]">
                <h1 className="font-serif text-[1.85rem] font-semibold mb-[18px] text-ucsg-brown-900 leading-[1.15]">
                  {event.title}
                </h1>

                {/* Fecha / Ubicación */}
                <div className="flex flex-col gap-[9px] bg-ucsg-warm p-[18px] rounded-[11px] mb-[22px] text-[0.92rem] font-semibold text-ucsg-brown border-l-4 border-ucsg-crimson">
                  <span className="flex items-center gap-[9px]"><Calendar size={16} strokeWidth={2.25} className="shrink-0" /> {formatFullDate(event.eventDate)}</span>
                  <span className="flex items-center gap-[9px]">
                    <MapPin size={16} strokeWidth={2.25} className="shrink-0" /> {event.zone?.name ?? event.zoneName ?? 'Campus UCSG'}
                    {event.locationText ? ` — ${event.locationText}` : ''}
                  </span>
                </div>

                {/* Descripción */}
                {event.caption && (
                  <p className="text-[1rem] leading-[1.65] text-ucsg-brown-600 mb-[26px] whitespace-pre-line">
                    {event.caption}
                  </p>
                )}

                {/* Mapa */}
                {(event.zone?.latitude != null && event.zone?.longitude != null) && (
                  <>
                    <p className="text-[0.74rem] font-bold mb-[11px] text-ucsg-brown-200 uppercase tracking-[1px]">
                      Ubicación en el campus
                    </p>
                    <div className="w-full h-[280px] rounded-[11px] border border-ucsg-border mb-[26px] overflow-hidden">
                      <EventMap
                        lat={event.zone.latitude}
                        lng={event.zone.longitude}
                        locationName={event.zone.name}
                      />
                    </div>
                  </>
                )}

                {/* Recordatorio */}
                <button
                  onClick={handleToggleReminder}
                  disabled={reminderBusy}
                  className={`w-full py-4 text-white border-none rounded-[11px] text-[1.02rem] font-bold cursor-pointer font-sans flex items-center justify-center gap-[9px] transition-colors disabled:opacity-60 ${
                    reminderId
                      ? 'bg-ucsg-success hover:bg-ucsg-success-800'
                      : 'bg-ucsg-crimson hover:bg-ucsg-crimson-700'
                  }`}
                >
                  {reminderId
                    ? <><CheckCircle2 size={18} strokeWidth={2.25} /> Recordatorio activado — toca para cancelar</>
                    : <><Bell size={18} strokeWidth={2.25} /> Recordarme este evento</>}
                </button>

                {toast && (
                  <p className="text-center text-sm text-ucsg-brown-400 mt-3">{toast}</p>
                )}
              </div>
            </div>
          )}
        </div>
      </main>
    </div>
  )
}
