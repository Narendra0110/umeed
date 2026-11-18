import { useMutation } from '@tanstack/react-query';

import { apiFetch } from './api';
import type { ReportReason } from './types';

/** What is being reported: a listing (donation) or a need. */
export type ReportTarget = { kind: 'donation' | 'need'; id: string };

type ReportResponse = {
  id: string;
  donationId: string | null;
  needId: string | null;
  reason: ReportReason;
  status: string;
  createdAt: string;
};

export function reportPath({ kind, id }: ReportTarget) {
  return kind === 'need' ? `/api/v1/needs/${id}/reports` : `/api/v1/donations/${id}/reports`;
}

export function useCreateReport(target: ReportTarget) {
  return useMutation({
    mutationFn: ({ reason, details }: { reason: ReportReason; details: string }) =>
      apiFetch<ReportResponse>(reportPath(target), {
        method: 'POST',
        body: details.trim() ? { reason, details: details.trim() } : { reason },
      }),
  });
}
