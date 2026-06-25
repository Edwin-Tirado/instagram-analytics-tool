'use client'

interface CategoryFilterProps {
  categories: string[]
  active: string
  onSelect: (cat: string) => void
}

/** Normaliza 'Mis Recordatorios ⭐' → 'Mis Recordatorios' para comparar */
const normalize = (c: string) => c.replace(' ⭐', '')

export default function CategoryFilter({ categories, active, onSelect }: CategoryFilterProps) {
  return (
    <div className="flex gap-[10px] overflow-x-auto pb-[10px] mb-[34px] [-ms-overflow-style:none] [scrollbar-width:none]">
      {categories.map((cat) => {
        const isActive = normalize(active) === normalize(cat)
        return (
          <button
            key={cat}
            onClick={() => onSelect(normalize(cat))}
            className={`
              px-[22px] py-[11px] rounded-[24px]
              text-[0.85rem] font-semibold cursor-pointer whitespace-nowrap
              font-sans border transition-all duration-150
              ${isActive
                ? 'bg-ucsg-crimson border-ucsg-crimson text-white'
                : 'bg-white border-ucsg-border text-ucsg-brown-400 hover:border-ucsg-border-dark hover:text-ucsg-brown'}
            `}
          >
            {cat}
          </button>
        )
      })}
    </div>
  )
}
