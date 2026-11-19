import { router } from 'expo-router';
import { useEffect } from 'react';
import { Pressable, StyleSheet, Text } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';

import { font } from '@/components/theme';
import { colors } from '@/components/ui';
import { usePushBanner } from '@/lib/push';

const SHOW_FOR_MS = 4500;

/** A notification that arrived while the app was open. Tapping it opens the related screen. */
export function PushBanner() {
  const { banner, hide } = usePushBanner();
  const insets = useSafeAreaInsets();

  useEffect(() => {
    if (!banner) {
      return undefined;
    }
    const timer = setTimeout(hide, SHOW_FOR_MS);
    return () => clearTimeout(timer);
  }, [banner, hide]);

  if (!banner) {
    return null;
  }

  return (
    <Pressable
      accessibilityRole="alert"
      onPress={() => {
        hide();
        if (banner.route) {
          router.push(banner.route as never);
        }
      }}
      style={[styles.banner, { top: insets.top + 8 }]}>
      <Text style={styles.title} numberOfLines={1}>
        {banner.title}
      </Text>
      {banner.body ? (
        <Text style={styles.body} numberOfLines={2}>
          {banner.body}
        </Text>
      ) : null}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  banner: {
    position: 'absolute',
    left: 12,
    right: 12,
    padding: 14,
    borderRadius: 14,
    backgroundColor: colors.text,
    gap: 2,
    elevation: 6,
    shadowColor: '#000',
    shadowOpacity: 0.2,
    shadowRadius: 8,
    shadowOffset: { width: 0, height: 2 },
  },
  title: { color: colors.background, fontSize: 15, fontFamily: font.bold },
  body: { color: colors.background, fontSize: 14, fontFamily: font.regular },
});
