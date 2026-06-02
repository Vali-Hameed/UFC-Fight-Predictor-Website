"use client";

import { useRouter } from "next/navigation";
import { FormEvent, useState } from "react";
import { SectionCard } from "@/components/section-card";
import { useAuth } from "@/lib/session";
import { PasswordInput } from "@/components/password-input";

export default function LoginPage() {
  const { login } = useAuth();
  const router = useRouter();
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const username = String(formData.get("username") ?? "");
    const password = String(formData.get("password") ?? "");

    setLoading(true);
    setMessage(null);
    try {
      await login(username, password);
      router.push("/");
      router.refresh();
    } catch {
      setMessage("Login failed. Check your credentials and try again.");
    } finally {
      setLoading(false);
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

          {message ? <p className="text-sm text-red-300">{message}</p> : null}
          <button disabled={loading} className="w-full rounded-2xl bg-accent px-4 py-3 font-semibold text-white disabled:opacity-60">{loading ? "Signing in..." : "Login"}</button>
          
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