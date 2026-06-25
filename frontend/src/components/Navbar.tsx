'use client'

interface NavbarProps {
  onProfileClick?: () => void
}

export default function Navbar({ onProfileClick }: NavbarProps) {
  return (
    <header className="
      bg-ucsg-crimson text-white h-[72px]
      flex items-center justify-between px-[6%]
      sticky top-0 z-50
      shadow-[0_1px_0_rgba(0,0,0,0.08)]
    ">
      {/* Logo */}
      <div className="flex items-center gap-[13px]">
        <div className="
          w-[38px] h-[38px] border border-white/85 rounded-full
          flex items-center justify-center text-[1.15rem]
        ">
          ✛
        </div>
        <div className="flex flex-col leading-[1.05]">
          <span className="font-extrabold text-[1.18rem] tracking-[0.5px]">UCSG</span>
          <span className="font-normal text-[0.7rem] tracking-[2.5px] uppercase opacity-[0.82]">
            Eventos
          </span>
        </div>
      </div>

      {/* Nav links */}
      <nav className="flex gap-[30px] items-center">
        {['Cartelera', 'Facultades', 'Deportes'].map((link) => (
          <a
            key={link}
            href="#"
            className="
              text-white/90 no-underline text-[0.78rem] font-semibold
              uppercase tracking-[1px]
              hover:text-white transition-colors
            "
          >
            {link}
          </a>
        ))}
        <button
          onClick={onProfileClick}
          className="
            bg-white text-ucsg-crimson
            px-5 py-[9px] rounded-[22px]
            uppercase font-extrabold text-[0.72rem] tracking-[0.8px]
            border-none cursor-pointer font-sans whitespace-nowrap
            hover:bg-ucsg-pink transition-colors
          "
        >
          Mi Perfil
        </button>
      </nav>
    </header>
  )
}
