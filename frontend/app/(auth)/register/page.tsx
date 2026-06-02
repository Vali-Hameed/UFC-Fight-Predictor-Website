"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { SectionCard } from "@/components/section-card";
import { apiFetch } from "@/lib/api";
import { PasswordInput } from "@/components/password-input";
import { toast } from "sonner";

export default function RegisterPage() {
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [unverifiedEmail, setUnverifiedEmail] = useState<string | null>(null);
  const [resending, setResending] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setUnverifiedEmail(null);
    const formData = new FormData(event.currentTarget);
    const payload = {
      firstName: String(formData.get("firstName") ?? ""),
      lastName: String(formData.get("lastName") ?? ""),
      userName: String(formData.get("username") ?? ""),
      email: String(formData.get("email") ?? ""),
      password: String(formData.get("password") ?? "")
    };
    const confirmPassword = String(formData.get("confirmPassword") ?? "");

    if (payload.password !== confirmPassword) {
      toast.error("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      await apiFetch<string>("/api/v1/registration", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      toast.success("Account created! Please check your email.");
      router.push("/verify-email");
      router.refresh();
    } catch (error: any) {
      const msg = error?.message || "";
      if (msg.includes("registered but not verified")) {
        setUnverifiedEmail(payload.email);
        toast.error("This account exists but is not verified.");
      } else {
        toast.error(msg || "Registration failed. Please check your details and try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  const handleResend = async () => {
    if (!unverifiedEmail) return;
    setResending(true);
    try {
      const response = await fetch("http://localhost:8080/api/v1/registration/resend-verification", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: unverifiedEmail })
      });
      if (response.ok) {
        toast.success("Verification email resent. Check your inbox.");
        setUnverifiedEmail(null);
      } else {
        toast.error("Failed to resend email. Please try again.");
      }
    } catch {
      toast.error("An error occurred while resending the email.");
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="mx-auto max-w-xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Auth" title="Create account" description="Email verification is required before predictions become active.">
        <form className="grid gap-4" onSubmit={handleSubmit}>
          <input name="firstName" className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35" placeholder="First name" />
          <input name="lastName" className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35" placeholder="Last name" />
          <input name="username" className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35" placeholder="Username" />
          <input name="email" className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35" placeholder="Email" />
          <PasswordInput name="password" placeholder="Password" />
          <PasswordInput name="confirmPassword" placeholder="Confirm password" />
          <button disabled={loading} className="rounded-2xl bg-accent px-4 py-3 font-semibold text-white disabled:opacity-60">{loading ? "Creating account..." : "Register"}</button>
          
          {unverifiedEmail && (
            <div className="mt-2 p-4 rounded-xl border border-yellow-500/30 bg-yellow-500/10 text-center">
              <p className="text-sm text-yellow-200 mb-3">Your account is registered but not verified.</p>
              <button 
                type="button" 
                onClick={handleResend} 
                disabled={resending}
                className="w-full rounded-xl bg-yellow-500/20 px-4 py-2 text-sm font-semibold text-yellow-400 hover:bg-yellow-500/30 disabled:opacity-50 transition-colors"
              >
                {resending ? "Resending..." : "Resend Verification Email"}
              </button>
            </div>
          )}
        </form>
      </SectionCard>
    </div>
  );
}