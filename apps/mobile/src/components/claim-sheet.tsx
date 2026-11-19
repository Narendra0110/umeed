import { zodResolver } from '@hookform/resolvers/zod';
import { Controller, useForm } from 'react-hook-form';
import { KeyboardAvoidingView, Modal, Platform, Pressable, StyleSheet, Text, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { font } from '@/components/theme';
import { Body, Button, colors, ErrorBanner, Field } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { useCreateClaim } from '@/lib/claims';
import { useT } from '@/lib/i18n';
import { claimSchema, type ClaimFormValues } from '@/lib/schemas';

type ClaimSheetProps = {
  donationId: string;
  donationTitle: string;
  donorName: string;
  visible: boolean;
  onClose: () => void;
  onClaimed: () => void;
};

/** Asks for an optional message to the donor, then sends the claim. */
export function ClaimSheet({ donationId, donationTitle, donorName, visible, onClose, onClaimed }: ClaimSheetProps) {
  'use no memo';
  const m = useT();
  const claim = useCreateClaim();
  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<ClaimFormValues>({ resolver: zodResolver(claimSchema), defaultValues: { message: '' } });

  const close = () => {
    reset();
    claim.reset();
    onClose();
  };

  const onSubmit = handleSubmit(({ message }) => {
    claim.mutate(
      { donationId, message },
      {
        onSuccess: () => {
          reset();
          onClaimed();
        },
      },
    );
  });

  const serverFieldError =
    claim.error instanceof ApiError ? claim.error.fieldErrors.find((f) => f.field === 'message')?.message : undefined;

  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={close}>
      <Pressable style={styles.backdrop} onPress={close} accessibilityLabel={m.common.close} />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined}>
        <SafeAreaView edges={['bottom']} style={styles.sheet}>
          <View style={styles.handle} />
          <Text style={styles.title} accessibilityRole="header">
            {m.claimSheet.title(donationTitle)}
          </Text>
          <Body>{m.claimSheet.intro(donorName)}</Body>
          <Controller
            control={control}
            name="message"
            render={({ field: { onChange, onBlur, value } }) => (
              <Field
                label={m.claimSheet.message}
                placeholder={m.claimSheet.messagePlaceholder}
                multiline
                maxLength={300}
                value={value}
                onChangeText={onChange}
                onBlur={onBlur}
                error={errors.message?.message ?? serverFieldError}
              />
            )}
          />
          <ErrorBanner message={claim.isError && !serverFieldError ? errorMessage(claim.error) : null} />
          <Button title={m.claimSheet.send} onPress={onSubmit} loading={claim.isPending} />
          <Button title={m.common.cancel} variant="secondary" onPress={close} />
        </SafeAreaView>
      </KeyboardAvoidingView>
    </Modal>
  );
}

const styles = StyleSheet.create({
  backdrop: { flex: 1, backgroundColor: 'rgba(0,0,0,0.4)' },
  sheet: {
    backgroundColor: colors.background,
    borderTopLeftRadius: 20,
    borderTopRightRadius: 20,
    padding: 24,
    gap: 14,
  },
  handle: { alignSelf: 'center', width: 40, height: 4, borderRadius: 2, backgroundColor: colors.border },
  title: { fontSize: 22, fontFamily: font.bold, color: colors.text },
});
