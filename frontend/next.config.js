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
        protocol: 'http',
        hostname: 'minio.scala.uz',
        pathname: '/**',
      },
    ],
  },
  experimental: {
    optimizePackageImports: ['react-hot-toast'],
  },
};

module.exports = nextConfig;
