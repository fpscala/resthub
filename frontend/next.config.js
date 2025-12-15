/** @type {import('next').NextConfig} */
const nextConfig = {
  output: 'standalone',
  images: {
    remotePatterns: [
      {
        protocol: 'http',
        hostname: 'localhost',
        port: '9000',
        pathname: '/**',
      },
      {
        protocol: 'https',
        hostname: 'minio.scala.uz',
        pathname: '/**',
      },
    ],
    // Disable image optimization in standalone mode to avoid sharp dependency issues
    unoptimized: true,
  },
  experimental: {
    optimizePackageImports: ['react-hot-toast'],
    serverComponentsExternalPackages: ['@aws-sdk/client-s3'],
  },
  logging: {
    fetches: {
      fullUrl: true,
    },
  },
};

module.exports = nextConfig;
