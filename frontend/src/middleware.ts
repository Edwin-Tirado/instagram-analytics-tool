import { NextRequest, NextResponse } from 'next/server'

const ROLE_COOKIE = 'ucsg_role'

export function middleware(req: NextRequest) {
  const { pathname } = req.nextUrl
  const role = req.cookies.get(ROLE_COOKIE)?.value ?? ''

  // ── Rutas de administrador ────────────────────────────────────────
  // /admin/dashboard es compartido: ADMIN y SUPERVISOR tienen acceso.
  // El componente en cliente controla qué acciones ve cada rol.
  if (pathname.startsWith('/admin')) {
    if (!role) return NextResponse.redirect(new URL('/login', req.url))
    if (role !== 'ROLE_ADMIN' && role !== 'ROLE_SUPERVISOR') {
      return NextResponse.redirect(new URL('/403', req.url))
    }
  }

  // ── Rutas de supervisor ───────────────────────────────────────────
  if (pathname.startsWith('/supervisor')) {
    if (!role) return NextResponse.redirect(new URL('/login', req.url))
    if (role !== 'ROLE_ADMIN' && role !== 'ROLE_SUPERVISOR') {
      return NextResponse.redirect(new URL('/403', req.url))
    }
  }

  return NextResponse.next()
}

export const config = {
  matcher: ['/admin/:path*', '/supervisor/:path*'],
}
