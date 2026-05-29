import type { ReactNode } from "react";

type SectionCardProps = {
  eyebrow?: string;
  title: string;
  description?: string;
  children: ReactNode;
};

export function SectionCard({ eyebrow, title, description, children }: SectionCardProps) {
  return (
    <section className="rounded-3xl border border-white/10 bg-panel/90 p-6 shadow-2xl shadow-black/20 backdrop-blur">
      {eyebrow ? <p className="mb-3 text-xs font-semibold uppercase tracking-[0.3em] text-gold">{eyebrow}</p> : null}
      <div className="mb-5 space-y-2">
        <h2 className="text-2xl font-semibold text-white">{title}</h2>
        {description ? <p className="max-w-2xl text-sm leading-6 text-white/65">{description}</p> : null}
      </div>
      {children}
    </section>
  );
}