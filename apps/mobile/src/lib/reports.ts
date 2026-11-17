import { useMutation } from '@tanstack/react-query';

import { apiFetch } from './api';
import type { ReportReason } from './types';

type ReportResponse = { id: string; donationId: string; reason: ReportReason; status: string; createdAt: string };

export function useCreateReport(donationId: string) {
  return useMutation({
    mutationFn: ({ reason, details }: { reason: ReportReason; details: string }) =>
      apiFetch<ReportResponse>(`/api/v1/donations/${donationId}/reports`, {
        method: 'POST',
        body: details.trim() ? { reason, details: details.trim() } : { reason },
      }),
  });
}
