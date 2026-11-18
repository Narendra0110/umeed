'use client';

import { useState } from 'react';

import { ActionTable } from '@/components/action-table';
import { ErrorBox, Loading, Pager } from '@/components/ui';
import { useApi } from '@/lib/hooks';
import type { ActionRow, Page } from '@/lib/types';

/** Every step any admin took, newest first. */
export default function LogPage() {
  const [page, setPage] = useState(0);
  const actions = useApi<Page<ActionRow>>(`/api/v1/admin/actions?page=${page}&size=30`);

  return (
    <>
      <h1>Admin log</h1>
      {actions.loading && !actions.data ? <Loading /> : null}
      {actions.error ? <ErrorBox message={actions.error} onRetry={actions.reload} /> : null}
      {actions.data ? (
        <>
          <ActionTable actions={actions.data.items} />
          <Pager page={page} totalPages={actions.data.totalPages} onChange={setPage} />
        </>
      ) : null}
    </>
  );
}
