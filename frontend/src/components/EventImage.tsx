interface EventImageProps {
  src: string
  alt?: string
  className?: string
}

/**
 * Muestra una imagen SIN recortarla ni estirarla, sin importar la relación de
 * aspecto del contenedor: un fondo desenfocado (bg-cover) rellena el espacio
 * y la imagen completa (bg-contain) se centra encima, intacta.
 */
export default function EventImage({ src, alt = '', className = '' }: EventImageProps) {
  return (
    <div className={`relative overflow-hidden ${className}`}>
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
