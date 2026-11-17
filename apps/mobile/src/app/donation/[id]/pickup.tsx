import { router, useLocalSearchParams } from 'expo-router';
import { Alert, Linking, StyleSheet, Text, View } from 'react-native';

import { Body, Button, colors, EmptyState, ErrorState, Loading, Pill, Screen, Title } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { pickupRole, useCancelPickup, useConfirmPickup, usePickup } from '@/lib/pickup';
import { useProfile } from '@/lib/profile';
import type { PickupParty } from '@/lib/types';

/**
 * Private pickup details for the donor and the chosen receiver: the address, each other's phone
 * number, and the handover confirmations. Both confirmations complete the donation.
 */
export default function PickupScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const pickup = usePickup(id);
  const profile = useProfile(true);
  const confirm = useConfirmPickup(id);
  const cancel = useCancelPickup(id);

  if (pickup.isPending || profile.isPending) {
    return <Loading />;
  }
  if (pickup.isError) {
    const status = pickup.error instanceof ApiError ? pickup.error.status : 0;
    if (status === 403 || status === 404 || status === 409) {
      return (
        <Screen>
          <EmptyState
            title="No pickup details"
            message="Pickup details are shown only to the donor and the chosen receiver while the item is reserved or given away."
          />
        </Screen>
      );
    }
    return (
      <Screen>
        <ErrorState message={errorMessage(pickup.error)} onRetry={() => pickup.refetch()} retrying={pickup.isFetching} />
      </Screen>
    );
  }

  const details = pickup.data;
  const role = pickupRole(details, profile.data?.phoneNumber);
  const completed = details.status === 'COMPLETED';
  const iConfirmed = role === 'donor' ? details.donorConfirmed : details.receiverConfirmed;
  const other = role === 'donor' ? details.receiver : details.donor;
  const otherLabel = role === 'donor' ? 'Receiver' : 'Donor';

  const confirmHandover = () =>
    Alert.alert(
      role === 'donor' ? 'Have you handed it over?' : 'Have you received it?',
      'Confirm only after the item has changed hands. When you both confirm, the donation is complete.',
      [
        { text: 'Not yet', style: 'cancel' },
        {
          text: 'Confirm',
          onPress: () =>
            confirm.mutate(undefined, { onError: (error) => Alert.alert('Could not confirm', errorMessage(error)) }),
        },
      ],
    );

  const cancelReservation = () =>
    Alert.alert(
      'Cancel this reservation?',
      role === 'donor'
        ? `The item goes back to the feed and ${details.receiver.name} will not be able to claim it again. The other people who claimed it can be chosen again.`
        : 'The item goes back to the donor and you will not be able to claim it again.',
      [
        { text: 'Keep reservation', style: 'cancel' },
        {
          text: 'Cancel reservation',
          style: 'destructive',
          onPress: () =>
            cancel.mutate(undefined, {
              onSuccess: () => router.back(),
              onError: (error) => Alert.alert('Could not cancel', errorMessage(error)),
            }),
        },
      ],
    );

  return (
    <Screen scroll>
      <Title>{details.donationTitle}</Title>
      {completed ? (
        <Pill label="Given away – thank you!" tone="success" />
      ) : (
        <Pill label="Reserved – arrange the pickup by phone" tone="warning" />
      )}

      <View style={styles.card}>
        <Text style={styles.cardLabel}>Pickup address</Text>
        <Text style={styles.address} selectable>
          {details.pickupAddress}
        </Text>
      </View>

      {role ? (
        <PartyCard label={otherLabel} party={other} />
      ) : (
        <>
          <PartyCard label="Donor" party={details.donor} />
          <PartyCard label="Receiver" party={details.receiver} />
        </>
      )}

      <View style={styles.card}>
        <Text style={styles.cardLabel}>Handover</Text>
        <ConfirmLine label={`Donor (${details.donor.name})`} confirmed={details.donorConfirmed} />
        <ConfirmLine label={`Receiver (${details.receiver.name})`} confirmed={details.receiverConfirmed} />
      </View>

      {!completed && role ? (
        <View style={styles.actions}>
          {iConfirmed ? (
            <Body>You have confirmed. Waiting for {other.name} to confirm too.</Body>
          ) : (
            <Button
              title={role === 'donor' ? 'I have handed it over' : 'I have received it'}
              onPress={confirmHandover}
              loading={confirm.isPending}
            />
          )}
          <Button title="Cancel reservation" variant="danger" onPress={cancelReservation} loading={cancel.isPending} />
        </View>
      ) : null}
      <Body>Meet in a safe, public place if you can. Umeed items are always free.</Body>
    </Screen>
  );
}

function PartyCard({ label, party }: { label: string; party: PickupParty }) {
  return (
    <View style={styles.card}>
      <Text style={styles.cardLabel}>{label}</Text>
      <Text style={styles.name}>{party.name}</Text>
      <Text style={styles.phone} selectable>
        {party.phoneNumber}
      </Text>
      <Button
        title={`Call ${party.name}`}
        variant="secondary"
        onPress={() =>
          Linking.openURL(`tel:${party.phoneNumber}`).catch(() =>
            Alert.alert('Could not start a call', `Please dial ${party.phoneNumber}.`),
          )
        }
      />
    </View>
  );
}

function ConfirmLine({ label, confirmed }: { label: string; confirmed: boolean }) {
  return (
    <View style={styles.confirmLine}>
      <Text style={styles.confirmLabel}>{label}</Text>
      <Pill label={confirmed ? 'Confirmed' : 'Not yet'} tone={confirmed ? 'success' : 'neutral'} />
    </View>
  );
}

const styles = StyleSheet.create({
  card: { borderWidth: 1, borderColor: colors.border, borderRadius: 14, padding: 16, gap: 8 },
  cardLabel: { fontSize: 13, fontWeight: '700', color: colors.muted, textTransform: 'uppercase' },
  address: { fontSize: 17, lineHeight: 24, color: colors.text },
  name: { fontSize: 18, fontWeight: '600', color: colors.text },
  phone: { fontSize: 17, color: colors.text },
  confirmLine: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 12 },
  confirmLabel: { flexShrink: 1, fontSize: 15, color: colors.text },
  actions: { gap: 12 },
});
