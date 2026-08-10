'use client'

import { CheckCircle2 } from 'lucide-react'

interface ToastProps {
  visible: boolean
  message?: string
  icon?: React.ReactNode
}

export default function Toast({ visible, message = 'Guardado en tus recordatorios', icon }: ToastProps) {
  if (!visible) return null

  return (
    <div
      className="
        fixed left-1/2 -translate-x-1/2 bottom-10 z-[200]
        bg-ucsg-success text-white
        px-[30px] py-[15px] rounded-[11px]
        text-[1rem] font-semibold shadow-toast
        flex items-center gap-[10px]
        animate-toast-in
      "
    >
      {icon ?? <CheckCircle2 size={20} strokeWidth={2.25} className="shrink-0" />}
      {message}
    </div>
  )
}
