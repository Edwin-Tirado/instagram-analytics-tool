'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { UIEvent, Zone } from '@/types'

interface CampusMapProps {
  /** Eventos ya filtrados: activos (no pasados) y con coordenadas. */
  events: UIEvent[]
  /** Todas las zonas del campus — solo se usan para centrar el mapa si no hay eventos activos. */
  zones: Zone[]
  onSelectEvent: (id: string) => void
}

interface ZoneMarker {
  key: string
  name: string
  lat: number
  lng: number
  events: UIEvent[]
}

function groupByZone(events: UIEvent[]): ZoneMarker[] {
  const map = new Map<string, ZoneMarker>()
  for (const ev of events) {
    if (!ev.coordinates) continue
    const key = `${ev.coordinates.lat.toFixed(5)},${ev.coordinates.lng.toFixed(5)}`
    const existing = map.get(key)
    if (existing) {
      existing.events.push(ev)
    } else {
      map.set(key, {
        key,
        name: ev.tag,
        lat: ev.coordinates.lat,
        lng: ev.coordinates.lng,
        events: [ev],
      })
    }
  }
  return Array.from(map.values())
}

// Cargamos Leaflet íntegramente dentro de useEffect — mismo patrón que
// EventMap.tsx — para que nunca se evalúe en el servidor ni en el grafo
// estático de módulos de Next.js.
export default function CampusMap({ events, zones, onSelectEvent }: CampusMapProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const mapInstanceRef = useRef<any>(null)
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const markersLayerRef = useRef<any>(null)
  const resizeObserverRef = useRef<ResizeObserver | null>(null)
  const [ready, setReady] = useState(false)

  const zoneMarkers = useMemo(() => groupByZone(events), [events])
  // Firma estable: solo cambia cuando cambia el conjunto real de eventos activos,
  // no en cada re-render del componente padre.
  const markersSignature = zoneMarkers
    .map(z => `${z.key}:${z.events.map(e => e.id).join(',')}`)
    .sort()
    .join('|')

  const onSelectEventRef = useRef(onSelectEvent)
  onSelectEventRef.current = onSelectEvent

  // ── Montaje del mapa base ────────────────────────────────────────────────
  useEffect(() => {
    if (!containerRef.current) return
    let cancelled = false

    import('leaflet').then((mod) => {
      if (cancelled || !containerRef.current) return
      const L = mod.default

      if (!document.querySelector('link[data-leaflet-css]')) {
        const link = document.createElement('link')
        link.rel  = 'stylesheet'
        link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'
        link.setAttribute('data-leaflet-css', '1')
        document.head.appendChild(link)
      }

      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      delete (L.Icon.Default.prototype as any)._getIconUrl
      L.Icon.Default.mergeOptions({
        iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
        shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
      })

      const fallbackZone = zones.find(z => z.latitude != null && z.longitude != null)
      const initialCenter: [number, number] = fallbackZone
        ? [fallbackZone.latitude as number, fallbackZone.longitude as number]
        : [0, 0]

      const map = L.map(containerRef.current).setView(initialCenter, 16)

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(map)

      mapInstanceRef.current = map
      if (!cancelled) setReady(true)

      setTimeout(() => {
        if (!cancelled && map) map.invalidateSize()
      }, 350)

      const resizeObserver = new ResizeObserver(() => map.invalidateSize())
      resizeObserver.observe(containerRef.current!)
      resizeObserverRef.current = resizeObserver
    })

    return () => {
      cancelled = true
      resizeObserverRef.current?.disconnect()
      resizeObserverRef.current = null
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
        mapInstanceRef.current = null
        markersLayerRef.current = null
        setReady(false)
      }
    }
    // Solo se remonta si cambia el set de zonas usado para el centrado inicial.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [zones.length])

  // ── Marcadores: locations con eventos activos ───────────────────────────
  useEffect(() => {
    const map = mapInstanceRef.current
    if (!map || !ready) return
    let cancelled = false

    import('leaflet').then((mod) => {
      if (cancelled) return
      const L = mod.default

      if (markersLayerRef.current) {
        markersLayerRef.current.remove()
        markersLayerRef.current = null
      }

      const layer = L.layerGroup()

      zoneMarkers.forEach((zm) => {
        const popupEl = document.createElement('div')
        popupEl.className = 'ucsg-map-popup'
        popupEl.style.minWidth = '180px'

        const title = document.createElement('div')
        title.style.fontWeight = '700'
        title.style.fontSize = '0.85rem'
        title.style.marginBottom = '6px'
        title.style.color = '#2d1b0e'
        title.textContent = `${zm.name} · ${zm.events.length} evento${zm.events.length !== 1 ? 's' : ''}`
        popupEl.appendChild(title)

        zm.events.forEach((ev) => {
          const btn = document.createElement('button')
          btn.textContent = `• ${ev.title}`
          btn.style.display = 'block'
          btn.style.width = '100%'
          btn.style.textAlign = 'left'
          btn.style.background = 'none'
          btn.style.border = 'none'
          btn.style.padding = '3px 0'
          btn.style.cursor = 'pointer'
          btn.style.color = '#931934'
          btn.style.fontSize = '0.78rem'
          btn.style.fontWeight = '600'
          btn.onclick = () => onSelectEventRef.current(ev.id)
          popupEl.appendChild(btn)
        })

        L.marker([zm.lat, zm.lng]).addTo(layer).bindPopup(popupEl)
      })

      layer.addTo(map)
      markersLayerRef.current = layer

      if (zoneMarkers.length > 0) {
        const bounds = L.latLngBounds(zoneMarkers.map(z => [z.lat, z.lng] as [number, number]))
        map.fitBounds(bounds, { padding: [50, 50], maxZoom: 17 })
      }
    })

    return () => { cancelled = true }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ready, markersSignature])

  return (
    <div className="relative isolate w-full h-[520px] rounded-2xl overflow-hidden border border-ucsg-border">
      {!ready && (
        <div className="
          absolute inset-0 flex items-center justify-center z-[1]
          bg-ucsg-warm-100
          [background-image:radial-gradient(#d8cfc8_1px,transparent_1px)]
          [background-size:22px_22px]
        ">
          <span className="text-ucsg-brown-400 text-[0.82rem] font-medium animate-pulse">
            Cargando mapa del campus…
          </span>
        </div>
      )}

      {ready && zoneMarkers.length === 0 && (
        <div className="absolute top-3 left-1/2 -translate-x-1/2 z-[400] bg-white/95 border border-ucsg-border-dark rounded-full px-4 py-2 shadow-md">
          <span className="text-[0.78rem] font-semibold text-ucsg-brown-400">
            No hay eventos activos con ubicación en este momento
          </span>
        </div>
      )}

      <div ref={containerRef} className="w-full h-full" />
    </div>
  )
}
