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
  },
  experimental: {
    optimizePackageImports: ['react-hot-toast'],
    serverComponentsExternalPackages: ['@aws-sdk/client-s3'],
  },
};

module.exports = nextConfig;
