interface FooterColumn {
  title: string
  items: string[]
}

interface FooterProps {
  columns: FooterColumn[]
  onItemClick?: (item: string) => void
}

export default function Footer({ columns, onItemClick }: FooterProps) {
  return (
    <footer className="
      bg-ucsg-brown-900 text-ucsg-brown-200
      pt-16 pb-8 px-[6%] mt-10
      border-t-4 border-ucsg-crimson
    ">
      <div className="max-w-[1180px] mx-auto">
        <h2 className="
          font-serif text-white text-[1.7rem] font-semibold
          mb-[42px] text-center
        ">
          Instalaciones y Espacios UCSG
        </h2>

        <div className="
          grid gap-9 border-b border-[#3a322d] pb-11
          [grid-template-columns:repeat(auto-fit,minmax(200px,1fr))]
        ">
          {columns.map((col) => (
            <div key={col.title}>
              <h3 className="
                text-white text-[0.82rem] mb-[18px] font-bold
                uppercase tracking-[1px]
                border-l-[3px] border-ucsg-crimson pl-[11px]
              ">
                {col.title}
              </h3>
              <div className="flex flex-col gap-[11px]">
                {col.items.map((item) => (
                  <button
                    key={item}
                    onClick={() => onItemClick?.(item)}
                    className="
                      bg-transparent border-none p-0 text-left cursor-pointer
                      text-ucsg-brown-100 text-[0.9rem] font-sans
                      flex items-center gap-2
                      hover:text-white transition-colors focus:outline-none
                    "
                  >
                    <span className="text-ucsg-crimson-400">→</span>
                    {item}
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>

        <div className="
          grid gap-9 sm:grid-cols-2 border-b border-[#3a322d] py-11
        ">
          <div>
            <h3 className="
              text-white text-[0.82rem] mb-[18px] font-bold
              uppercase tracking-[1px]
              border-l-[3px] border-ucsg-crimson pl-[11px]
            ">
              Contáctenos
            </h3>
            <div className="flex flex-col gap-[11px] text-ucsg-brown-100 text-[0.9rem] font-sans">
              <p>
                Teléfono:{' '}
                <a href="tel:+59343804600" className="hover:text-white transition-colors">
                  +593 4 3804600
                </a>
              </p>
              <p>
                WhatsApp:{' '}
                <a
                  href="https://wa.me/593990994445"
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-white transition-colors"
                >
                  0990994445
                </a>
              </p>
              <p>Av. Carlos Julio Arosemena Km 1 ½</p>
              <p>
                <a href="mailto:admisiones@cu.ucsg.edu.ec" className="hover:text-white transition-colors">
                  admisiones@cu.ucsg.edu.ec
                </a>
              </p>
              <p>
                <a href="mailto:info@cu.ucsg.edu.ec" className="hover:text-white transition-colors">
                  info@cu.ucsg.edu.ec
                </a>
              </p>
            </div>
          </div>

          <div className="flex items-start sm:items-end sm:justify-end gap-x-8 gap-y-3 flex-wrap">
            <span className="text-ucsg-crimson-400 text-[0.9rem] font-bold uppercase tracking-[0.5px]">
              Sala de Prensa UCSG
            </span>
            <a
              href="mailto:info@cu.ucsg.edu.ec"
              className="text-ucsg-crimson-400 text-[0.9rem] font-bold uppercase tracking-[0.5px] hover:text-white transition-colors"
            >
              Envíenos su mensaje
            </a>
          </div>
        </div>

        <p className="
          text-center pt-[26px] text-[0.82rem] text-ucsg-brown-600
        ">
          © {new Date().getFullYear()} Universidad Católica de Santiago de Guayaquil ·
          Sistema de Notificaciones Analytics
        </p>
      </div>
    </footer>
  )
}
