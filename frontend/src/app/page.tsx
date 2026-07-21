'use client'

import { useCallback, useEffect, useMemo, useState } from 'react'
import dynamic from 'next/dynamic'
import { useRouter } from 'next/navigation'
import CategoryFilter from '@/components/CategoryFilter'
import EventCard from '@/components/EventCard'
import Footer from '@/components/Footer'
import HeroCarousel from '@/components/HeroCarousel'
import Navbar from '@/components/Navbar'
import Toast from '@/components/Toast'
import { getEvents, getZones, addReminder, deleteReminder, getMyReminders } from '@/lib/api'
import { isAuthenticated } from '@/lib/auth'
import { toUIEvent } from '@/lib/eventUtils'
import { MOCK_EVENTS, HERO_SLIDES, CATEGORIES } from '@/lib/mockData'
import { EventSummary, ReminderMinutes, UIEvent, Zone } from '@/types'

// Nombre de la zona genérica de respaldo (V12__rebuild_zones_from_docx.sql) —
// no es una instalación real que valga la pena mostrar como link del footer.
const GENERIC_ZONE_NAME = 'Campus Universitario'

/** Reparte los nombres de zonas en columnas alfabéticas para el footer. */
function splitIntoColumns(names: string[], numCols: number) {
  const sorted = [...names].sort((a, b) => a.localeCompare(b, 'es'))
  const perCol = Math.ceil(sorted.length / numCols) || 1
  const columns: { title: string; items: string[] }[] = []
  for (let i = 0; i < numCols; i++) {
    const slice = sorted.slice(i * perCol, (i + 1) * perCol)
    if (slice.length === 0) continue
    const first = slice[0][0]?.toUpperCase() ?? ''
    const last  = slice[slice.length - 1][0]?.toUpperCase() ?? ''
    columns.push({ title: first === last ? first : `${first} – ${last}`, items: slice })
  }
  return columns
}

// Cargas dinámicas para evitar que Leaflet intente ejecutarse en SSR
const EventModal = dynamic(() => import('@/components/EventModal'), { ssr: false })
const MapModal   = dynamic(() => import('@/components/MapModal'), { ssr: false })
const CampusMap  = dynamic(() => import('@/components/CampusMap'), { ssr: false })


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
  const router = useRouter()
  const [rawEvents, setRawEvents]       = useState<EventSummary[]>(MOCK_EVENTS)
  const [loading, setLoading]           = useState(false)
  const [activeCategory, setActiveCategory] = useState('Todos')
  const [selectedId, setSelectedId]     = useState<string | null>(null)
  const [reminders, setReminders]       = useState<Set<string>>(new Set())
  const [reminderMap, setReminderMap]   = useState<Map<string, string>>(new Map()) // eventId → reminderId
  const [toast, setToast]               = useState(false)
  const [toastMsg, setToastMsg]         = useState('✅ Guardado en tus recordatorios')
  const [activeMapFacility, setActiveMapFacility] = useState<{ title: string; lat: number; lng: number } | null>(null)
  const [zones, setZones]               = useState<Zone[]>([])
  const [view, setView]                 = useState<'lista' | 'mapa'>('lista')

  // Zonas del footer — en vivo desde el backend, así que cualquier ubicación
  // nueva que se agregue desde "Editar evento" aparece acá automáticamente.
  useEffect(() => {
    getZones().then(setZones).catch(() => {})
  }, [])

  const footerColumns = useMemo(() => {
    const names = zones.map(z => z.name).filter(name => name !== GENERIC_ZONE_NAME)
    return splitIntoColumns(names, 4)
  }, [zones])

  // Evita hydration mismatch: isAuthenticated() lee localStorage, que no existe en SSR.
  // Se evalúa únicamente en el cliente, tras el montaje del componente.
  const [authenticated, setAuthenticated] = useState(false)
  useEffect(() => { setAuthenticated(isAuthenticated()) }, [])

  // ── Carga inicial de eventos desde el backend ────────────────────────────
  useEffect(() => {
    setLoading(true)
    getEvents()
      .then((page) => { if (page.content.length > 0) setRawEvents(page.content) })
      .catch(() => { /* usa mockData como fallback */ })
      .finally(() => setLoading(false))
  }, [])

  // Carga recordatorios del usuario autenticado (solo si hay sesión)
  useEffect(() => {
    if (!authenticated) return // sin sesión → no llamar al backend
    getMyReminders()
      .then((list) => {
        const ids  = new Set(list.map((r) => r.eventId))
        const map  = new Map(list.map((r) => [r.eventId, r.id]))
        setReminders(ids)
        setReminderMap(map)
      })
      .catch(() => { /* error silencioso */ })
  }, [authenticated])

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

      // Guard: redirigir al login si el usuario no tiene sesión activa
      if (!isAuthenticated()) {
        router.push('/login')
        return
      }

      const isActive = reminders.has(selectedId)

      if (isActive) {
        const rid = reminderMap.get(selectedId)
        if (!rid) return
        try {
          await deleteReminder(rid)
          setReminderMap((m) => { const n = new Map(m); n.delete(selectedId); return n })
          setReminders((prev) => { const n = new Set(prev); n.delete(selectedId); return n })
          showToast('🗑️ Recordatorio eliminado')
        } catch (e: any) {
          // Antes esto se tragaba el error y el toast decía "eliminado" igual,
          // aunque el backend lo hubiera rechazado.
          showToast(`⚠️ ${e.message ?? 'No se pudo eliminar el recordatorio'}`)
        }
      } else {
        try {
          const res = await addReminder({ eventId: selectedId, minutesBefore: minutes })
          setReminderMap((m) => new Map(m).set(selectedId, res.id))
          setReminders((prev) => new Set(prev).add(selectedId))
          showToast('✅ Guardado en tus recordatorios')
        } catch (e: any) {
          // Antes esto se tragaba el error (ej. 409 por duplicado, o evento ya
          // finalizado) y el toast decía "Guardado" igual, mintiéndole al usuario.
          showToast(`⚠️ ${e.message ?? 'No se pudo guardar el recordatorio'}`)
        }
      }
    },
    [selectedId, reminders, reminderMap],
  )

  const handleOpenFacilityMap = useCallback((item: string) => {
    const zone = zones.find(z => z.name === item)
    if (zone?.latitude != null && zone?.longitude != null) {
      setActiveMapFacility({
        title: item,
        lat: zone.latitude,
        lng: zone.longitude,
      })
    }
  }, [zones])

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
          categories={authenticated ? CATEGORIES : CATEGORIES.filter(c => !c.includes('Recordatorios'))}
          active={activeCategory}
          onSelect={setActiveCategory}
        />

        {/* Alternar entre vista de lista y mapa del campus */}
        <div className="flex gap-[10px] mb-[26px]">
          {([
            { key: 'lista', label: '🗒️ Lista' },
            { key: 'mapa',  label: '🗺️ Mapa del Campus' },
          ] as const).map(({ key, label }) => (
            <button
              key={key}
              onClick={() => setView(key)}
              className={`
                px-[18px] py-[9px] rounded-[24px]
                text-[0.8rem] font-bold cursor-pointer whitespace-nowrap
                border transition-all duration-150
                ${view === key
                  ? 'bg-ucsg-brown-900 border-ucsg-brown-900 text-white'
                  : 'bg-white border-ucsg-border text-ucsg-brown-400 hover:border-ucsg-border-dark hover:text-ucsg-brown'}
              `}
            >
              {label}
            </button>
          ))}
        </div>

        {view === 'mapa' ? (
          <CampusMap
            events={filtered.filter((ev) => !ev.isPast && ev.coordinates)}
            zones={zones}
            onSelectEvent={setSelectedId}
          />
        ) : filtered.length > 0 ? (
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

      <Footer columns={footerColumns} onItemClick={handleOpenFacilityMap} />

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

      {/* Map modal for footer spaces */}
      {activeMapFacility && (
        <MapModal
          title={activeMapFacility.title}
          lat={activeMapFacility.lat}
          lng={activeMapFacility.lng}
          onClose={() => setActiveMapFacility(null)}
        />
      )}
    </div>
  )
}
