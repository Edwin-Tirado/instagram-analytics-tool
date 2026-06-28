import { EventSummary, UIEvent } from '@/types'

const MONTHS_ES = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic']
const DAYS_ES   = ['domingo','lunes','martes','miércoles','jueves','viernes','sábado']

export function toUIEvent(ev: EventSummary, reminded: boolean): UIEvent {
  const date = ev.eventDate ? new Date(ev.eventDate) : null

  const month   = date ? MONTHS_ES[date.getMonth()] : '—'
  const day     = date ? String(date.getDate()).padStart(2, '0') : '—'
  const time    = date
    ? date.toLocaleTimeString('es-EC', { hour: '2-digit', minute: '2-digit' })
    : 'Por confirmar'
  const fullDate = date
    ? `${DAYS_ES[date.getDay()].charAt(0).toUpperCase() + DAYS_ES[date.getDay()].slice(1)} ${date.getDate()} de ${MONTHS_ES[date.getMonth()]} de ${date.getFullYear()} · ${time}`
    : 'Fecha por confirmar'

  // El "tag" de categoría se deriva del nombre de la zona o del texto de ubicación
  const tag = ev.zone?.name ?? ev.zoneName ?? ev.locationText ?? 'Universidad'

  // El resumen del caption: primera 150 caracteres sin el bloque de hashtags
  const cleaned = (ev.caption ?? ev.title)
    .replace(/#\S+/g, '')
    .replace(/\n{2,}/g, '\n')
    .trim()
  const short = cleaned.length > 160 ? cleaned.slice(0, 157) + '…' : cleaned

  const lat = ev.zone?.latitude ?? ev.latitude
  const lng = ev.zone?.longitude ?? ev.longitude
  const coordinates = lat != null && lng != null ? { lat, lng } : undefined

  return {
    id:        ev.id,
    title:     ev.title,
    tag,
    month,
    day,
    time,
    fullDate,
    location:  ev.locationText ?? ev.zone?.name ?? ev.zoneName ?? 'Campus UCSG',
    short,
    full:      cleaned,
    imageUrl:  ev.images?.[0]?.mediaUrl ?? ev.thumbnailUrl ?? null,
    eventDate: date,
    reminded,
    coordinates,
  }
}
