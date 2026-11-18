'use client';

import { signOut } from 'firebase/auth';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { type ReactNode, useEffect } from 'react';

import { ErrorBox, Loading } from '@/components/ui';
import { firebaseAuth } from '@/lib/firebase';
import { nameOf } from '@/lib/format';
import { useApi, useFirebaseUser } from '@/lib/hooks';
import type { AdminUser } from '@/lib/types';

const links = [
  { href: '/', label: 'Overview' },
  { href: '/reports', label: 'Reports' },
  { href: '/users', label: 'Users' },
  { href: '/log', label: 'Admin log' },
];

/**
 * Every dashboard page sits inside this. It sends visitors who are not logged in to /login and shows
 * nothing but a refusal to logged-in people who are not admins. The backend checks the role again on
 * every request; this screen only keeps the pages from loading for nothing.
 */
export default function DashboardLayout({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const { ready, user } = useFirebaseUser();
  const me = useApi<AdminUser>(user ? '/api/v1/admin/me' : null);

  useEffect(() => {
    if (ready && !user) {
      router.replace('/login');
    }
  }, [ready, user, router]);

  const logOut = async () => {
    const auth = firebaseAuth();
    if (auth) {
      await signOut(auth);
    }
    router.replace('/login');
  };

  if (!ready || !user || (me.loading && !me.data)) {
    return (
      <main className="page">
        <Loading />
      </main>
    );
  }
  if (me.error || !me.data) {
    return (
      <main className="page">
        <h1>Umeed Admin</h1>
        <ErrorBox
          message={
            me.error === 'Admins only'
              ? 'This account is not an admin. Ask the project owner to give it the admin role, then log in again.'
              : (me.error ?? 'Could not check your account.')
          }
          onRetry={me.reload}
        />
        <button type="button" className="button secondary" onClick={logOut}>
          Log out
        </button>
      </main>
    );
  }

  return (
    <div className="shell">
      <header className="topbar">
        <strong className="brand">Umeed Admin</strong>
        <nav>
          {links.map((link) => {
            const active = link.href === '/' ? pathname === '/' : pathname.startsWith(link.href);
            return (
              <Link key={link.href} href={link.href} className={active ? 'active' : undefined}>
                {link.label}
              </Link>
            );
          })}
        </nav>
        <span className="muted who">{nameOf(me.data.displayName)}</span>
        <button type="button" className="button secondary small" onClick={logOut}>
          Log out
        </button>
      </header>
      <main className="page">{children}</main>
    </div>
  );
}
