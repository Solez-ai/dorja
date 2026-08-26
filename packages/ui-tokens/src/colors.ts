export const colors = {
  ink: {
    950: '#0B1F33',
    800: '#17324D',
  },
  jol: {
    700: '#006B68',
    600: '#007C78', // Signature DORJA colour
    100: '#D7F1EE',
  },
  paper: {
    50: '#FBF8F2',
    100: '#F2EDE3',
  },
  sand: {
    300: '#D9CCB9',
  },
  amber: {
    500: '#E79C2E',
    100: '#FCE8BE',
  },
  leaf: {
    600: '#267450',
  },
  red: {
    600: '#B83D37',
    100: '#F8DDD9',
  },
  sky: {
    500: '#3D86B9',
  },
} as const;

export type ColorToken = typeof colors;
