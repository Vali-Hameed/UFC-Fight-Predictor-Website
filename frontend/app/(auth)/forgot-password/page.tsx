"use client";

import { FormEvent, useState } from "react";
import { SectionCard } from "@/components/section-card";
import { apiFetch } from "@/lib/api";
import { toast } from "sonner";

export default function ForgotPasswordPage() {
  const [loading, setLoading] = useState(false);

  const handleRequest = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const email = String(formData.get("email") ?? "");
    setLoading(true);
    try {
      await apiFetch("/api/v1/password/request", {
        method: "POST",
        body: JSON.stringify({ email })
      });
      toast.success("Reset link requested. Check your email inbox.");
    } catch {
      toast.error("Could not request a reset link.");
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
