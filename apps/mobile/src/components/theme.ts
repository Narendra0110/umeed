/**
 * Umeed's look (U01): a warm "hope" palette, the Baloo 2 font (Latin and Devanagari in one family, so
 * English and Hindi match), rounded corners and soft shadows. Every screen takes its colours and fonts
 * from here. Contrast of text colours is checked in __tests__/theme.test.ts.
 */

export const colors = {
  /** Buttons, links, selected tabs. Deep enough for white text on it. */
  primary: '#B84A0C',
  primaryPressed: '#9A3D08',
  primaryLight: '#FDEBDD',
  /** Bright saffron, for decoration only (splash, brand accents); not for text on white. */
  accent: '#F2762E',
  text: '#2B2118',
  muted: '#6B5F55',
  border: '#E8DCCF',
  /** Light warm grey for secondary surfaces (segmented control, placeholders). */
  surface: '#F5ECE2',
  /** Cards and inputs sit on the background. */
  card: '#FFFFFF',
  background: '#FFFAF4',
  error: '#C62828',
  errorBackground: '#FDECEA',
  success: '#0F6F66',
  successBackground: '#E1F4F1',
  warning: '#865A00',
  warningBackground: '#FFF4D6',
};

/** Baloo 2 weights, loaded in app/_layout.tsx. Use these instead of fontWeight. */
export const font = {
  regular: 'Baloo2_400Regular',
  medium: 'Baloo2_500Medium',
  semibold: 'Baloo2_600SemiBold',
  bold: 'Baloo2_700Bold',
  extraBold: 'Baloo2_800ExtraBold',
};

export const radius = { small: 10, medium: 14, large: 20, pill: 999 };

/** A soft shadow for cards and buttons (elevation on Android, shadow on iOS). */
export const shadow = {
  elevation: 2,
  shadowColor: '#5A3A1A',
  shadowOpacity: 0.08,
  shadowRadius: 8,
  shadowOffset: { width: 0, height: 2 },
};
