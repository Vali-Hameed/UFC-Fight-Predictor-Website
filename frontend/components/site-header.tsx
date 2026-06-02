"use client";

import Link from "next/link";
import type { Route } from "next";
import { usePathname, useRouter } from "next/navigation";
import { useAuth } from "@/lib/session";

const links = [
  { href: "/events", label: "Events" },
  { href: "/leaderboard", label: "Leaderboard" },
  { href: "/forum", label: "Forum" },
  { href: "/notifications", label: "Notifications" },
  { href: "/admin", label: "Admin" }
] satisfies ReadonlyArray<{ href: Route; label: string }>;

export function SiteHeader() {
  const { token, loading, user, logout } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  const handleLogout = async () => {
    await logout();
    router.push("/");
    router.refresh();
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
            <div className="text-xs text-white/40">Prediction engine and community scoreboard</div>
          </div>
        </Link>
        <nav className="hidden items-center gap-6 text-sm text-white/70 md:flex">
          {links.map((link) => (
            <Link key={link.href} href={link.href} className={`transition hover:text-white ${pathname === link.href ? "text-white" : ""}`}>
              {link.label}
            </Link>
          ))}
          {user && (
            <Link href={`/profile/${user.username}`} className={`transition hover:text-white ${pathname.startsWith("/profile") ? "text-white" : ""}`}>
              Profile
            </Link>
          )}
        </nav>
        <div className="flex items-center gap-3">
          <div className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-white/80">
            {loading ? "Syncing session" : token ? "Signed in" : "Dark mode default"}
          </div>
          {token ? (
            <button onClick={handleLogout} className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-white/80 transition hover:bg-white/10">
              Logout
            </button>
          ) : (
            <Link href="/login" className="rounded-full border border-white/10 bg-white/5 px-4 py-2 text-sm text-white/80 transition hover:bg-white/10">
              Login
            </Link>
          )}
        </div>
      </div>
    </header>
  );
}