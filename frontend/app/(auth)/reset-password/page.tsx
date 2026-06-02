"use client";

import { FormEvent, useEffect, useState } from "react";
import { SectionCard } from "@/components/section-card";
import { apiFetch } from "@/lib/api";
import { PasswordInput } from "@/components/password-input";
import { useRouter } from "next/navigation";

export default function ResetPasswordPage() {
  const [tokenFromQuery, setTokenFromQuery] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  useEffect(() => {
    const queryToken = new URLSearchParams(window.location.search).get("token") ?? "";
    setTokenFromQuery(queryToken);
  }, []);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const token = tokenFromQuery;
    const password = String(formData.get("password") ?? "");
    const confirmPassword = String(formData.get("confirmPassword") ?? "");

    if (!token) {
        setMessage("Invalid or missing reset token.");
        return;
    }

    if (password !== confirmPassword) {
        setMessage("Passwords do not match.");
        return;
    }

    setLoading(true);
    setMessage(null);
    try {
      await apiFetch("/api/v1/password/confirm", {
        method: "POST",
        body: JSON.stringify({ token, password })
      });
      setMessage("Password updated successfully. You will be redirected to login.");
      setTimeout(() => router.push("/login"), 2000);
    } catch {
      setMessage("Reset failed. The token may be expired or invalid.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Auth" title="Reset password" description="Enter your new password below.">
        <form className="grid gap-4" onSubmit={handleSubmit}>
          <PasswordInput name="password" placeholder="New password" />
          <PasswordInput name="confirmPassword" placeholder="Confirm new password" />
          <button disabled={loading} className="rounded-2xl bg-accent px-4 py-3 font-semibold text-white disabled:opacity-60">{loading ? "Updating..." : "Update password"}</button>
        </form>
        {message ? <p className="mt-4 text-sm text-white/70">{message}</p> : null}
      </SectionCard>
    </div>
  );
}