import type { NextConfig } from 'next';

/**
 * The dashboard calls the backend through its own /api path, so the browser only ever talks to one address
 * and the backend needs no CORS setup. UMEED_API_URL is where the Spring Boot backend runs.
 */
const backend = process.env.UMEED_API_URL ?? 'http://localhost:8080';

const nextConfig: NextConfig = {
  poweredByHeader: false,
  async rewrites() {
    return [{ source: '/api/:path*', destination: `${backend}/api/:path*` }];
  },
  async headers() {
    return [
      {
        source: '/:path*',
        headers: [
          { key: 'X-Frame-Options', value: 'DENY' },
          { key: 'X-Content-Type-Options', value: 'nosniff' },
          { key: 'Referrer-Policy', value: 'same-origin' },
          { key: 'X-Robots-Tag', value: 'noindex, nofollow' },
        ],
      },
    ];
  },
};

export default nextConfig;
