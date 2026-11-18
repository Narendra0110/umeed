import { t } from './i18n';
import type { Category, ClaimStatus, DonationStatus, ItemCondition, ReportReason } from './types';

/** Labels in the current language. Components that show them use useT(), so they re-render on a switch. */
export const conditionLabel = (condition: ItemCondition) => t().labels.condition[condition];
export const donationStatusLabel = (status: DonationStatus) => t().labels.donationStatus[status];
export const claimStatusLabel = (status: ClaimStatus) => t().labels.claimStatus[status];
export const reportReasonLabel = (reason: ReportReason) => t().labels.reportReason[reason];

/** Category names come from the server in English; known ones are shown in the current language. */
export const categoryLabel = (category: Pick<Category, 'slug' | 'name'>) =>
  t().labels.category[category.slug] ?? category.name;

export type Section<T> = { key: string; title: string; data: T[] };

/** Sections for "My donations": items people can still claim, reserved items, then given away. */
export function groupMyDonations<T extends { status: DonationStatus }>(items: T[]): Section<T>[] {
  const s = t().sections;
  return nonEmpty([
    { key: 'available', title: s.available, data: items.filter((d) => d.status === 'AVAILABLE') },
    { key: 'reserved', title: s.reserved, data: items.filter((d) => d.status === 'RESERVED') },
    { key: 'completed', title: s.givenAway, data: items.filter((d) => d.status === 'COMPLETED') },
  ]);
}

/**
 * Sections for "My claims": accepted items still to pick up first (they need action), then waiting,
 * then items already received, then closed. A claim stays ACCEPTED after the handover; the item's own
 * status (COMPLETED) is what shows it was received.
 */
export function groupMyClaims<T extends { status: ClaimStatus; donationStatus: DonationStatus | null }>(
  items: T[],
): Section<T>[] {
  const s = t().sections;
  const received = (c: T) => c.status === 'ACCEPTED' && c.donationStatus === 'COMPLETED';
  return nonEmpty([
    { key: 'accepted', title: s.accepted, data: items.filter((c) => c.status === 'ACCEPTED' && !received(c)) },
    { key: 'pending', title: s.waiting, data: items.filter((c) => c.status === 'PENDING') },
    { key: 'received', title: s.received, data: items.filter(received) },
    {
      key: 'closed',
      title: s.closed,
      data: items.filter((c) => c.status === 'REJECTED' || c.status === 'WITHDRAWN'),
    },
  ]);
}

function nonEmpty<T>(sections: Section<T>[]): Section<T>[] {
  return sections.filter((s) => s.data.length > 0);
}

export type Coordinates = { latitude: number; longitude: number };

/** Straight-line distance in kilometres (haversine formula). */
export function distanceKm(from: Coordinates, to: Coordinates): number {
  const earthRadiusKm = 6371;
  const toRadians = (degrees: number) => (degrees * Math.PI) / 180;
  const dLat = toRadians(to.latitude - from.latitude);
  const dLng = toRadians(to.longitude - from.longitude);
  const a =
    Math.sin(dLat / 2) ** 2 +
    Math.cos(toRadians(from.latitude)) * Math.cos(toRadians(to.latitude)) * Math.sin(dLng / 2) ** 2;
  return 2 * earthRadiusKm * Math.asin(Math.sqrt(a));
}

/**
 * A rough distance for display. Donation coordinates are rounded to about one kilometre,
 * so anything closer than that is shown as "Within 1 km".
 */
export function formatDistance(km: number): string {
  const time = t().time;
  return km < 1 ? time.withinOneKm : time.kmAway(Math.round(km));
}

/** "Just now", "5 min ago", "3 h ago", "2 days ago", or a date for anything older than a week. */
export function timeAgo(isoDate: string, now: Date = new Date()): string {
  const time = t().time;
  const seconds = Math.max(0, (now.getTime() - new Date(isoDate).getTime()) / 1000);
  if (seconds < 60) {
    return time.justNow;
  }
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return time.minutesAgo(minutes);
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return time.hoursAgo(hours);
  }
  const days = Math.floor(hours / 24);
  if (days < 7) {
    return time.daysAgo(days);
  }
  return new Date(isoDate).toLocaleDateString(time.dateLocale, { day: 'numeric', month: 'short', year: 'numeric' });
}
