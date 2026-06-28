'use client'
import Link from 'next/link'

export default function ForbiddenPage() {
  return (
    <div className="min-h-screen bg-[#f9f6f1] flex items-center justify-center p-6">
      <div className="text-center max-w-md">
        <div className="text-8xl font-black text-[#931934] mb-4">403</div>
        <h1 className="text-2xl font-bold text-[#2d1b0e] mb-2">Acceso denegado</h1>
        <p className="text-[#7a6652] mb-8">
          No tienes permisos para ver esta página. Contacta al administrador si crees que esto es un error.
        </p>
        <Link
          href="/"
          className="inline-block bg-[#931934] text-white px-6 py-3 rounded-lg font-semibold hover:bg-[#7a1528] transition-colors"
        >
          Volver al inicio
        </Link>
      </div>
    </div>
  )
}
