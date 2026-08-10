'use client'

import { Clock, ImageOff, MapPin, Star } from 'lucide-react'
import EventImage from './EventImage'
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
          <EventImage src={event.imageUrl} alt={event.title} className="w-full h-full" />
        ) : (
          <div className="w-full h-full flex items-center justify-center bg-ucsg-warm-100">
            <ImageOff size={40} strokeWidth={1.5} className="text-ucsg-muted" />
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
            shadow-md
          ">
            <Star size={14} strokeWidth={2} fill="currentColor" />
          </div>
        )}
      </div>

      {/* Content column */}
      <div className="px-7 py-[26px] flex-1 flex flex-col justify-center">
        <div className="flex items-center gap-2 mb-[9px]">
          <span className="
            text-[0.7rem] font-bold text-ucsg-crimson-400 uppercase
            tracking-[1.2px]
          ">
            {event.tag}
          </span>
          {event.isPast && (
            <span className="text-[0.65rem] font-bold uppercase tracking-[0.5px] bg-ucsg-warm-100 text-ucsg-muted px-2 py-[2px] rounded-full">
              Finalizado
            </span>
          )}
        </div>

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
          <span className="flex items-center gap-[6px]"><Clock size={14} strokeWidth={2.25} className="shrink-0 text-ucsg-crimson-400" /> {event.time}</span>
          <span className="flex items-center gap-[6px]"><MapPin size={14} strokeWidth={2.25} className="shrink-0 text-ucsg-crimson-400" /> {event.location}</span>
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
