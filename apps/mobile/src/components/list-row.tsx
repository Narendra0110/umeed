import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';

import { AuthImage } from '@/components/photos';
import { colors } from '@/components/ui';

type ListRowProps = {
  title: string;
  subtitle?: string;
  /** Photo path for a small thumbnail on the left. */
  imagePath?: string;
  /** Status pill or other small content under the title. */
  extra?: ReactNode;
  /** Buttons shown under the row. */
  actions?: ReactNode;
  onPress?: () => void;
};

/** One item in a list on the Activity tab or the claims screen. */
export function ListRow({ title, subtitle, imagePath, extra, actions, onPress }: ListRowProps) {
  return (
    <View style={styles.card}>
      <Pressable
        onPress={onPress}
        disabled={!onPress}
        accessibilityRole={onPress ? 'button' : undefined}
        style={({ pressed }) => [styles.row, pressed ? styles.pressed : null]}>
        {imagePath ? <AuthImage path={imagePath} style={styles.thumb} /> : null}
        <View style={styles.text}>
          <Text style={styles.title} numberOfLines={2}>
            {title}
          </Text>
          {subtitle ? (
            <Text style={styles.subtitle} numberOfLines={3}>
              {subtitle}
            </Text>
          ) : null}
          {extra}
        </View>
        {onPress ? <Text style={styles.chevron}>›</Text> : null}
      </Pressable>
      {actions ? <View style={styles.actions}>{actions}</View> : null}
    </View>
  );
}

export function SectionTitle({ children }: { children: string }) {
  return (
    <Text style={styles.section} accessibilityRole="header">
      {children}
    </Text>
  );
}

const styles = StyleSheet.create({
  card: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 14,
    backgroundColor: colors.background,
    overflow: 'hidden',
  },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 12 },
  pressed: { backgroundColor: colors.surface },
  thumb: { width: 64, height: 64, borderRadius: 10 },
  text: { flex: 1, gap: 4 },
  title: { fontSize: 16, fontWeight: '600', color: colors.text },
  subtitle: { fontSize: 14, color: colors.muted, lineHeight: 19 },
  chevron: { fontSize: 26, color: colors.muted },
  actions: { flexDirection: 'row', gap: 8, paddingHorizontal: 12, paddingBottom: 12 },
  section: { fontSize: 15, fontWeight: '700', color: colors.muted, marginTop: 8, textTransform: 'uppercase' },
});
