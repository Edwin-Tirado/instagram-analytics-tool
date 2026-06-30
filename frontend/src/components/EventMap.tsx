'use client'

import { useEffect, useRef, useState } from 'react'

interface EventMapProps {
  lat: number
  lng: number
  locationName: string
}

// Cargamos Leaflet íntegramente dentro de useEffect para que NUNCA se evalúe
// en el servidor ni en el grafo estático de módulos de Next.js 15 / Turbopack.
// Esto elimina el "Element type is invalid… got: undefined" que ocurre cuando
// L o el CSS de Leaflet se importan a nivel de módulo.
export default function EventMap({ lat, lng, locationName }: EventMapProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  // Referencia a la instancia del mapa para destruirla en cleanup
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  const mapInstanceRef = useRef<any>(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    if (!containerRef.current) return

    // Flag de cancelación: evita actualizaciones de estado si el efecto se
    // limpió (modal cerrado) antes de que la Promise de import() resuelva.
    let cancelled = false

    import('leaflet').then((mod) => {
      if (cancelled || !containerRef.current) return

      const L = mod.default

      // Inyectar CSS de Leaflet una sola vez en el documento
      if (!document.querySelector('link[data-leaflet-css]')) {
        const link = document.createElement('link')
        link.rel  = 'stylesheet'
        link.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'
        link.setAttribute('data-leaflet-css', '1')
        document.head.appendChild(link)
      }

      // Parchear íconos: el bundler no resuelve las rutas internas de
      // leaflet/dist/images, por lo que apuntamos directo al CDN de Leaflet.
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      delete (L.Icon.Default.prototype as any)._getIconUrl
      L.Icon.Default.mergeOptions({
        iconUrl:       'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon.png',
        iconRetinaUrl: 'https://unpkg.com/leaflet@1.9.4/dist/images/marker-icon-2x.png',
        shadowUrl:     'https://unpkg.com/leaflet@1.9.4/dist/images/marker-shadow.png',
      })

      const map = L.map(containerRef.current).setView([lat, lng], 17)

      L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
      }).addTo(map)

      L.marker([lat, lng])
        .addTo(map)
        .bindPopup(locationName)
        .openPopup()

      mapInstanceRef.current = map
      if (!cancelled) setReady(true)

      // Invalida el tamaño del mapa una vez estabilizada la animación del modal
      setTimeout(() => {
        if (!cancelled && map) {
          map.invalidateSize()
        }
      }, 350)
    })

    return () => {
      cancelled = true
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove()
        mapInstanceRef.current = null
        setReady(false)
      }
    }
  }, [lat, lng, locationName])

  return (
    <div className="relative w-full h-full">
      {/* Esqueleto mientras Leaflet carga */}
      {!ready && (
        <div className="
          absolute inset-0 flex items-center justify-center rounded-[11px]
          bg-ucsg-warm-100
          [background-image:radial-gradient(#d8cfc8_1px,transparent_1px)]
          [background-size:22px_22px]
        ">
          <span className="text-ucsg-brown-400 text-[0.82rem] font-medium animate-pulse">
            Cargando mapa…
          </span>
        </div>
      )}
      {/* Leaflet monta el mapa en este div directamente (sin React) */}
      <div ref={containerRef} className="w-full h-full rounded-[11px]" />
    </div>
  )
}
