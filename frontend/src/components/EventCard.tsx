'use client'

import { UIEvent } from '@/types'

interface EventCardProps {
  event: UIEvent
  onClick: () => void
}

export default function EventCard({ event, onClick }: EventCardProps) {
  return (
    <article
      onClick={onClick}
      className="
        flex bg-white rounded-2xl border border-ucsg-border overflow-hidden
        shadow-card cursor-pointer min-h-[208px]
        transition-all duration-200
        hover:-translate-y-[3px] hover:shadow-card-hover hover:border-ucsg-border-dark
        group
      "
    >
      {/* Image column */}
      <div className="relative w-[296px] min-w-[296px] bg-ucsg-warm-100">
        {event.imageUrl ? (
          <div
            className="w-full h-full bg-cover bg-center"
            style={{ backgroundImage: `url(${event.imageUrl})` }}
          />
        ) : (
          <div className="w-full h-full flex items-center justify-center bg-ucsg-warm-100">
            <span className="text-ucsg-muted text-4xl">📸</span>
          </div>
        )}

        {/* Date badge */}
        <div className="
          absolute top-4 left-4 bg-white rounded-[11px] text-center
          w-[58px] shadow-date overflow-hidden
        ">
          <span className="
            bg-ucsg-crimson text-white text-[0.66rem] font-bold
            uppercase tracking-[0.5px] py-[5px] block
          ">
            {event.month}
          </span>
          <span className="
            text-ucsg-brown-900 text-[1.45rem] font-extrabold
            py-[6px] block font-serif
          ">
            {event.day}
          </span>
        </div>

        {/* Reminder indicator */}
        {event.reminded && (
          <div className="
            absolute top-4 right-4 bg-ucsg-success/95 text-white
            w-[30px] h-[30px] rounded-full flex items-center justify-center
            text-[0.85rem] shadow-md
          ">
            ★
          </div>
        )}
      </div>

      {/* Content column */}
      <div className="px-7 py-[26px] flex-1 flex flex-col justify-center">
        <span className="
          text-[0.7rem] font-bold text-ucsg-crimson-400 uppercase
          mb-[9px] tracking-[1.2px]
        ">
          {event.tag}
        </span>

        <h3 className="
          font-serif text-[1.5rem] text-ucsg-brown-900 mb-[13px]
          leading-[1.15] font-semibold
        ">
          {event.title}
        </h3>

        <div className="
          flex gap-5 text-[0.86rem] text-ucsg-brown-400 mb-[13px]
          font-medium flex-wrap
        ">
          <span className="flex items-center gap-[6px]">🕒 {event.time}</span>
          <span className="flex items-center gap-[6px]">📍 {event.location}</span>
        </div>

        <p className="
          text-[0.93rem] leading-[1.55] text-ucsg-brown-600
          line-clamp-2
        ">
          {event.short}
        </p>
      </div>
    </article>
  )
}
