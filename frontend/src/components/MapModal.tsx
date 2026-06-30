'use client'

import { useEffect } from 'react'
import EventMap from './EventMap'

interface MapModalProps {
  title: string
  lat: number
  lng: number
  onClose: () => void
}

export default function MapModal({ title, lat, lng, onClose }: MapModalProps) {
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
          bg-white rounded-[18px] w-full max-w-[500px] overflow-hidden
          shadow-modal animate-ucsg-rise-fast p-6 relative
        "
      >
        {/* Close button */}
        <button
          onClick={onClose}
          className="
            absolute top-4 right-4 bg-ucsg-warm hover:bg-ucsg-border-dark text-ucsg-brown-600
            border border-ucsg-border-dark rounded-full w-[32px] h-[32px]
            cursor-pointer text-[0.8rem] flex items-center justify-center
            transition-colors z-10
          "
        >
          ✕
        </button>

        <h3 className="font-serif text-[1.4rem] font-semibold text-ucsg-brown-900 mb-4 pr-8">
          {title}
        </h3>

        <div className="w-full h-[300px] rounded-[11px] border border-ucsg-border overflow-hidden">
          <EventMap lat={lat} lng={lng} locationName={title} />
        </div>
        
        <div className="mt-4 text-center">
          <p className="text-[0.78rem] text-ucsg-brown-400 font-medium">
            Coordenadas: {lat.toFixed(6)}, {lng.toFixed(6)}
          </p>
        </div>
      </div>
    </div>
  )
}
