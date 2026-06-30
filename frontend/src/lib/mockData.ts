/**
 * Datos de muestra idénticos al prototipo HTML.
 * Se usan mientras el backend no está disponible o el token no está configurado.
 * Reemplaza con la llamada real a getEvents() cuando el backend esté levantado.
 */
import { EventSummary } from '@/types'

export const MOCK_EVENTS: EventSummary[] = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    title: 'Casa Abierta de Medicina y Ciencias de la Salud',
    caption: 'Simulaciones médicas, chequeos gratuitos y exposición de proyectos anatómicos por parte de los estudiantes de internado. Ven y conoce de cerca el trabajo de nuestros estudiantes. Habrá simulaciones médicas, toma de signos vitales gratuita y charlas sobre prevención.',
    locationText: 'Plazoleta Central',
    zone: { id: 1, name: 'Plazoleta Central', description: null, latitude: -2.182485, longitude: -79.904347 },
    eventDate: '2026-08-15T09:00:00',
    status: 'APPROVED',
    images: [{ id: 1, mediaUrl: 'https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?w=600', displayOrder: 0 }],
    createdAt: '2026-07-01T00:00:00',
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    title: 'Concierto Sinfónico y Coro UCSG',
    caption: 'Nuestra orquesta sinfónica universitaria rinde homenaje a la música ecuatoriana. Entrada gratuita para estudiantes y personal administrativo presentando su credencial.',
    locationText: 'Aula Magna',
    zone: { id: 2, name: 'Aula Magna', description: null, latitude: -2.180823, longitude: -79.904107 },
    eventDate: '2026-08-22T18:30:00',
    status: 'APPROVED',
    images: [{ id: 2, mediaUrl: 'https://images.unsplash.com/photo-1514320291840-2e0a9bf2a9ae?w=600', displayOrder: 0 }],
    createdAt: '2026-07-02T00:00:00',
  },
  {
    id: '33333333-3333-3333-3333-333333333333',
    title: 'Copa Interfacultades de Fútbol',
    caption: 'Arranca la temporada deportiva con la gran final entre las facultades. Habrá animación, hinchada organizada y premiación al final del encuentro. Acompaña a tu facultad y vive la pasión del fútbol universitario.',
    locationText: 'Cancha de Césped Sintético',
    zone: { id: 3, name: 'Cancha Deportiva', description: null, latitude: -2.183800, longitude: -79.905500 },
    eventDate: '2026-08-28T16:00:00',
    status: 'APPROVED',
    images: [{ id: 3, mediaUrl: 'https://images.unsplash.com/photo-1574629810360-7efbbe195018?w=600', displayOrder: 0 }],
    createdAt: '2026-07-03T00:00:00',
  },
  {
    id: '44444444-4444-4444-4444-444444444444',
    title: 'Congreso de Ingeniería e Inteligencia Artificial',
    caption: 'Una jornada completa con ponencias de expertos nacionales e internacionales sobre inteligencia artificial aplicada, ciberseguridad y desarrollo de software. Incluye talleres prácticos y feria de proyectos estudiantiles.',
    locationText: 'Auditorio Leonidas Ortega',
    zone: { id: 6, name: 'Facultad de Ingeniería', description: null, latitude: -2.181166, longitude: -79.904848 },
    eventDate: '2026-09-05T08:30:00',
    status: 'APPROVED',
    images: [{ id: 4, mediaUrl: 'https://images.unsplash.com/photo-1677756119517-756a188d2d94?w=600', displayOrder: 0 }],
    createdAt: '2026-07-04T00:00:00',
  },
]

export const HERO_SLIDES = [
  {
    imageUrl: 'https://images.unsplash.com/photo-1523050854058-8df90110c9f1?w=1200',
    title: 'Formando verdaderos líderes',
    subtitle: 'Descubre todo lo que pasa en nuestro campus.',
  },
  {
    imageUrl: 'https://images.unsplash.com/photo-1541339907198-e08756dedf3f?w=1200',
    title: 'Vive la cartelera universitaria',
    subtitle: 'Eventos académicos, culturales y deportivos en un solo lugar.',
  },
  {
    imageUrl: 'https://images.unsplash.com/photo-1564981797816-1043664bf78d?w=1200',
    title: 'No te pierdas nada',
    subtitle: 'Guarda tus recordatorios y recibe notificaciones a tiempo.',
  },
]

export const FOOTER_COLS = [
  {
    title: 'Campus Principal',
    items: ['Facultades de Ingeniería', 'Facultades de Medicina', 'Edificio de Posgrados', 'Clínica Odontológica'],
  },
  {
    title: 'Auditorios',
    items: ['Aula Magna', 'Auditorio Leonidas Ortega', 'Salón de Usos Múltiples', 'Salas de Conferencias IT'],
  },
  {
    title: 'Deportes y Recreación',
    items: ['Canchas Múltiples', 'Cancha de Césped Sintético', 'Piscina Universitaria', 'Gimnasio UCSG'],
  },
  {
    title: 'Servicios Estudiantiles',
    items: ['Biblioteca General', 'Medios UCSG (Radio/TV)', 'Capilla Universitaria', 'CoWorking Space'],
  },
]

export const CATEGORIES = ['Todos', 'Académicos', 'Arte y Cultura', 'Deportes', 'Mis Recordatorios ⭐']

export const FACILITY_COORDINATES: Record<string, { lat: number; lng: number }> = {
  // Campus Principal
  'Facultades de Ingeniería': { lat: -2.181166, lng: -79.904848 },
  'Facultades de Medicina':    { lat: -2.179836, lng: -79.904533 },
  'Edificio de Posgrados':     { lat: -2.180424, lng: -79.903901 },
  'Clínica Odontológica':      { lat: -2.180018, lng: -79.904153 },

  // Auditorios
  'Aula Magna':                { lat: -2.180823, lng: -79.904107 },
  'Auditorio Leonidas Ortega': { lat: -2.181285, lng: -79.904630 },
  'Salón de Usos Múltiples':    { lat: -2.182190, lng: -79.904420 },
  'Salas de Conferencias IT':  { lat: -2.181340, lng: -79.904910 },

  // Deportes y Recreación
  'Canchas Múltiples':         { lat: -2.183200, lng: -79.905100 },
  'Cancha de Césped Sintético': { lat: -2.183800, lng: -79.905500 },
  'Piscina Universitaria':      { lat: -2.183500, lng: -79.905300 },
  'Gimnasio UCSG':             { lat: -2.183000, lng: -79.905000 },

  // Servicios Estudiantiles
  'Biblioteca General':        { lat: -2.181543, lng: -79.904322 },
  'Medios UCSG (Radio/TV)':    { lat: -2.182800, lng: -79.904600 },
  'Capilla Universitaria':     { lat: -2.182350, lng: -79.904000 },
  'CoWorking Space':           { lat: -2.181950, lng: -79.904250 },
}

