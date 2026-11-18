import { router, useLocalSearchParams } from 'expo-router';
import { Alert, Linking, StyleSheet, Text, View } from 'react-native';

import { Body, Button, colors, EmptyState, ErrorState, Loading, Pill, Screen, Title } from '@/components/ui';
import { ApiError, errorMessage } from '@/lib/api';
import { pickupRole, useCancelPickup, useConfirmPickup, usePickup } from '@/lib/pickup';
import { useT } from '@/lib/i18n';
import { useProfile } from '@/lib/profile';
import type { PickupParty } from '@/lib/types';

/**
 * Private pickup details for the donor and the chosen receiver: the address, each other's phone
 * number, and the handover confirmations. Both confirmations complete the donation.
 */
export default function PickupScreen() {
  const m = useT();
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
            title={m.pickup.noDetailsTitle}
            message={m.pickup.noDetailsMessage}
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
  const otherLabel = role === 'donor' ? m.pickup.receiver : m.pickup.donor;

  const confirmHandover = () =>
    Alert.alert(
      role === 'donor' ? m.pickup.handedOverQuestion : m.pickup.receivedQuestion,
      m.pickup.confirmMessage,
      [
        { text: m.common.notYet, style: 'cancel' },
        {
          text: m.pickup.confirm,
          onPress: () =>
            confirm.mutate(undefined, { onError: (error) => Alert.alert(m.pickup.couldNotConfirm, errorMessage(error)) }),
        },
      ],
    );

  const cancelReservation = () =>
    Alert.alert(
      m.pickup.cancelTitle,
      role === 'donor'
        ? m.pickup.cancelDonor(details.receiver.name)
        : m.pickup.cancelReceiver,
      [
        { text: m.pickup.keepReservation, style: 'cancel' },
        {
          text: m.pickup.cancelReservation,
          style: 'destructive',
          onPress: () =>
            cancel.mutate(undefined, {
              onSuccess: () => router.back(),
              onError: (error) => Alert.alert(m.pickup.couldNotCancel, errorMessage(error)),
            }),
        },
      ],
    );

  return (
    <Screen scroll>
      <Title>{details.donationTitle}</Title>
      {completed ? (
        <Pill label={m.pickup.givenAway} tone="success" />
      ) : (
        <Pill label={m.pickup.reserved} tone="warning" />
      )}

      <View style={styles.card}>
        <Text style={styles.cardLabel}>{m.pickup.address}</Text>
        <Text style={styles.address} selectable>
          {details.pickupAddress}
        </Text>
      </View>

      {role ? (
        <>
          <PartyCard label={otherLabel} party={other} />
          {details.claimId ? (
            <Button
              title={completed ? m.pickup.seeMessages(other.name) : m.pickup.message(other.name)}
              onPress={() => router.push(`/chat/${details.claimId}`)}
            />
          ) : null}
        </>
      ) : (
        <>
          <PartyCard label={m.pickup.donor} party={details.donor} />
          <PartyCard label={m.pickup.receiver} party={details.receiver} />
        </>
      )}

      <View style={styles.card}>
        <Text style={styles.cardLabel}>{m.pickup.handover}</Text>
        <ConfirmLine label={m.pickup.donorLine(details.donor.name)} confirmed={details.donorConfirmed} />
        <ConfirmLine label={m.pickup.receiverLine(details.receiver.name)} confirmed={details.receiverConfirmed} />
      </View>

      {!completed && role ? (
        <View style={styles.actions}>
          {iConfirmed ? (
            <Body>{m.pickup.waitingFor(other.name)}</Body>
          ) : (
            <Button
              title={role === 'donor' ? m.pickup.handedOver : m.pickup.receivedIt}
              onPress={confirmHandover}
              loading={confirm.isPending}
            />
          )}
          <Button title={m.pickup.cancelReservation} variant="danger" onPress={cancelReservation} loading={cancel.isPending} />
        </View>
      ) : null}
      <Body>{m.pickup.safety}</Body>
    </Screen>
  );
}

function PartyCard({ label, party }: { label: string; party: PickupParty }) {
  const m = useT();
  return (
    <View style={styles.card}>
      <Text style={styles.cardLabel}>{label}</Text>
      <Text style={styles.name}>{party.name}</Text>
      <Text style={styles.phone} selectable>
        {party.phoneNumber}
      </Text>
      <Button
        title={m.pickup.call(party.name)}
        variant="secondary"
        onPress={() =>
          Linking.openURL(`tel:${party.phoneNumber}`).catch(() =>
            Alert.alert(m.pickup.couldNotCall, m.pickup.pleaseDial(party.phoneNumber)),
          )
        }
      />
    </View>
  );
}

function ConfirmLine({ label, confirmed }: { label: string; confirmed: boolean }) {
  const m = useT();
  return (
    <View style={styles.confirmLine}>
      <Text style={styles.confirmLabel}>{label}</Text>
      <Pill label={confirmed ? m.pickup.confirmed : m.common.notYet} tone={confirmed ? 'success' : 'neutral'} />
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
