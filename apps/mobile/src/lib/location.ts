import { useQuery } from '@tanstack/react-query';
import * as Location from 'expo-location';

import type { Coordinates } from './format';

export type LocationResult =
  | { permission: 'granted'; coords: Coordinates }
  | { permission: 'denied'; coords: null };

/**
 * The phone's approximate position. Asks for permission the first time.
 * Used to sort the feed by distance and to place a new donation; the backend rounds
 * donation coordinates to about one kilometre and never stores the viewer's position.
 */
export function useCurrentLocation() {
  return useQuery({
    queryKey: ['location'],
    queryFn: getApproximateLocation,
    staleTime: 10 * 60 * 1000,
    retry: false,
  });
}

async function getApproximateLocation(): Promise<LocationResult> {
  const { granted } = await Location.requestForegroundPermissionsAsync();
  if (!granted) {
    return { permission: 'denied', coords: null };
  }
  const recent = await Location.getLastKnownPositionAsync({ maxAge: 10 * 60 * 1000 });
  const position =
    recent ?? (await Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced }));
  return {
    permission: 'granted',
    coords: { latitude: position.coords.latitude, longitude: position.coords.longitude },
  };
}
