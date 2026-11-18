/**
 * Shapes of the backend's JSON responses (see section 10 of UMEED_PROJECT_INDEX.md).
 * Status and condition values are the backend's enum names and must stay in step with it.
 */

export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
};

export type Category = { id: string; slug: string; name: string };

export const ITEM_CONDITIONS = ['NEW', 'LIKE_NEW', 'GOOD', 'USABLE'] as const;
export type ItemCondition = (typeof ITEM_CONDITIONS)[number];

export type DonationStatus = 'AVAILABLE' | 'RESERVED' | 'COMPLETED' | 'REMOVED';

/** The public view of a donation. It never contains the pickup address or a phone number. */
export type Donation = {
  id: string;
  title: string;
  description: string;
  itemCondition: ItemCondition;
  status: DonationStatus;
  categoryId: string;
  city: string;
  area: string;
  latitude: number;
  longitude: number;
  donorId: string;
  donorName: string;
  imageUrls: string[];
  createdAt: string;
};

export type ClaimStatus = 'PENDING' | 'ACCEPTED' | 'REJECTED' | 'WITHDRAWN';

export type Claim = {
  id: string;
  donationId: string;
  donationTitle: string;
  /** The item's own status. An ACCEPTED claim on a COMPLETED item has been handed over. */
  donationStatus: DonationStatus | null;
  claimantId: string;
  claimantName: string;
  message: string | null;
  status: ClaimStatus;
  createdAt: string;
};

export type PickupParty = { name: string; phoneNumber: string };

/** Private pickup details. Returned only to the donor and the accepted receiver. */
export type Pickup = {
  donationId: string;
  /** The accepted claim; its conversation is the chat between donor and receiver. */
  claimId: string | null;
  donationTitle: string;
  status: DonationStatus;
  pickupAddress: string;
  donor: PickupParty;
  receiver: PickupParty;
  donorConfirmed: boolean;
  receiverConfirmed: boolean;
};

export type ChatMessage = {
  id: string;
  mine: boolean;
  body: string;
  createdAt: string;
  read: boolean;
};

/** One conversation. `open` is false once the pickup is finished or cancelled (read-only). */
export type ChatThread = {
  claimId: string;
  donationId: string;
  donationTitle: string;
  otherPartyName: string;
  open: boolean;
  messages: ChatMessage[];
};

/** One row of the Chats tab. The last-message fields are null until something is written. */
export type Conversation = {
  claimId: string;
  donationId: string;
  donationTitle: string;
  otherPartyName: string;
  open: boolean;
  lastMessage: string | null;
  lastMessageMine: boolean;
  lastMessageAt: string | null;
  unreadCount: number;
};

export const REPORT_REASONS = [
  'SELLING_OR_ASKING_MONEY',
  'PROHIBITED_ITEM',
  'FAKE_OR_MISLEADING',
  'OFFENSIVE',
  'OTHER',
] as const;
export type ReportReason = (typeof REPORT_REASONS)[number];
