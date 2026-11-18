'use client';

import { useParams } from 'next/navigation';

import { ActionTable } from '@/components/action-table';
import { ErrorBox, Loading, NoteAction, Pill } from '@/components/ui';
import { apiFetch } from '@/lib/api';
import { formatDate, nameOf } from '@/lib/format';
import { useApi } from '@/lib/hooks';
import type { UserDetail } from '@/lib/types';

/** One person: who they are, what they have done, and suspension. */
export default function UserPage() {
  const { id } = useParams<{ id: string }>();
  const detail = useApi<UserDetail>(`/api/v1/admin/users/${id}`);

  if (detail.loading && !detail.data) {
    return <Loading />;
  }
  if (detail.error || !detail.data) {
    return <ErrorBox message={detail.error ?? 'Could not load this user.'} onRetry={detail.reload} />;
  }
  const { user, counts, history, createdAt } = detail.data;

  const act = (step: 'suspend' | 'unsuspend') => async (note: string) => {
    detail.setData(
      await apiFetch<UserDetail>(`/api/v1/admin/users/${id}/${step}`, { method: 'POST', body: { note } }),
    );
  };

  return (
    <>
      <p className="muted small">User</p>
      <div className="row spread">
        <h1>{user.status === 'DELETED' ? 'Deleted account' : nameOf(user.displayName)}</h1>
        <div className="row">
          {user.role === 'ADMIN' ? <Pill value="ADMIN" /> : null}
          <Pill value={user.status} />
        </div>
      </div>
      <p className="muted">
        {user.phoneNumber ?? 'No phone number'} · joined {formatDate(createdAt)}
      </p>

      <section className="cards">
        <Count label="Listings" value={counts.listings} hint={`${counts.availableListings} available`} />
        <Count label="Open needs" value={counts.openNeeds} />
        <Count label="Claims made" value={counts.claimsMade} />
        <Count label="Reports against them" value={counts.reportsAgainst} />
        <Count label="Reports they made" value={counts.reportsMade} />
      </section>

      {user.role !== 'ADMIN' && user.status === 'ACTIVE' ? (
        <section className="danger-zone">
          <p>
            Suspending stops them from posting, claiming, offering and chatting. Their listings and needs are hidden from
            everyone until you lift it. Nothing is deleted.
          </p>
          <NoteAction label="Suspend user" confirmLabel="Suspend user" onConfirm={act('suspend')} />
        </section>
      ) : null}
      {user.status === 'SUSPENDED' ? (
        <section className="danger-zone">
          <p>Lifting the suspension brings back their listings and needs.</p>
          <NoteAction label="Lift suspension" confirmLabel="Lift suspension" tone="primary" onConfirm={act('unsuspend')} />
        </section>
      ) : null}

      <h2>Admin steps about this user</h2>
      <ActionTable actions={history} />
    </>
  );
}

function Count({ label, value, hint }: { label: string; value: number; hint?: string }) {
  return (
    <div className="card">
      <div className="muted">{label}</div>
      <div className="big">{value}</div>
      {hint ? <div className="muted small">{hint}</div> : null}
    </div>
  );
}
