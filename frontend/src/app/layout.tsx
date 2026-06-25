import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'UCSG Campus Analytics — Cartelera de Eventos',
  description: 'Explora y participa en las actividades académicas y extracurriculares de la comunidad UCSG.',
}

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="es">
      <body className="min-h-screen flex flex-col bg-ucsg-warm">
        {children}
      </body>
    </html>
  )
}
