import type { ClaimStatus, DonationStatus, ItemCondition, ReportReason } from './types';

export const conditionLabels: Record<ItemCondition, string> = {
  NEW: 'New',
  LIKE_NEW: 'Like new',
  GOOD: 'Good',
  USABLE: 'Usable',
};

export const donationStatusLabels: Record<DonationStatus, string> = {
  AVAILABLE: 'Available',
  RESERVED: 'Reserved',
  COMPLETED: 'Given away',
  REMOVED: 'Removed',
};

export const claimStatusLabels: Record<ClaimStatus, string> = {
  PENDING: 'Waiting for the donor',
  ACCEPTED: 'Accepted',
  REJECTED: 'Not chosen',
  WITHDRAWN: 'Withdrawn',
};

export const reportReasonLabels: Record<ReportReason, string> = {
  SELLING_OR_ASKING_MONEY: 'Selling or asking for money',
  PROHIBITED_ITEM: 'Item not allowed',
  FAKE_OR_MISLEADING: 'Fake or misleading',
  OFFENSIVE: 'Offensive',
  OTHER: 'Something else',
};

export type Section<T> = { key: string; title: string; data: T[] };

/** Sections for "My donations": items people can still claim, reserved items, then given away. */
export function groupMyDonations<T extends { status: DonationStatus }>(items: T[]): Section<T>[] {
  return nonEmpty([
    { key: 'available', title: 'Available', data: items.filter((d) => d.status === 'AVAILABLE') },
    { key: 'reserved', title: 'Reserved – arrange pickup', data: items.filter((d) => d.status === 'RESERVED') },
    { key: 'completed', title: 'Given away', data: items.filter((d) => d.status === 'COMPLETED') },
  ]);
}

/** Sections for "My claims": accepted first (they need action), then waiting, then closed. */
export function groupMyClaims<T extends { status: ClaimStatus }>(items: T[]): Section<T>[] {
  return nonEmpty([
    { key: 'accepted', title: 'Accepted – arrange pickup', data: items.filter((c) => c.status === 'ACCEPTED') },
    { key: 'pending', title: 'Waiting for the donor', data: items.filter((c) => c.status === 'PENDING') },
    {
      key: 'closed',
      title: 'Closed',
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
  if (km < 1) {
    return 'Within 1 km';
  }
  return `About ${Math.round(km)} km away`;
}

/** "Just now", "5 min ago", "3 h ago", "2 days ago", or a date for anything older than a week. */
export function timeAgo(isoDate: string, now: Date = new Date()): string {
  const seconds = Math.max(0, (now.getTime() - new Date(isoDate).getTime()) / 1000);
  if (seconds < 60) {
    return 'Just now';
  }
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) {
    return `${minutes} min ago`;
  }
  const hours = Math.floor(minutes / 60);
  if (hours < 24) {
    return `${hours} h ago`;
  }
  const days = Math.floor(hours / 24);
  if (days < 7) {
    return days === 1 ? '1 day ago' : `${days} days ago`;
  }
  return new Date(isoDate).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' });
}
