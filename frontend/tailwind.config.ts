import type { Config } from 'tailwindcss'

const config: Config = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        ucsg: {
          crimson:       '#9b0e3e',
          'crimson-700': '#7d0b32',
          'crimson-400': '#c21a53',
          pink:          '#f3b3c7',
          warm:          '#f7f4f1',
          'warm-100':    '#f1ece8',
          border:        '#ece7e2',
          'border-dark': '#e0d6cf',
          muted:         '#d8cfc8',
          brown:         '#3a342f',
          'brown-900':   '#1f1a17',
          'brown-600':   '#6b635c',
          'brown-400':   '#8a817b',
          'brown-200':   '#a89f98',
          'brown-100':   '#c9bfb8',
          success:       '#108f5a',
          'success-800': '#0a6642',
        },
      },
      fontFamily: {
        sans:  ['Libre Franklin', '-apple-system', 'BlinkMacSystemFont', 'sans-serif'],
        serif: ['Newsreader', 'Georgia', 'serif'],
      },
      animation: {
        'ucsg-fade':     'ucsgFade 0.8s ease both',
        'ucsg-rise':     'ucsgRise 0.6s ease both',
        'ucsg-rise-fast':'ucsgRise 0.3s ease both',
        'toast-in':      'toastIn 0.45s cubic-bezier(0.68,-0.55,0.265,1.55) both',
        'toast-out':     'toastOut 0.3s ease both',
      },
      keyframes: {
        ucsgFade: {
          from: { opacity: '0', transform: 'scale(1.04)' },
          to:   { opacity: '1', transform: 'scale(1)'    },
        },
        ucsgRise: {
          from: { opacity: '0', transform: 'translateY(24px)' },
          to:   { opacity: '1', transform: 'translateY(0)'    },
        },
        toastIn: {
          from: { opacity: '0', transform: 'translateX(-50%) translateY(40px)' },
          to:   { opacity: '1', transform: 'translateX(-50%) translateY(0)'    },
        },
        toastOut: {
          from: { opacity: '1', transform: 'translateX(-50%) translateY(0)'    },
          to:   { opacity: '0', transform: 'translateX(-50%) translateY(40px)' },
        },
      },
      boxShadow: {
        card:       '0 1px 2px rgba(31,26,23,0.04)',
        'card-hover':'0 16px 36px -16px rgba(155,14,62,0.28)',
        date:       '0 6px 16px rgba(31,26,23,0.22)',
        modal:      '0 30px 70px -20px rgba(0,0,0,0.6)',
        toast:      '0 14px 30px -8px rgba(16,143,90,0.45)',
      },
    },
  },
  plugins: [],
}

export default config
