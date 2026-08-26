import type { NextConfig } from 'next';

const nextConfig: NextConfig = {
  transpilePackages: ['@dorja/contracts', '@dorja/domain', '@dorja/ui-tokens'],
  images: {
    remotePatterns: [
      { protocol: 'http', hostname: 'localhost', port: '9000' },
    ],
  },
};

export default nextConfig;
