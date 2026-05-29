"use client";

export default function ErrorPage({ reset }: { reset: () => void }) {
  return (
    <div className="mx-auto flex min-h-[60vh] max-w-2xl flex-col items-center justify-center px-4 text-center">
      <div className="rounded-3xl border border-white/10 bg-panel p-8">
        <p className="text-xs uppercase tracking-[0.4em] text-gold">500</p>
        <h1 className="mt-4 text-3xl font-semibold text-white">Something went wrong</h1>
        <p className="mt-3 text-white/65">The page hit an unexpected problem. Try again in a moment.</p>
        <button onClick={reset} className="mt-6 rounded-2xl bg-accent px-5 py-3 font-semibold text-white">
          Retry
        </button>
      </div>
    </div>
  );
}