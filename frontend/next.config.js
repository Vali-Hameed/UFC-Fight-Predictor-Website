/** @type {import('next').NextConfig} */
const nextConfig = {
  typedRoutes: true,
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: 'http://79.72.93.231:8080/api/:path*',
      },
    ]
  },
};

module.exports = nextConfig;