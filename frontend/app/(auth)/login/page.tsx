"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { SectionCard } from "@/components/section-card";
import { useAuth } from "@/lib/session";
import { toast } from "sonner";
import { PasswordInput } from "@/components/password-input";

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [unverifiedEmail, setUnverifiedEmail] = useState<string | null>(null);
  const [resending, setResending] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setUnverifiedEmail(null);
    const formData = new FormData(event.currentTarget);
    const username = String(formData.get("username") ?? "");
    const password = String(formData.get("password") ?? "");

    setLoading(true);
    try {
      await login(username, password);
      toast.success("Successfully logged in.");
      router.push("/");
      router.refresh();
    } catch (error: any) {
      if (error?.errorCode === "USER_DISABLED") {
        setUnverifiedEmail(username);
        toast.error("Account is not verified. Please verify your email.");
      } else if (error?.errorCode === "USER_LOCKED") {
        toast.error("Your account is locked. Please contact an administrator.");
      } else {
        toast.error("Login failed. Check your credentials and try again.");
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
      <SectionCard eyebrow="Auth" title="Sign in" description="JWT access tokens stay in memory and refresh tokens are stored in HttpOnly cookies.">
        <form className="space-y-4" onSubmit={handleSubmit}>
          <input name="username" className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35" placeholder="Username or email" />
          <PasswordInput name="password" />
          
          <div className="flex justify-end">
            <a href="/forgot-password" className="text-sm text-white/50 hover:text-white hover:underline transition-colors">Forgot password?</a>
          </div>

          <button disabled={loading} className="w-full rounded-2xl bg-accent px-4 py-3 font-semibold text-white disabled:opacity-60">{loading ? "Signing in..." : "Login"}</button>
          
          {unverifiedEmail && (
            <div className="mt-4 p-4 rounded-xl border border-yellow-500/30 bg-yellow-500/10 text-center">
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

          <div className="mt-4 text-center text-sm text-white/70">
            Don't have an account?{" "}
            <a href="/register" className="text-accent hover:underline">
              Sign up
            </a>
          </div>
        </form>
      </SectionCard>
    </div>
  );
}