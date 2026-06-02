"use client";

import { FormEvent, useState } from "react";
import { SectionCard } from "@/components/section-card";
import { apiFetch } from "@/lib/api";

export default function ForgotPasswordPage() {
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleRequest = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const email = String(formData.get("email") ?? "");
    setLoading(true);
    setMessage(null);
    try {
      await apiFetch("/api/v1/password/request", {
        method: "POST",
        body: JSON.stringify({ email })
      });
      setMessage("Reset link requested. Check your email inbox.");
    } catch {
      setMessage("Could not request a reset link.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Auth" title="Forgot password" description="Enter your email to receive a password reset link.">
        <form className="grid gap-4" onSubmit={handleRequest}>
          <input name="email" className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35" placeholder="Email for reset link" required />
          <button disabled={loading} className="rounded-2xl bg-accent px-4 py-3 font-semibold text-white disabled:opacity-60">Request reset link</button>
        </form>
        {message ? <p className="mt-4 text-sm text-white/70">{message}</p> : null}
        <div className="mt-6 text-center text-sm text-white/70">
          Remember your password?{" "}
          <a href="/login" className="text-accent hover:underline">
            Sign in
          </a>
        </div>
      </SectionCard>
    </div>
  );
}
