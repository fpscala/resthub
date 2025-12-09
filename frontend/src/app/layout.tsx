import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import { Providers } from './providers';
import { Navbar } from '@/components/Navbar';
import { ToasterProvider } from '@/components/ToasterProvider';

const inter = Inter({ subsets: ['latin'] });

export const metadata: Metadata = {
  title: 'NestHub - Rental Marketplace',
  description: 'Find your perfect rental property with NestHub',
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={inter.className}>
        <Providers>
          <div className="flex min-h-screen flex-col">
            <Navbar />
            <main className="flex-1">{children}</main>
            <footer className="border-t border-gray-200 bg-white py-6">
              <div className="container-custom text-center text-sm text-gray-600">
                © 2024 NestHub. All rights reserved.
              </div>
            </footer>
          </div>
          <ToasterProvider />
        </Providers>
      </body>
    </html>
  );
}
