import { Redirect } from 'expo-router';
import { NativeTabs } from 'expo-router/native-tabs';

import { colors, Loading } from '@/components/ui';
import { useAuthUser } from '@/lib/auth';
import { usePushNotifications } from '@/lib/push';

/** The four main tabs. Only reachable while logged in. */
export default function TabsLayout() {
  const { initializing, user } = useAuthUser();

  if (initializing) {
    return <Loading />;
  }
  if (!user) {
    return <Redirect href="/login" />;
  }

  return (
    <>
    <PushNotifications />
    <NativeTabs tintColor={colors.primary}>
      <NativeTabs.Trigger name="feed">
        <NativeTabs.Trigger.Label>Home</NativeTabs.Trigger.Label>
        <NativeTabs.Trigger.Icon sf={{ default: 'house', selected: 'house.fill' }} md="home" />
      </NativeTabs.Trigger>
      <NativeTabs.Trigger name="donate">
        <NativeTabs.Trigger.Label>Donate</NativeTabs.Trigger.Label>
        <NativeTabs.Trigger.Icon sf={{ default: 'plus.circle', selected: 'plus.circle.fill' }} md="add_circle" />
      </NativeTabs.Trigger>
      <NativeTabs.Trigger name="activity">
        <NativeTabs.Trigger.Label>Activity</NativeTabs.Trigger.Label>
        <NativeTabs.Trigger.Icon sf={{ default: 'shippingbox', selected: 'shippingbox.fill' }} md="inventory_2" />
      </NativeTabs.Trigger>
      <NativeTabs.Trigger name="profile">
        <NativeTabs.Trigger.Label>Profile</NativeTabs.Trigger.Label>
        <NativeTabs.Trigger.Icon sf={{ default: 'person', selected: 'person.fill' }} md="person" />
      </NativeTabs.Trigger>
    </NativeTabs>
    </>
  );
}

/** Notifications start once the user is past login and profile setup, so the server knows them. */
function PushNotifications() {
  usePushNotifications();
  return null;
}
