/** @type {import('next').NextConfig} */
const nextConfig = {
  typedRoutes: true,
  async rewrites() {
    // Vercel will use BACKEND_URL if you set it in your Vercel Environment Variables
    const backendUrl = process.env.BACKEND_URL || 'http://79.72.93.231:8080';
    return [
      {
        source: '/api/:path*',
        destination: `${backendUrl}/api/:path*`,
      },
    ]
  },
};

module.exports = nextConfig;