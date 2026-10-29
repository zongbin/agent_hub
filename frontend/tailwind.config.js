/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}"
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#EEF2FF',
          100: '#E0E7FF',
          200: '#C7D2FE',
          300: '#A5B4FC',
          400: '#818CF8',
          500: '#4F46E5',
          600: '#4338CA',
          700: '#3730A3',
          800: '#312E81',
          900: '#1E1B4B'
        },
        /* ===== AI 科技风霓虹色板 ===== */
        ai: {
          violet: '#7C5CFF',
          indigo: '#6366F1',
          cyan: '#22D3EE',
          sky: '#38BDF8',
          pink: '#F472B6',
          lime: '#A3E635',
          amber: '#FBBF24',
          bg: '#060912',
          panel: '#0B1120',
          line: 'rgba(148,163,184,0.16)'
        },
        ink: {
          DEFAULT: '#E6ECF8',
          soft: '#C3CEE0',
          dim: '#9AA8C0',
          faint: '#64748B'
        }
      },
      fontFamily: {
        sans: ['-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'PingFang SC', 'Microsoft YaHei', 'sans-serif']
      },
      boxShadow: {
        glow: '0 0 0 1px rgba(124,92,255,0.35), 0 10px 36px -10px rgba(124,92,255,0.55)',
        'glow-cyan': '0 0 0 1px rgba(34,211,238,0.30), 0 10px 36px -12px rgba(34,211,238,0.45)',
        card: '0 18px 44px -22px rgba(0,0,0,0.85)',
        inset: 'inset 0 1px 0 0 rgba(255,255,255,0.06)'
      },
      backgroundImage: {
        'ai-gradient': 'linear-gradient(120deg, #7C5CFF 0%, #22D3EE 55%, #A3E635 100%)',
        'ai-gradient-soft': 'linear-gradient(120deg, rgba(124,92,255,0.22) 0%, rgba(34,211,238,0.18) 60%, rgba(163,230,53,0.16) 100%)'
      },
      keyframes: {
        float: {
          '0%,100%': { transform: 'translateY(0) scale(1)' },
          '50%': { transform: 'translateY(-24px) scale(1.06)' }
        },
        aurora: {
          '0%,100%': { transform: 'translate3d(-6%, -4%, 0) rotate(0deg)' },
          '50%': { transform: 'translate3d(6%, 5%, 0) rotate(12deg)' }
        },
        pulseGlow: {
          '0%,100%': { opacity: '0.55' },
          '50%': { opacity: '1' }
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' }
        },
        caret: {
          '0%,100%': { opacity: '1' },
          '50%': { opacity: '0' }
        },
        rise: {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' }
        }
      },
      animation: {
        float: 'float 8s ease-in-out infinite',
        aurora: 'aurora 16s ease-in-out infinite',
        'pulse-glow': 'pulseGlow 4s ease-in-out infinite',
        shimmer: 'shimmer 3s linear infinite',
        caret: 'caret 1s step-end infinite',
        rise: 'rise 0.4s cubic-bezier(0.22,1,0.36,1) both'
      }
    }
  },
  plugins: []
}
