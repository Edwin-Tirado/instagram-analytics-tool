'use client'

interface ToastProps {
  visible: boolean
  message?: string
}

export default function Toast({ visible, message = '✅ Guardado en tus recordatorios' }: ToastProps) {
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
      {message}
    </div>
  )
}
