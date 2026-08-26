export const spacing = {
  px: '1px',
  0: '0',
  1: '0.25rem',  // 4px
  2: '0.5rem',   // 8px
  3: '0.75rem',  // 12px
  4: '1rem',     // 16px
  5: '1.25rem',  // 20px
  6: '1.5rem',   // 24px
  8: '2rem',     // 32px
  10: '2.5rem',  // 40px
  12: '3rem',    // 48px
  16: '4rem',    // 64px
  20: '5rem',    // 80px
} as const;

export const radii = {
  /** Ordinary cards */
  card: '10px',
  /** Stamped status labels */
  stamp: '2px',
  /** Clipped corner motif */
  clipped: '10px',
  /** Full rounding */
  full: '9999px',
  /** None */
  none: '0',
} as const;

export const shadows = {
  sm: '0 1px 2px rgba(11, 31, 51, 0.06)',
  md: '0 2px 4px rgba(11, 31, 51, 0.08)',
  lg: '0 4px 8px rgba(11, 31, 51, 0.1)',
} as const;

export const layout = {
  /** Web navigation rail width */
  railWidth: '72px',
  /** Minimum touch target */
  touchTarget: '48px',
  /** Minimum text size in primary flows */
  minTextSize: '14px',
} as const;

export const motion = {
  /** Button press scale */
  pressScale: 0.97,
  /** Panel enter transition duration range */
  panelEnter: { min: 180, max: 240 },
  /** Duration in ms */
  fast: 150,
  normal: 200,
  slow: 300,
} as const;

export type SpacingToken = typeof spacing;
export type RadiiToken = typeof radii;
