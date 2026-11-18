'use client';

import Link from 'next/link';

import { NoteAction, Pill } from '@/components/ui';
import { apiFetch } from '@/lib/api';
import { formatDate, reasonLabels } from '@/lib/format';
import type { ReportRow } from '@/lib/types';

/** Reports as cards. Open ones get Reviewed and Dismiss buttons; onChanged runs after either. */
export function ReportList({
  reports,
  showTarget = true,
  onChanged,
}: {
  reports: ReportRow[];
  showTarget?: boolean;
  onChanged: () => void;
}) {
  if (reports.length === 0) {
    return <p className="muted">No reports.</p>;
  }

  const review = (id: string, decision: 'REVIEWED' | 'DISMISSED') => async (note: string) => {
    await apiFetch(`/api/v1/admin/reports/${id}/review`, { method: 'POST', body: { decision, note } });
    onChanged();
  };

  return (
    <ul className="list">
      {reports.map((report) => {
        const href = report.donationId ? `/donations/${report.donationId}` : `/needs/${report.needId}`;
        return (
          <li key={report.id} className="card">
            <div className="row spread">
              <strong>{reasonLabels[report.reason] ?? report.reason}</strong>
              <Pill value={report.status} />
            </div>
            {showTarget ? (
              <p>
                {report.donationId ? 'Listing' : 'Need'} <Link href={href}>“{report.targetTitle}”</Link>{' '}
                <Pill value={report.targetStatus} /> by <Link href={`/users/${report.ownerId}`}>{report.ownerName}</Link>
                {report.reportsOnTarget > 1 ? (
                  <span className="warn-text"> · {report.reportsOnTarget} reports on it</span>
                ) : null}
              </p>
            ) : null}
            {report.details ? <blockquote>{report.details}</blockquote> : null}
            <p className="muted small">
              Reported by <Link href={`/users/${report.reporterId}`}>{report.reporterName}</Link> on{' '}
              {formatDate(report.createdAt)}
              {report.reviewedAt ? ` · handled ${formatDate(report.reviewedAt)}` : ''}
              {report.reviewNote ? ` · “${report.reviewNote}”` : ''}
            </p>
            {report.status === 'OPEN' ? (
              <div className="row">
                <NoteAction
                  label="Mark reviewed"
                  confirmLabel="Mark reviewed"
                  tone="primary"
                  noteRequired={false}
                  onConfirm={review(report.id, 'REVIEWED')}
                />
                <NoteAction
                  label="Dismiss"
                  confirmLabel="Dismiss report"
                  tone="secondary"
                  noteRequired={false}
                  onConfirm={review(report.id, 'DISMISSED')}
                />
              </div>
            ) : null}
          </li>
        );
      })}
    </ul>
  );
}
