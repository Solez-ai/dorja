export const typography = {
  fonts: {
    display: {
      family: 'Space Grotesk',
      weights: [600, 700] as const,
      usage: 'Short headings and numeric price blocks',
    },
    body: {
      family: 'IBM Plex Sans',
      weights: [400, 500, 600] as const,
      usage: 'Forms, tables, and interface body text',
    },
    bangla: {
      family: 'Hind Siliguri',
      weights: [400, 500, 600, 700] as const,
      usage: 'All Bangla labels and sentences',
    },
    mono: {
      family: 'IBM Plex Mono',
      weights: [500, 600] as const,
      usage: 'Confirmation timestamps, status IDs, capture data, price blocks',
    },
  },
  webFontDisplay: 'swap' as const,
  sizes: {
    xs: '0.75rem',   // 12px
    sm: '0.875rem',  // 14px — minimum for primary flows
    base: '1rem',    // 16px
    lg: '1.125rem',  // 18px
    xl: '1.25rem',   // 20px
    '2xl': '1.5rem', // 24px
    '3xl': '2rem',   // 32px
    '4xl': '2.5rem', // 40px
  },
  lineHeights: {
    tight: 1.2,
    normal: 1.5,
    relaxed: 1.75,
  },
} as const;

export type TypographyToken = typeof typography;
