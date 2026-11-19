import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import Animated, { FadeInDown } from 'react-native-reanimated';

import { AuthImage } from '@/components/photos';
import { font, radius, shadow } from '@/components/theme';
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
    <Animated.View entering={FadeInDown.duration(220)} style={styles.card}>
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
    </Animated.View>
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
    borderRadius: radius.large,
    backgroundColor: colors.card,
    ...shadow,
  },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, padding: 12, borderRadius: radius.large },
  pressed: { backgroundColor: colors.surface },
  thumb: { width: 68, height: 68, borderRadius: radius.medium },
  text: { flex: 1, gap: 4 },
  title: { fontSize: 16, fontFamily: font.semibold, color: colors.text },
  subtitle: { fontSize: 14, color: colors.muted, lineHeight: 21, fontFamily: font.regular },
  chevron: { fontSize: 26, color: colors.muted, fontFamily: font.regular },
  actions: { flexDirection: 'row', gap: 8, paddingHorizontal: 12, paddingBottom: 12 },
  section: { fontSize: 15, fontFamily: font.bold, color: colors.muted, marginTop: 8, textTransform: 'uppercase' },
});
