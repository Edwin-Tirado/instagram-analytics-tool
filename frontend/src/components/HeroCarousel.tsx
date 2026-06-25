'use client'

import { useEffect, useState } from 'react'

interface Slide {
  imageUrl: string
  title: string
  subtitle: string
}

interface HeroCarouselProps {
  slides: Slide[]
}

export default function HeroCarousel({ slides }: HeroCarouselProps) {
  const [index, setIndex] = useState(0)

  // Auto-advance every 5 seconds
  useEffect(() => {
    const id = setInterval(
      () => setIndex((i) => (i + 1) % slides.length),
      5000,
    )
    return () => clearInterval(id)
  }, [slides.length])

  const go  = (i: number) => setIndex(i)
  const prev = () => setIndex((i) => (i - 1 + slides.length) % slides.length)
  const next = () => setIndex((i) => (i + 1) % slides.length)

  const slide = slides[index]

  return (
    <section className="relative w-full h-[380px] bg-ucsg-brown-900 overflow-hidden">

      {/* Background image */}
      <div
        key={index}
        className="absolute inset-0 bg-cover bg-center opacity-[0.62] animate-ucsg-fade"
        style={{ backgroundImage: `url(${slide.imageUrl})` }}
      />

      {/* Overlay gradient */}
      <div className="absolute inset-0 bg-gradient-to-r from-[rgba(26,20,18,0.78)] via-[rgba(26,20,18,0.25)] to-[rgba(155,14,62,0.32)]" />

      {/* Text content */}
      <div className="absolute bottom-[54px] left-[6%] right-[6%] text-white max-w-[640px] animate-ucsg-rise">
        <span className="
          inline-block text-[0.72rem] font-bold tracking-[2.5px] uppercase
          text-ucsg-pink mb-[14px]
        ">
          Universidad Católica de Santiago de Guayaquil
        </span>
        <h2 className="
          font-serif text-[3rem] font-semibold leading-[1.05]
          mb-3 [text-shadow:0_2px_18px_rgba(0,0,0,0.4)]
        ">
          {slide.title}
        </h2>
        <p className="text-[1.08rem] font-normal opacity-[0.92] [text-shadow:0_1px_8px_rgba(0,0,0,0.4)]">
          {slide.subtitle}
        </p>
      </div>

      {/* Navigation dots */}
      <div className="absolute bottom-8 right-[6%] flex gap-[9px] items-center z-10">
        {slides.map((_, i) => (
          <button
            key={i}
            onClick={() => go(i)}
            className={`
              h-[7px] rounded border-none cursor-pointer p-0 transition-all duration-300
              ${i === index
                ? 'w-[26px] bg-white'
                : 'w-[7px] bg-white/45'}
            `}
          />
        ))}
      </div>

      {/* Prev / Next arrows */}
      {[
        { label: '‹', fn: prev, side: 'left-6' },
        { label: '›', fn: next, side: 'right-6' },
      ].map(({ label, fn, side }) => (
        <button
          key={label}
          onClick={fn}
          className={`
            absolute top-1/2 -translate-y-1/2 ${side} z-10
            bg-white/[0.14] backdrop-blur-[6px] text-white
            border border-white/25 w-[46px] h-[46px] rounded-full
            text-[1.4rem] cursor-pointer flex items-center justify-center
            hover:bg-white/25 transition-colors
          `}
        >
          {label}
        </button>
      ))}
    </section>
  )
}
