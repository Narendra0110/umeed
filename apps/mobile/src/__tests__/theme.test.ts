import { colors, font } from '@/components/theme';

/** WCAG contrast ratio between two #RRGGBB colours. */
function contrast(a: string, b: string): number {
  const luminance = (hex: string) => {
    const [r, g, bl] = [1, 3, 5]
      .map((i) => parseInt(hex.slice(i, i + 2), 16) / 255)
      .map((v) => (v <= 0.03928 ? v / 12.92 : ((v + 0.055) / 1.055) ** 2.4));
    return 0.2126 * r + 0.7152 * g + 0.0722 * bl;
  };
  const [x, y] = [luminance(a), luminance(b)].sort((p, q) => q - p);
  return (x + 0.05) / (y + 0.05);
}

describe('the U01 colours are easy to read', () => {
  // 4.5 is the WCAG AA level for normal text.
  it.each([
    ['text on the background', colors.text, colors.background],
    ['text on cards', colors.text, colors.card],
    ['secondary text on the background', colors.muted, colors.background],
    ['white button text on the main colour', '#FFFFFF', colors.primary],
    ['links on the background', colors.primary, colors.background],
    ['success text on its background', colors.success, colors.successBackground],
    ['error text on its background', colors.error, colors.errorBackground],
    ['warning text on its background', colors.warning, colors.warningBackground],
    ['the main colour on its light tint', colors.primary, colors.primaryLight],
  ])('%s', (_, foreground, background) => {
    expect(contrast(foreground, background)).toBeGreaterThanOrEqual(4.5);
  });

  it('uses the Baloo 2 font in every weight', () => {
    for (const name of Object.values(font)) {
      expect(name).toMatch(/^Baloo2_/);
    }
  });
});
