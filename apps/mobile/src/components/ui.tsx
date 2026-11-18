import type { ReactNode } from 'react';
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  type TextInputProps,
  View,
} from 'react-native';
import { type Edge, SafeAreaView } from 'react-native-safe-area-context';

import { type Language, useLanguageStore, useT } from '@/lib/i18n';

export const colors = {
  primary: '#208AEF',
  primaryPressed: '#1670C4',
  primaryLight: '#E7F2FD',
  text: '#1B1F24',
  muted: '#5B6470',
  border: '#CBD2DA',
  surface: '#F4F6F8',
  error: '#C62828',
  errorBackground: '#FDECEA',
  success: '#2E7D32',
  successBackground: '#E8F5E9',
  warning: '#9A6700',
  warningBackground: '#FFF4D6',
  background: '#FFFFFF',
};

type ScreenProps = {
  children: ReactNode;
  /** Scrollable content, for forms and long details. */
  scroll?: boolean;
  /** Tab screens have no header, so they also need the top safe area. */
  edges?: Edge[];
};

export function Screen({ children, scroll, edges = ['bottom', 'left', 'right'] }: ScreenProps) {
  return (
    <SafeAreaView style={styles.safe} edges={edges}>
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        {scroll ? (
          <ScrollView contentContainerStyle={styles.content} keyboardShouldPersistTaps="handled">
            {children}
          </ScrollView>
        ) : (
          <View style={[styles.content, styles.flex]}>{children}</View>
        )}
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}

export function Title({ children }: { children: ReactNode }) {
  return (
    <Text style={styles.title} accessibilityRole="header">
      {children}
    </Text>
  );
}

export function Body({ children }: { children: ReactNode }) {
  return <Text style={styles.body}>{children}</Text>;
}

export function Loading() {
  return (
    <View style={styles.center}>
      <ActivityIndicator size="large" color={colors.primary} />
    </View>
  );
}

type FieldProps = TextInputProps & { label: string; error?: string; prefix?: string; hint?: string };

export function Field({ label, error, prefix, hint, multiline, ...inputProps }: FieldProps) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <View style={[styles.inputRow, error ? styles.inputRowError : null]}>
        {prefix ? <Text style={styles.prefix}>{prefix}</Text> : null}
        <TextInput
          style={[styles.input, multiline ? styles.inputMultiline : null]}
          placeholderTextColor={colors.muted}
          accessibilityLabel={label}
          multiline={multiline}
          {...inputProps}
        />
      </View>
      {hint && !error ? <Text style={styles.hint}>{hint}</Text> : null}
      {error ? <Text style={styles.fieldError}>{error}</Text> : null}
    </View>
  );
}

type ButtonProps = {
  title: string;
  onPress: () => void;
  loading?: boolean;
  disabled?: boolean;
  variant?: 'primary' | 'secondary' | 'danger';
};

export function Button({ title, onPress, loading, disabled, variant = 'primary' }: ButtonProps) {
  const inactive = disabled || loading;
  const primary = variant === 'primary';
  return (
    <Pressable
      onPress={onPress}
      disabled={inactive}
      accessibilityRole="button"
      accessibilityState={{ disabled: !!inactive, busy: !!loading }}
      style={({ pressed }) => [
        styles.button,
        primary ? styles.buttonPrimary : variant === 'danger' ? styles.buttonDanger : styles.buttonSecondary,
        pressed && primary ? styles.buttonPrimaryPressed : null,
        pressed && !primary ? styles.buttonSecondaryPressed : null,
        inactive ? styles.buttonInactive : null,
      ]}>
      {loading ? (
        <ActivityIndicator color={primary ? '#FFFFFF' : colors.primary} />
      ) : (
        <Text
          style={
            primary
              ? styles.buttonPrimaryText
              : variant === 'danger'
                ? styles.buttonDangerText
                : styles.buttonSecondaryText
          }>
          {title}
        </Text>
      )}
    </Pressable>
  );
}

/** A small text button, for less important actions such as "Report". */
export function LinkButton({ title, onPress, color = colors.primary }: { title: string; onPress: () => void; color?: string }) {
  return (
    <Pressable onPress={onPress} accessibilityRole="button" hitSlop={10} style={styles.linkButton}>
      <Text style={[styles.linkButtonText, { color }]}>{title}</Text>
    </Pressable>
  );
}

type Choice<T extends string> = { value: T; label: string };

/** A row of selectable chips for choosing one value, such as a category or a condition. */
export function ChoiceChips<T extends string>({
  label,
  choices,
  value,
  onChange,
  error,
}: {
  label: string;
  choices: Choice<T>[];
  value: T | undefined;
  onChange: (value: T) => void;
  error?: string;
}) {
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{label}</Text>
      <View style={styles.chips} accessibilityRole="radiogroup" accessibilityLabel={label}>
        {choices.map((choice) => {
          const selected = choice.value === value;
          return (
            <Pressable
              key={choice.value}
              onPress={() => onChange(choice.value)}
              accessibilityRole="radio"
              accessibilityState={{ selected }}
              style={[styles.chip, selected ? styles.chipSelected : null]}>
              <Text style={[styles.chipText, selected ? styles.chipTextSelected : null]}>{choice.label}</Text>
            </Pressable>
          );
        })}
      </View>
      {error ? <Text style={styles.fieldError}>{error}</Text> : null}
    </View>
  );
}

/** Two or three tabs inside a screen, such as "My donations" and "My claims". */
export function Segmented<T extends string>({
  choices,
  value,
  onChange,
}: {
  choices: Choice<T>[];
  value: T;
  onChange: (value: T) => void;
}) {
  return (
    <View style={styles.segmented} accessibilityRole="tablist">
      {choices.map((choice) => {
        const selected = choice.value === value;
        return (
          <Pressable
            key={choice.value}
            onPress={() => onChange(choice.value)}
            accessibilityRole="tab"
            accessibilityState={{ selected }}
            style={[styles.segment, selected ? styles.segmentSelected : null]}>
            <Text style={[styles.segmentText, selected ? styles.segmentTextSelected : null]}>{choice.label}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

type PillTone = 'neutral' | 'info' | 'success' | 'warning' | 'error';

const pillColors: Record<PillTone, { text: string; background: string }> = {
  neutral: { text: colors.muted, background: colors.surface },
  info: { text: colors.primaryPressed, background: colors.primaryLight },
  success: { text: colors.success, background: colors.successBackground },
  warning: { text: colors.warning, background: colors.warningBackground },
  error: { text: colors.error, background: colors.errorBackground },
};

export function Pill({ label, tone = 'neutral' }: { label: string; tone?: PillTone }) {
  return (
    <View style={[styles.pill, { backgroundColor: pillColors[tone].background }]}>
      <Text style={[styles.pillText, { color: pillColors[tone].text }]}>{label}</Text>
    </View>
  );
}

/** Shown when a list has nothing in it yet. */
export function EmptyState({ title, message, action }: { title: string; message?: string; action?: ReactNode }) {
  return (
    <View style={styles.state}>
      <Text style={styles.stateTitle}>{title}</Text>
      {message ? <Text style={styles.stateMessage}>{message}</Text> : null}
      {action}
    </View>
  );
}

/** Shown when loading failed, with a way to try again. */
export function ErrorState({
  message,
  onRetry,
  retrying,
}: {
  message: string;
  onRetry: () => void;
  retrying?: boolean;
}) {
  const m = useT();
  return (
    <View style={styles.state}>
      <Text style={styles.stateTitle}>{m.common.couldNotLoad}</Text>
      <Text style={styles.stateMessage}>{message}</Text>
      <Button title={m.common.tryAgain} onPress={onRetry} loading={retrying} />
    </View>
  );
}

/**
 * English / हिन्दी switch. Each choice is written in its own language so anyone can find theirs.
 * onChange is called after the app has switched; the caller may also save it to the account.
 */
export function LanguageSwitch({ onChange }: { onChange?: (language: Language) => void }) {
  const m = useT();
  const language = useLanguageStore((state) => state.language);
  const setLanguage = useLanguageStore((state) => state.setLanguage);
  return (
    <View style={styles.field}>
      <Text style={styles.label}>{m.language.label} / Language</Text>
      <Segmented
        choices={[
          { value: 'en', label: 'English' },
          { value: 'hi', label: 'हिन्दी' },
        ]}
        value={language}
        onChange={(value: Language) => {
          setLanguage(value, true);
          onChange?.(value);
        }}
      />
    </View>
  );
}

export function ErrorBanner({ message }: { message?: string | null }) {
  if (!message) {
    return null;
  }
  return (
    <View style={styles.banner} accessibilityRole="alert">
      <Text style={styles.bannerText}>{message}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.background },
  flex: { flex: 1 },
  content: { padding: 24, gap: 16 },
  center: { flex: 1, alignItems: 'center', justifyContent: 'center', backgroundColor: colors.background },
  title: { fontSize: 26, fontWeight: '700', color: colors.text },
  body: { fontSize: 16, lineHeight: 22, color: colors.muted },
  field: { gap: 6 },
  label: { fontSize: 14, fontWeight: '600', color: colors.text },
  inputRow: {
    flexDirection: 'row',
    alignItems: 'center',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 12,
    paddingHorizontal: 14,
    minHeight: 52,
  },
  inputRowError: { borderColor: colors.error },
  prefix: { fontSize: 18, color: colors.text, marginRight: 8 },
  input: { flex: 1, fontSize: 18, color: colors.text, paddingVertical: 12 },
  inputMultiline: { minHeight: 96, textAlignVertical: 'top' },
  hint: { fontSize: 13, color: colors.muted },
  fieldError: { fontSize: 14, color: colors.error },
  button: { minHeight: 52, borderRadius: 12, alignItems: 'center', justifyContent: 'center', paddingHorizontal: 20 },
  buttonPrimary: { backgroundColor: colors.primary },
  buttonPrimaryPressed: { backgroundColor: colors.primaryPressed },
  buttonSecondary: { backgroundColor: colors.background, borderWidth: 1, borderColor: colors.border },
  buttonSecondaryPressed: { backgroundColor: colors.surface },
  buttonDanger: { backgroundColor: colors.background, borderWidth: 1, borderColor: colors.error },
  buttonInactive: { opacity: 0.6 },
  buttonPrimaryText: { color: '#FFFFFF', fontSize: 17, fontWeight: '600' },
  buttonSecondaryText: { color: colors.text, fontSize: 17, fontWeight: '600' },
  buttonDangerText: { color: colors.error, fontSize: 17, fontWeight: '600' },
  linkButton: { paddingVertical: 4 },
  linkButtonText: { fontSize: 15, fontWeight: '600' },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  chip: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: 20,
    paddingHorizontal: 14,
    paddingVertical: 8,
    minHeight: 40,
    justifyContent: 'center',
  },
  chipSelected: { backgroundColor: colors.primary, borderColor: colors.primary },
  chipText: { fontSize: 15, color: colors.text },
  chipTextSelected: { color: '#FFFFFF', fontWeight: '600' },
  segmented: { flexDirection: 'row', backgroundColor: colors.surface, borderRadius: 12, padding: 4 },
  segment: { flex: 1, minHeight: 40, borderRadius: 9, alignItems: 'center', justifyContent: 'center' },
  segmentSelected: { backgroundColor: colors.background, elevation: 1 },
  segmentText: { fontSize: 15, color: colors.muted, fontWeight: '600' },
  segmentTextSelected: { color: colors.text },
  pill: { alignSelf: 'flex-start', borderRadius: 10, paddingHorizontal: 10, paddingVertical: 3 },
  pillText: { fontSize: 13, fontWeight: '600' },
  state: { flex: 1, alignItems: 'center', justifyContent: 'center', gap: 12, padding: 24 },
  stateTitle: { fontSize: 20, fontWeight: '700', color: colors.text, textAlign: 'center' },
  stateMessage: { fontSize: 16, lineHeight: 22, color: colors.muted, textAlign: 'center' },
  banner: { backgroundColor: colors.errorBackground, borderRadius: 12, padding: 12 },
  bannerText: { color: colors.error, fontSize: 15 },
});
