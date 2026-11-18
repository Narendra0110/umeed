'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';

import { ErrorBox, Loading, Pager, Pill } from '@/components/ui';
import { formatDate, nameOf } from '@/lib/format';
import { useApi } from '@/lib/hooks';
import type { Page, UserRow } from '@/lib/types';

/** Find people by name or phone number. Deleted accounts are not listed. */
export default function UsersPage() {
  const [text, setText] = useState('');
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);

  // Search after a short pause in typing.
  useEffect(() => {
    const timer = setTimeout(() => {
      setQuery(text.trim());
      setPage(0);
    }, 350);
    return () => clearTimeout(timer);
  }, [text]);

  const params = new URLSearchParams({ page: String(page), size: '20' });
  if (query) params.set('q', query);
  if (status) params.set('status', status);
  const users = useApi<Page<UserRow>>(`/api/v1/admin/users?${params.toString()}`);

  return (
    <>
      <h1>Users</h1>
      <div className="row filters">
        <input
          type="search"
          placeholder="Name or phone number"
          value={text}
          maxLength={80}
          onChange={(event) => setText(event.target.value)}
        />
        <select
          value={status}
          onChange={(event) => {
            setStatus(event.target.value);
            setPage(0);
          }}>
          <option value="">Any status</option>
          <option value="ACTIVE">Active</option>
          <option value="SUSPENDED">Suspended</option>
        </select>
      </div>
      {users.loading && !users.data ? <Loading /> : null}
      {users.error ? <ErrorBox message={users.error} onRetry={users.reload} /> : null}
      {users.data ? (
        <>
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Phone</th>
                <th>Status</th>
                <th>Joined</th>
              </tr>
            </thead>
            <tbody>
              {users.data.items.map((user) => (
                <tr key={user.id}>
                  <td>
                    <Link href={`/users/${user.id}`}>{nameOf(user.displayName)}</Link>
                    {user.role === 'ADMIN' ? <> <Pill value="ADMIN" /></> : null}
                  </td>
                  <td>{user.phoneNumber}</td>
                  <td>
                    <Pill value={user.status} />
                  </td>
                  <td>{formatDate(user.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
          {users.data.items.length === 0 ? <p className="muted">Nobody matches.</p> : null}
          <Pager page={page} totalPages={users.data.totalPages} onChange={setPage} />
        </>
      ) : null}
    </>
  );
}
