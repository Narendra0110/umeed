'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';

import { ReportList } from '@/components/report-list';
import { AuthImage, ErrorBox, Loading, NoteAction, Pill } from '@/components/ui';
import { apiFetch } from '@/lib/api';
import { formatDate } from '@/lib/format';
import { useApi } from '@/lib/hooks';
import type { DonationDetail } from '@/lib/types';

/** One listing in any status, with its photos and every report about it. */
export default function DonationPage() {
  const { id } = useParams<{ id: string }>();
  const detail = useApi<DonationDetail>(`/api/v1/admin/donations/${id}`);

  if (detail.loading && !detail.data) {
    return <Loading />;
  }
  if (detail.error || !detail.data) {
    return <ErrorBox message={detail.error ?? 'Could not load this listing.'} onRetry={detail.reload} />;
  }
  const { donation, reports } = detail.data;

  const remove = async (note: string) => {
    detail.setData(
      await apiFetch<DonationDetail>(`/api/v1/admin/donations/${id}/remove`, { method: 'POST', body: { note } }),
    );
  };

  return (
    <>
      <p className="muted small">Listing</p>
      <div className="row spread">
        <h1>{donation.title}</h1>
        <Pill value={donation.status} />
      </div>
      <p className="muted">
        {donation.area}, {donation.city} · condition {donation.itemCondition.toLowerCase().replace('_', ' ')} · posted{' '}
        {formatDate(donation.createdAt)} by <Link href={`/users/${donation.donorId}`}>{donation.donorName}</Link>
      </p>
      <p className="description">{donation.description}</p>
      {donation.imageUrls.length > 0 ? (
        <div className="photos">
          {donation.imageUrls.map((path, index) => (
            <AuthImage key={path} path={path} alt={`${donation.title}, photo ${index + 1}`} />
          ))}
        </div>
      ) : (
        <p className="muted">No photos (they are deleted when an account is deleted).</p>
      )}

      {donation.status === 'AVAILABLE' ? (
        <section className="danger-zone">
          <p>
            Removing takes the listing down for everyone. Waiting claims are rejected and offers withdrawn. Its open
            reports are marked reviewed with your reason.
          </p>
          <NoteAction label="Remove listing" confirmLabel="Remove listing" onConfirm={remove} />
        </section>
      ) : donation.status === 'RESERVED' ? (
        <p className="muted">
          A pickup is in progress, so this listing cannot be removed. Its two people finish or cancel the pickup; if the
          donor is the problem, suspend them.
        </p>
      ) : null}

      <h2>Reports ({reports.length})</h2>
      <ReportList reports={reports} showTarget={false} onChanged={detail.reload} />
    </>
  );
}
