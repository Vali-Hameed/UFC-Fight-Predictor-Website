import Link from "next/link";

export default function NotFound() {
  return (
    <div className="mx-auto flex min-h-[60vh] max-w-2xl flex-col items-center justify-center px-4 text-center">
      <div className="rounded-3xl border border-white/10 bg-panel p-8">
        <p className="text-xs uppercase tracking-[0.4em] text-gold">404</p>
        <h1 className="mt-4 text-3xl font-semibold text-white">Page not found</h1>
        <p className="mt-3 text-white/65">The requested page does not exist or has moved.</p>
        <Link href="/" className="mt-6 inline-flex rounded-2xl bg-accent px-5 py-3 font-semibold text-white">
          Back home
        </Link>
      </div>
    </div>
  );
}