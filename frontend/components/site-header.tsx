"use client";

import Link from "next/link";
import type { Route } from "next";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/session";
import { useEffect, useState } from "react";
import { getUnreadNotificationCount } from "@/lib/api";
import { toast } from "sonner";

const links = [
  { href: "/events", label: "Events" },
  { href: "/leaderboard", label: "Leaderboard" },
  { href: "/simulator", label: "Simulator" },
  { href: "/notifications", label: "Notifications" },
  { href: "/admin", label: "Admin" }
] satisfies ReadonlyArray<{ href: Route; label: string }>;

export function SiteHeader() {
  const { token, loading, user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);

  useEffect(() => {
    if (token) {
      getUnreadNotificationCount(token).then(setUnreadCount).catch(() => {});
    } else {
      setUnreadCount(0);
    }
  }, [token, pathname]);

  const handleLogout = async () => {
    await logout();
    router.push("/");
    router.refresh();
  };

  const toggleMobileMenu = () => {
    setIsMobileMenuOpen((prev) => !prev);
  };

  return (
    <header className="sticky top-0 z-40 border-b border-white/10 bg-bg/85 backdrop-blur-xl">
      <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6 lg:px-8">
        <Link href="/" className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-white/10 bg-gradient-to-br from-accent to-gold text-sm font-black text-white shadow-glow">
            UFC
          </div>
          <div>
            <div className="text-sm font-semibold uppercase tracking-[0.35em] text-white/60">Fight Predictor</div>
            <div className="text-xs text-white/40 hidden sm:block">Prediction engine and community scoreboard</div>
          </div>
        </Link>

        {/* Desktop Nav */}
        <nav className="hidden items-center gap-6 text-sm text-white/70 md:flex">
          {links.map((link) => {
            if (link.href === "/admin" && user?.role !== "ROLE_ADMIN") return null;
            return (
              <Link key={link.href} href={link.href} className={`relative transition hover:text-white ${pathname === link.href ? "text-white" : ""}`}>
                {link.label}
                {link.href === "/notifications" && unreadCount > 0 && (
                  <span className="absolute -right-5 -top-2 flex h-4 w-4 items-center justify-center rounded-full bg-accent text-[9px] font-bold text-white shadow-glow">
                    {unreadCount > 99 ? "99+" : unreadCount}
                  </span>
                )}
              </Link>
            );
          })}
          {user && (
            <Link href={`/profile/${user.username}`} className={`transition hover:text-white ${pathname.startsWith("/profile") ? "text-white" : ""}`}>
              Profile
            </Link>
          )}
        </nav>

        <div className="flex items-center gap-3">
          <div className="hidden sm:block rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-white/80">
            {loading ? "Syncing session" : token ? "Signed in" : "Dark mode default"}
          </div>
          {token ? (
            <button onClick={handleLogout} className="hidden sm:block rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-white/80 transition hover:bg-white/10">
              Logout
            </button>
          ) : (
            <Link href="/login" className="hidden sm:block rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-white/80 transition hover:bg-white/10">
              Login
            </Link>
          )}
          
          {/* Mobile Menu Toggle Button */}
          <button onClick={toggleMobileMenu} className="md:hidden flex items-center justify-center rounded-lg p-2 text-white/70 hover:bg-white/10 hover:text-white transition">
            <svg className="h-6 w-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              {isMobileMenuOpen ? (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              ) : (
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
              )}
            </svg>
          </button>
        </div>
      </div>

      {/* Mobile Nav Dropdown */}
      {isMobileMenuOpen && (
        <div className="md:hidden border-t border-white/10 bg-bg">
          <nav className="flex flex-col gap-4 p-4 text-sm text-white/70">
            {links.map((link) => {
              if (link.href === "/admin" && user?.role !== "ROLE_ADMIN") return null;
              return (
                <Link 
                  key={link.href} 
                  href={link.href} 
                  onClick={() => setIsMobileMenuOpen(false)}
                  className={`relative flex items-center transition hover:text-white ${pathname === link.href ? "text-white" : ""}`}
                >
                  {link.label}
                  {link.href === "/notifications" && unreadCount > 0 && (
                    <span className="ml-2 flex h-5 px-1.5 items-center justify-center rounded-full bg-accent text-[10px] font-bold text-white shadow-glow">
                      {unreadCount > 99 ? "99+" : unreadCount}
                    </span>
                  )}
                </Link>
              );
            })}
            {user && (
              <Link 
                href={`/profile/${user.username}`} 
                onClick={() => setIsMobileMenuOpen(false)}
                className={`transition hover:text-white ${pathname.startsWith("/profile") ? "text-white" : ""}`}
              >
                Profile
              </Link>
            )}
            <hr className="border-white/10" />
            {token ? (
              <button 
                onClick={() => { handleLogout(); setIsMobileMenuOpen(false); }} 
                className="text-left text-white/80 transition hover:text-white"
              >
                Logout
              </button>
            ) : (
              <Link 
                href="/login" 
                onClick={() => setIsMobileMenuOpen(false)}
                className="text-white/80 transition hover:text-white"
              >
                Login
              </Link>
            )}
          </nav>
        </div>
      )}
    </header>
  );
}