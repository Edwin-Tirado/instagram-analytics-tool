'use client'

import { useCallback, useEffect, useState } from 'react'
import CategoryFilter from '@/components/CategoryFilter'
import EventCard from '@/components/EventCard'
import EventModal from '@/components/EventModal'
import Footer from '@/components/Footer'
import HeroCarousel from '@/components/HeroCarousel'
import Navbar from '@/components/Navbar'
import Toast from '@/components/Toast'
import { getEvents, addReminder, deleteReminder, getMyReminders } from '@/lib/api'
import { toUIEvent } from '@/lib/eventUtils'
import { MOCK_EVENTS, HERO_SLIDES, FOOTER_COLS, CATEGORIES } from '@/lib/mockData'
import { EventSummary, ReminderMinutes, UIEvent } from '@/types'

// ── Constantes de categoría ──────────────────────────────────────────────────

const TAG_GROUPS: Record<string, string[]> = {
  'Académicos':     ['Facultad de Ciencias Médicas', 'Facultad de Ingeniería', 'Posgrados', 'Ingeniería'],
  'Arte y Cultura': ['Arte y Cultura', 'Aula Magna', 'Sinfónica'],
  'Deportes':       ['Cancha', 'Deportes', 'Fútbol', 'Piscina', 'Gimnasio'],
}

function deriveGroup(tag: string): string {
  const t = tag.toLowerCase()
  for (const [group, keywords] of Object.entries(TAG_GROUPS)) {
    if (keywords.some((k) => t.includes(k.toLowerCase()))) return group
  }
  return 'Académicos'
}

// ── Page component ───────────────────────────────────────────────────────────

export default function HomePage() {
  const [rawEvents, setRawEvents]       = useState<EventSummary[]>(MOCK_EVENTS)
  const [loading, setLoading]           = useState(false)
  const [activeCategory, setActiveCategory] = useState('Todos')
  const [selectedId, setSelectedId]     = useState<string | null>(null)
  const [reminders, setReminders]       = useState<Set<string>>(new Set())
  const [reminderMap, setReminderMap]   = useState<Map<string, string>>(new Map()) // eventId → reminderId
  const [toast, setToast]               = useState(false)
  const [toastMsg, setToastMsg]         = useState('✅ Guardado en tus recordatorios')

  // ── Carga inicial de eventos desde el backend ────────────────────────────
  useEffect(() => {
    setLoading(true)
    getEvents()
      .then((page) => { if (page.content.length > 0) setRawEvents(page.content) })
      .catch(() => { /* usa mockData como fallback */ })
      .finally(() => setLoading(false))
  }, [])

  // ── Carga recordatorios del usuario autenticado ──────────────────────────
  useEffect(() => {
    getMyReminders()
      .then((list) => {
        const ids  = new Set(list.map((r) => r.eventId))
        const map  = new Map(list.map((r) => [r.eventId, r.id]))
        setReminders(ids)
        setReminderMap(map)
      })
      .catch(() => { /* no autenticado — reminders vacíos */ })
  }, [])

  // ── Construir UIEvents ───────────────────────────────────────────────────
  const uiEvents: UIEvent[] = rawEvents.map((ev) =>
    toUIEvent(ev, reminders.has(ev.id)),
  )

  // ── Filtro por categoría ─────────────────────────────────────────────────
  const filtered = uiEvents.filter((ev) => {
    if (activeCategory === 'Todos') return true
    if (activeCategory === 'Mis Recordatorios') return ev.reminded
    return deriveGroup(ev.tag) === activeCategory
  })

  const selectedEvent = selectedId ? uiEvents.find((e) => e.id === selectedId) ?? null : null

  // ── Handlers ─────────────────────────────────────────────────────────────

  const showToast = (msg: string) => {
    setToastMsg(msg)
    setToast(true)
    setTimeout(() => setToast(false), 3000)
  }

  const handleToggleReminder = useCallback(
    async (minutes: ReminderMinutes) => {
      if (!selectedId) return
      const isActive = reminders.has(selectedId)

      if (isActive) {
        const rid = reminderMap.get(selectedId)
        if (rid) {
          await deleteReminder(rid).catch(() => null)
          setReminderMap((m) => { const n = new Map(m); n.delete(selectedId); return n })
        }
        setReminders((prev) => { const n = new Set(prev); n.delete(selectedId); return n })
        showToast('🗑️ Recordatorio eliminado')
      } else {
        const res = await addReminder({ eventId: selectedId, minutesBefore: minutes })
          .catch(() => null)
        if (res) {
          setReminderMap((m) => new Map(m).set(selectedId, res.id))
        }
        setReminders((prev) => new Set(prev).add(selectedId))
        showToast('✅ Guardado en tus recordatorios')
      }
    },
    [selectedId, reminders, reminderMap],
  )

  const countLabel = filtered.length === 1 ? '1 evento' : `${filtered.length} eventos`

  return (
    <div className="min-h-screen flex flex-col bg-ucsg-warm">
      <Navbar />

      <HeroCarousel slides={HERO_SLIDES} />

      <main className="flex-1 px-[6%] pt-14 pb-10 max-w-[1180px] w-full mx-auto">
        {/* Section header */}
        <div className="flex items-end justify-between gap-6 mb-2 flex-wrap">
          <h1 className="font-serif text-[2.6rem] font-semibold text-ucsg-brown-900 leading-[1.05]">
            Cartelera Universitaria
          </h1>
          <span className="text-[0.85rem] text-ucsg-crimson font-bold tracking-[0.3px]">
            {loading ? 'Cargando…' : countLabel}
          </span>
        </div>
        <p className="text-ucsg-brown-400 text-[1.02rem] max-w-[620px] mb-[30px] leading-relaxed">
          Explora y participa en las actividades académicas y extracurriculares de la comunidad universitaria.
        </p>

        <CategoryFilter
          categories={CATEGORIES}
          active={activeCategory}
          onSelect={setActiveCategory}
        />

        {/* Event list */}
        {filtered.length > 0 ? (
          <div className="flex flex-col gap-5">
            {filtered.map((ev) => (
              <EventCard
                key={ev.id}
                event={ev}
                onClick={() => setSelectedId(ev.id)}
              />
            ))}
          </div>
        ) : (
          <div className="
            text-center py-[70px] px-5 text-ucsg-brown-200
            border-[1.5px] border-dashed border-ucsg-border-dark rounded-2xl
          ">
            <div className="text-[2.4rem] mb-3">☆</div>
            <p className="text-[1.02rem] font-semibold text-ucsg-brown-400">
              {activeCategory === 'Mis Recordatorios'
                ? 'Aún no tienes eventos guardados'
                : 'No hay eventos en esta categoría'}
            </p>
            <p className="text-[0.9rem] mt-[6px]">
              {activeCategory === 'Mis Recordatorios'
                ? 'Abre un evento y pulsa «Recordarme» para verlo aquí.'
                : 'Prueba seleccionando otra categoría.'}
            </p>
          </div>
        )}
      </main>

      <Footer columns={FOOTER_COLS} />

      {/* Modal */}
      {selectedEvent && (
        <EventModal
          event={selectedEvent}
          onClose={() => setSelectedId(null)}
          onToggleReminder={handleToggleReminder}
        />
      )}

      {/* Toast notification */}
      <Toast visible={toast} message={toastMsg} />
    </div>
  )
}
