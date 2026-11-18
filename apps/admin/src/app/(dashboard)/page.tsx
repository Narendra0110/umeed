'use client';

import Link from 'next/link';

import { ErrorBox, Loading } from '@/components/ui';
import { useApi } from '@/lib/hooks';
import type { Stats } from '@/lib/types';

/** Overview: the numbers that matter for moderation, with the open reports first. */
export default function OverviewPage() {
  const stats = useApi<Stats>('/api/v1/admin/stats');

  if (stats.loading && !stats.data) {
    return <Loading />;
  }
  if (stats.error || !stats.data) {
    return <ErrorBox message={stats.error ?? 'Could not load the numbers.'} onRetry={stats.reload} />;
  }
  const s = stats.data;

  return (
    <>
      <div className="row spread">
        <h1>Overview</h1>
        <button type="button" className="button secondary small" onClick={stats.reload}>
          Refresh
        </button>
      </div>
      <Link href="/reports" className={`callout ${s.openReports > 0 ? 'warn' : ''}`}>
        <span className="big">{s.openReports}</span> open report{s.openReports === 1 ? '' : 's'} waiting for review →
      </Link>
      <section className="cards">
        <Card label="Active users" value={s.activeUsers} hint={`${s.newUsersLast7Days} new in 7 days`} />
        <Card label="Suspended users" value={s.suspendedUsers} />
        <Card label="Available listings" value={s.availableListings} hint={`${s.newListingsLast7Days} new in 7 days`} />
        <Card label="Pickups in progress" value={s.reservedListings} />
        <Card label="Items handed over" value={s.completedHandovers} />
        <Card label="Removed listings" value={s.removedListings} />
        <Card label="Open needs" value={s.openNeeds} />
      </section>
    </>
  );
}

function Card({ label, value, hint }: { label: string; value: number; hint?: string }) {
  return (
    <div className="card">
      <div className="muted">{label}</div>
      <div className="big">{value.toLocaleString('en-IN')}</div>
      {hint ? <div className="muted small">{hint}</div> : null}
    </div>
  );
}
