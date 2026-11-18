/** Shapes sent by /api/v1/admin (see AdminViews.java). */

export type Page<T> = {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
  hasNext: boolean;
};

export type AdminUser = {
  id: string;
  phoneNumber: string | null;
  displayName: string | null;
  profileComplete: boolean;
  role: 'USER' | 'ADMIN';
  status: 'ACTIVE' | 'SUSPENDED' | 'DELETED';
};

export type Stats = {
  activeUsers: number;
  suspendedUsers: number;
  newUsersLast7Days: number;
  availableListings: number;
  reservedListings: number;
  completedHandovers: number;
  removedListings: number;
  newListingsLast7Days: number;
  openNeeds: number;
  openReports: number;
};

export type ReportStatus = 'OPEN' | 'REVIEWED' | 'DISMISSED';

export type ReportRow = {
  id: string;
  reason: string;
  details: string | null;
  status: ReportStatus;
  createdAt: string;
  donationId: string | null;
  needId: string | null;
  targetTitle: string;
  targetStatus: string;
  ownerId: string;
  ownerName: string;
  reporterId: string;
  reporterName: string;
  reportsOnTarget: number;
  reviewNote: string | null;
  reviewedAt: string | null;
};

export type UserRow = {
  id: string;
  displayName: string | null;
  phoneNumber: string | null;
  role: string;
  status: string;
  createdAt: string;
};

export type ActionRow = {
  id: string;
  adminId: string;
  adminName: string;
  action: string;
  targetType: 'REPORT' | 'DONATION' | 'NEED' | 'USER';
  targetId: string;
  note: string | null;
  createdAt: string;
};

export type UserDetail = {
  user: AdminUser;
  createdAt: string;
  counts: {
    listings: number;
    availableListings: number;
    openNeeds: number;
    claimsMade: number;
    reportsAgainst: number;
    reportsMade: number;
  };
  history: ActionRow[];
};

export type Donation = {
  id: string;
  title: string;
  description: string;
  itemCondition: string;
  status: string;
  city: string;
  area: string;
  donorId: string;
  donorName: string;
  imageUrls: string[];
  createdAt: string;
};

export type Need = {
  id: string;
  title: string;
  description: string;
  status: string;
  city: string;
  area: string;
  requesterId: string;
  requesterName: string;
  pendingOfferCount: number | null;
  createdAt: string;
};

export type DonationDetail = { donation: Donation; reports: ReportRow[] };
export type NeedDetail = { need: Need; reports: ReportRow[] };
