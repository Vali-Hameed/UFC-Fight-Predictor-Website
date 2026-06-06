"use client";

import { useEffect, useState, useRef } from "react";
import { useRouter, useSearchParams } from "next/navigation";
import { SectionCard } from "@/components/section-card";
import { apiFetch } from "@/lib/api";
import { toast } from "sonner";

export default function VerifyEmailPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [success, setSuccess] = useState(false);
  const [loading, setLoading] = useState(true);
  const [hasToken, setHasToken] = useState(false);
  const initialized = useRef(false);

  const [emailToResend, setEmailToResend] = useState("");
  const [resending, setResending] = useState(false);

  useEffect(() => {
    const queryToken = searchParams.get("token");
    if (!queryToken) {
      setLoading(false);
      return;
    }
    
    setHasToken(true);
    
    if (initialized.current) return;
    initialized.current = true;

    apiFetch<string>(`/api/v1/registration/confirm?token=${queryToken}`, {
      method: "GET"
    }).then(() => {
      setSuccess(true);
      toast.success("Email verified successfully! You can now log in.");
      setTimeout(() => {
        router.push("/login");
      }, 3000);
    }).catch(() => {
      toast.error("Verification failed. The link may be expired or invalid.");
    }).finally(() => {
      setLoading(false);
    });
  }, [router]);

  const handleResend = async () => {
    if (!emailToResend) {
      toast.error("Please enter your email address.");
      return;
    }
    setResending(true);
    try {
      const response = await fetch("http://localhost:8080/api/v1/registration/resend-verification", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email: emailToResend })
      });
      if (response.ok) {
        toast.success("Verification email resent. Check your inbox.");
        setEmailToResend("");
      } else {
        toast.error("Failed to resend email. Account may already be verified.");
      }
    } catch {
      toast.error("An error occurred while resending the email.");
    } finally {
      setResending(false);
    }
  };

  return (
    <div className="mx-auto max-w-xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard 
        eyebrow="Auth" 
        title={hasToken ? "Verifying email" : "Check your inbox"} 
        description={hasToken ? "Please wait while we verify your account." : "We've sent you a verification email. Please check your inbox and click the link to activate your account."}
      >
        <div className="py-8 text-center text-sm text-white/70">
          {loading ? (
            <div className="animate-pulse">Processing verification...</div>
          ) : success ? (
            <div className="text-green-400">Email verified! Redirecting to login...</div>
          ) : hasToken ? (
            <div className="space-y-4">
              <div className="text-red-300">Verification failed. The link may have expired.</div>
              <div className="mx-auto max-w-sm mt-4 p-4 rounded-xl border border-white/10 bg-black/40">
                <p className="mb-3 text-white">Resend verification email:</p>
                <input 
                  type="email" 
                  placeholder="Your email address" 
                  value={emailToResend}
                  onChange={(e) => setEmailToResend(e.target.value)}
                  className="w-full rounded-xl border border-white/10 bg-bg/70 px-4 py-2 text-white outline-none placeholder:text-white/35 mb-3"
                />
                <button 
                  onClick={handleResend}
                  disabled={resending || !emailToResend}
                  className="w-full rounded-xl bg-accent px-4 py-2 font-semibold text-white disabled:opacity-50"
                >
                  {resending ? "Resending..." : "Resend Link"}
                </button>
              </div>
            </div>
          ) : (
            <div>Waiting for you to click the link in your email... You can close this tab if you open the link in a new one.</div>
          )}
        </div>
      </SectionCard>
    </div>
  );
}