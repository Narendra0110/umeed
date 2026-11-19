import { useEffect } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import Animated, { FadeIn, FadeOut, ZoomIn } from 'react-native-reanimated';

import { useCelebration } from '@/lib/delight';

import { colors, font, shadow } from './theme';

const SHOW_FOR_MS = 1600;

/** The success moment (U03): a big check mark that pops in with a short message, then fades away. */
export function Celebration() {
  const { message, key, hide } = useCelebration();

  useEffect(() => {
    if (!message) {
      return undefined;
    }
    const timer = setTimeout(hide, SHOW_FOR_MS);
    return () => clearTimeout(timer);
  }, [message, key, hide]);

  if (!message) {
    return null;
  }
  return (
    <Animated.View
      key={key}
      entering={FadeIn.duration(150)}
      exiting={FadeOut.duration(250)}
      style={styles.backdrop}
      pointerEvents="none"
      accessibilityRole="alert"
      accessibilityLiveRegion="polite">
      <View style={styles.card}>
        <Animated.View entering={ZoomIn.springify().damping(12)} style={styles.circle}>
          <Text style={styles.check}>✓</Text>
        </Animated.View>
        <Text style={styles.message}>{message}</Text>
      </View>
    </Animated.View>
  );
}

const styles = StyleSheet.create({
  backdrop: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(43,33,24,0.25)',
  },
  card: {
    alignItems: 'center',
    gap: 12,
    paddingVertical: 24,
    paddingHorizontal: 28,
    marginHorizontal: 40,
    borderRadius: 24,
    backgroundColor: colors.card,
    ...shadow,
  },
  circle: {
    width: 84,
    height: 84,
    borderRadius: 42,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.success,
  },
  check: { color: '#FFFFFF', fontSize: 48, lineHeight: 60, fontFamily: font.bold },
  message: { fontSize: 18, lineHeight: 25, fontFamily: font.bold, color: colors.text, textAlign: 'center' },
});
