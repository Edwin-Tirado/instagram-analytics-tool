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
 * Usa un <img> oculto para detectar fallos de carga; el fondo desenfocado
 * (bg-cover) rellena el espacio y la imagen completa (bg-contain) se centra.
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
      {/* Imagen oculta para detectar errores de carga */}
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img
        src={src}
        alt=""
        className="hidden"
        onError={() => setFailed(true)}
        aria-hidden="true"
      />
      <div
        className="absolute inset-0 bg-cover bg-center scale-110 blur-md opacity-50"
        style={{ backgroundImage: `url(${src})` }}
        aria-hidden="true"
      />
      <div
        className="absolute inset-0 bg-contain bg-center bg-no-repeat"
        style={{ backgroundImage: `url(${src})` }}
        role="img"
        aria-label={alt}
      />
    </div>
  )
}
