import { SectionCard } from "@/components/section-card";

export default function VerifyEmailPage() {
  return (
    <div className="mx-auto max-w-xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Auth" title="Verify email" description="Paste the token from the verification email to activate your account.">
        <input className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white outline-none placeholder:text-white/35" placeholder="Verification token" />
      </SectionCard>
    </div>
  );
}