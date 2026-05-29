"use client";

import { FormEvent, useEffect, useState } from "react";
import { SectionCard } from "@/components/section-card";
import { apiFetch } from "@/lib/api";

export default function ResetPasswordPage() {
  const [tokenFromQuery, setTokenFromQuery] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const queryToken = new URLSearchParams(window.location.search).get("token") ?? "";
    setTokenFromQuery(queryToken);
  }, []);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const token = String(formData.get("token") ?? tokenFromQuery);
    const password = String(formData.get("password") ?? "");

    setLoading(true);
    setMessage(null);
    try {
      await apiFetch("/api/v1/password/confirm", {
        method: "POST",
        body: JSON.stringify({ token, password })
      });
      setMessage("Password updated. You can sign in now.");
    } catch {
      setMessage("Reset failed. The token may be expired or invalid.");
    } finally {
      setLoading(false);
    }
  };

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
      <SectionCard eyebrow="Auth" title="Reset password" description="Reset tokens expire after 30 minutes and are single-use.">
        <form className="grid gap-4" onSubmit={handleRequest}>
          <input name="email" className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35" placeholder="Email for reset link" defaultValue="" />
          <button disabled={loading} className="rounded-2xl bg-accent px-4 py-3 font-semibold text-white disabled:opacity-60">Request reset link</button>
        </form>
        <form className="mt-6 grid gap-4" onSubmit={handleSubmit}>
          <input name="token" className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35" placeholder="Reset token" defaultValue={tokenFromQuery} />
          <input name="password" className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35" placeholder="New password" type="password" />
          <button disabled={loading} className="rounded-2xl bg-white/10 px-4 py-3 font-semibold text-white disabled:opacity-60">{loading ? "Updating..." : "Update password"}</button>
        </form>
        {message ? <p className="mt-4 text-sm text-white/70">{message}</p> : null}
      </SectionCard>
    </div>
  );
}