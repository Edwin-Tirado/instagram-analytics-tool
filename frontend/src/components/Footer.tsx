interface FooterColumn {
  title: string
  items: string[]
}

interface FooterProps {
  columns: FooterColumn[]
}

export default function Footer({ columns }: FooterProps) {
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
                  <a
                    key={item}
                    href="#"
                    className="
                      text-ucsg-brown-100 no-underline text-[0.9rem]
                      flex items-center gap-2
                      hover:text-white transition-colors
                    "
                  >
                    <span className="text-ucsg-crimson-400">→</span>
                    {item}
                  </a>
                ))}
              </div>
            </div>
          ))}
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
