'use client'

import { useState } from 'react'

interface EventImageProps {
  src: string
  alt?: string
  className?: string
}

/**
 * Muestra una imagen SIN recortarla ni estirarla.
 * Si la URL de Instagram expira o da error CORS, muestra un placeholder.
 *
 * Usa dos <img> reales:
 *   1. Una imagen de fondo escalada y difuminada (object-cover + blur).
 *   2. La imagen principal centrada con object-contain.
 *
 * Se usa crossOrigin="anonymous" y referrerPolicy="no-referrer" para que
 * imágenes bloqueadas por CORS/referrer (ej.: CDN de Instagram) disparen
 * correctamente onError en lugar de quedar silenciosamente en blanco.
 */
export default function EventImage({ src, alt = '', className = '' }: EventImageProps) {
  const [failed, setFailed] = useState(false)

  if (failed) {
    return (
      <div className={`relative overflow-hidden flex items-center justify-center bg-[#f5ede4] ${className}`}>
        <span className="text-5xl opacity-40">📸</span>
      </div>
    )
  }

  return (
    <div className={`relative overflow-hidden ${className}`}>
      {/* Imagen de fondo desenfocada — rellena el espacio con la misma imagen */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={src}
        alt=""
        aria-hidden="true"
        crossOrigin="anonymous"
        referrerPolicy="no-referrer"
        className="absolute inset-0 w-full h-full object-cover scale-110 blur-md opacity-50 pointer-events-none select-none"
        onError={() => setFailed(true)}
      />
      {/* Imagen principal — sin recortar, centrada, aspect-ratio original */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={src}
        alt={alt}
        crossOrigin="anonymous"
        referrerPolicy="no-referrer"
        className="absolute inset-0 w-full h-full object-contain pointer-events-none select-none"
        onError={() => setFailed(true)}
      />
    </div>
  )
}
