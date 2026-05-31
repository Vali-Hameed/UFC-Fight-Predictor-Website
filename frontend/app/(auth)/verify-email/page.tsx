"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { SectionCard } from "@/components/section-card";
import { apiFetch } from "@/lib/api";

export default function VerifyEmailPage() {
  const router = useRouter();
  const [message, setMessage] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const token = String(formData.get("token") ?? "");

    setLoading(true);
    setMessage(null);
    try {
      await apiFetch<string>(`/api/v1/registration/confirm?token=${token}`, {
        method: "GET"
      });
      setSuccess(true);
      setMessage("Email verified successfully! You can now log in.");
      setTimeout(() => {
        router.push("/login");
      }, 3000);
    } catch {
      setMessage("Verification failed. Please check your token and try again.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Auth" title="Verify email" description="Paste the token from the verification email to activate your account.">
        <form className="space-y-4" onSubmit={handleSubmit}>
          <input name="token" className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35" placeholder="Verification token" />
          {message ? <p className={`text-sm ${success ? "text-green-400" : "text-red-300"}`}>{message}</p> : null}
          <button disabled={loading || success} className="w-full rounded-2xl bg-accent px-4 py-3 font-semibold text-white disabled:opacity-60">{loading ? "Verifying..." : "Verify"}</button>
        </form>
      </SectionCard>
    </div>
  );
}