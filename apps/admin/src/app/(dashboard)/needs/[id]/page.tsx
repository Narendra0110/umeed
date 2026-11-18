'use client';

import Link from 'next/link';
import { useParams } from 'next/navigation';

import { ReportList } from '@/components/report-list';
import { ErrorBox, Loading, NoteAction, Pill } from '@/components/ui';
import { apiFetch } from '@/lib/api';
import { formatDate } from '@/lib/format';
import { useApi } from '@/lib/hooks';
import type { NeedDetail } from '@/lib/types';

/** One need in any status, with every report about it. */
export default function NeedPage() {
  const { id } = useParams<{ id: string }>();
  const detail = useApi<NeedDetail>(`/api/v1/admin/needs/${id}`);

  if (detail.loading && !detail.data) {
    return <Loading />;
  }
  if (detail.error || !detail.data) {
    return <ErrorBox message={detail.error ?? 'Could not load this need.'} onRetry={detail.reload} />;
  }
  const { need, reports } = detail.data;

  const close = async (note: string) => {
    detail.setData(await apiFetch<NeedDetail>(`/api/v1/admin/needs/${id}/close`, { method: 'POST', body: { note } }));
  };

  return (
    <>
      <p className="muted small">Need</p>
      <div className="row spread">
        <h1>{need.title}</h1>
        <Pill value={need.status} />
      </div>
      <p className="muted">
        {need.area}, {need.city} · asked {formatDate(need.createdAt)} by{' '}
        <Link href={`/users/${need.requesterId}`}>{need.requesterName}</Link>
        {need.pendingOfferCount ? ` · ${need.pendingOfferCount} offers waiting` : ''}
      </p>
      <p className="description">{need.description}</p>

      {need.status === 'OPEN' ? (
        <section className="danger-zone">
          <p>Closing hides the need from everyone and declines its waiting offers. Its open reports are marked reviewed.</p>
          <NoteAction label="Close need" confirmLabel="Close need" onConfirm={close} />
        </section>
      ) : null}

      <h2>Reports ({reports.length})</h2>
      <ReportList reports={reports} showTarget={false} onChanged={detail.reload} />
    </>
  );
}
