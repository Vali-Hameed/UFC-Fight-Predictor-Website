import type { Metadata } from "next";
import { Space_Grotesk, Manrope } from "next/font/google";
import "./globals.css";
import { SiteHeader } from "@/components/site-header";
import type { ReactNode } from "react";
import { Providers } from "@/app/providers";

const spaceGrotesk = Space_Grotesk({ subsets: ["latin"], variable: "--font-sans" });
const manrope = Manrope({ subsets: ["latin"], variable: "--font-alt" });

export const metadata: Metadata = {
  title: "UFC Fight Predictor",
  description: "Premium UFC prediction, leaderboard, and community platform"
};

export default function RootLayout({ children }: Readonly<{ children: ReactNode }>) {
  return (
    <html lang="en" className={`${spaceGrotesk.variable} ${manrope.variable}`}>
      <body>
        <Providers>
          <SiteHeader />
          <main className="min-h-[calc(100vh-100px)]">{children}</main>
          <footer className="border-t border-white/10 py-6 text-center text-sm text-white/40">
            <p>&copy; {new Date().getFullYear()} Vali Hameed</p>
          </footer>
        </Providers>
      </body>
    </html>
  );
}