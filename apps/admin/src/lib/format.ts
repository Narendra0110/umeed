export const reasonLabels: Record<string, string> = {
  SELLING_OR_ASKING_MONEY: 'Selling or asking for money',
  PROHIBITED_ITEM: 'Prohibited item',
  FAKE_OR_MISLEADING: 'Fake or misleading',
  OFFENSIVE: 'Offensive',
  OTHER: 'Something else',
};

export const actionLabels: Record<string, string> = {
  REVIEW_REPORT: 'Marked a report reviewed',
  DISMISS_REPORT: 'Dismissed a report',
  REMOVE_DONATION: 'Removed a listing',
  CLOSE_NEED: 'Closed a need',
  SUSPEND_USER: 'Suspended a user',
  UNSUSPEND_USER: 'Lifted a suspension',
};

const dateTime = new Intl.DateTimeFormat('en-IN', { dateStyle: 'medium', timeStyle: 'short' });

export function formatDate(value: string | null | undefined): string {
  return value ? dateTime.format(new Date(value)) : '—';
}

/** "Umeed user" when someone never set a name. */
export function nameOf(name: string | null | undefined): string {
  return name && name.trim() ? name : 'Umeed user';
}

/** Where a log entry or report points to inside the dashboard. */
export function targetHref(type: string, id: string): string | null {
  switch (type) {
    case 'DONATION':
      return `/donations/${id}`;
    case 'NEED':
      return `/needs/${id}`;
    case 'USER':
      return `/users/${id}`;
    default:
      return null;
  }
}

/** Accepts 10 digits (India) or a full number starting with +. Returns E.164, or null when it is not valid. */
export function toE164(input: string): string | null {
  const compact = input.replace(/[\s-]/g, '');
  if (/^\+[1-9]\d{7,14}$/.test(compact)) {
    return compact;
  }
  if (/^[6-9]\d{9}$/.test(compact)) {
    return `+91${compact}`;
  }
  return null;
}
