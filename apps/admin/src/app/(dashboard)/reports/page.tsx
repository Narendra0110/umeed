'use client';

import { useState } from 'react';

import { ReportList } from '@/components/report-list';
import { ErrorBox, Loading, Pager } from '@/components/ui';
import { useApi } from '@/lib/hooks';
import type { Page, ReportRow, ReportStatus } from '@/lib/types';

const tabs: { value: ReportStatus; label: string }[] = [
  { value: 'OPEN', label: 'Open' },
  { value: 'REVIEWED', label: 'Reviewed' },
  { value: 'DISMISSED', label: 'Dismissed' },
];

/**
 * Reports. Open ones are oldest first. To act on a listing or need itself (remove, close), open it from the
 * report; doing so marks its open reports reviewed with the same reason.
 */
export default function ReportsPage() {
  const [status, setStatus] = useState<ReportStatus>('OPEN');
  const [page, setPage] = useState(0);
  const reports = useApi<Page<ReportRow>>(`/api/v1/admin/reports?status=${status}&page=${page}&size=20`);

  return (
    <>
      <h1>Reports</h1>
      <div className="tabs" role="tablist">
        {tabs.map((tab) => (
          <button
            key={tab.value}
            type="button"
            role="tab"
            aria-selected={status === tab.value}
            className={status === tab.value ? 'active' : undefined}
            onClick={() => {
              setStatus(tab.value);
              setPage(0);
            }}>
            {tab.label}
          </button>
        ))}
      </div>
      {reports.loading && !reports.data ? <Loading /> : null}
      {reports.error ? <ErrorBox message={reports.error} onRetry={reports.reload} /> : null}
      {reports.data ? (
        <>
          <p className="muted">{reports.data.totalItems} in total</p>
          <ReportList reports={reports.data.items} onChanged={reports.reload} />
          <Pager page={page} totalPages={reports.data.totalPages} onChange={setPage} />
        </>
      ) : null}
    </>
  );
}
