import type { Metadata } from "next";
import { Space_Grotesk, Manrope } from "next/font/google";
import "./globals.css";
import { SiteHeader } from "@/components/site-header";
import type { ReactNode } from "react";
import { Providers } from "@/app/providers";
import Link from "next/link";
import { CookieBanner } from "@/components/cookie-banner";
const spaceGrotesk = Space_Grotesk({ subsets: ["latin"], variable: "--font-sans" });
const manrope = Manrope({ subsets: ["latin"], variable: "--font-alt" });

export const metadata: Metadata = {
  title: "FightPicks",
  description: "Premium MMA prediction, leaderboard, and community platform",
  metadataBase: new URL('https://fightpicks.net'),
  openGraph: {
    title: "FightPicks",
    description: "Premium MMA prediction, leaderboard, and community platform",
    url: 'https://fightpicks.net',
    siteName: 'FightPicks',
    type: 'website',
    images: [
      {
        url: "/icon.png",
        width: 1200,
        height: 630,
        alt: "FightPicks Logo",
      },
    ],
  },
  twitter: {
    card: 'summary_large_image',
    title: "FightPicks",
    description: "Premium MMA prediction, leaderboard, and community platform",
    images: ['/icon.png'],
  },
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="en" className={`${spaceGrotesk.variable} ${manrope.variable}`}>
      <body>
        <Providers>
          <SiteHeader />
          <main className="min-h-[calc(100vh-100px)]">{children}</main>
          <footer className="border-t border-white/10 py-6 text-center text-sm text-white/40">
            <div className="mb-2 flex justify-center gap-4">
              <Link href="/privacy" className="hover:text-white transition">Privacy Policy</Link>
              <Link href="/terms" className="hover:text-white transition">Terms of Service</Link>
            </div>
            <p>&copy; {new Date().getFullYear()} Vali Hameed</p>
          </footer>
          <CookieBanner />
        </Providers>
      </body>
    </html>
  );
}