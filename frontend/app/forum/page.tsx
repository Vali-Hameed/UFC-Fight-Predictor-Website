import Link from "next/link";
import { SectionCard } from "@/components/section-card";
import { apiFetch, ForumThreadDto } from "@/lib/api";

export default async function ForumPage() {
  const threads = await apiFetch<ForumThreadDto[]>("/api/v1/forum/threads").catch(() => []);

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Forum" title="Event discussion" description="Each event has a forum and each fight has an auto-created thread.">
        <div className="space-y-3">
          {threads.map((thread) => (
            <Link key={thread.id} href={`/events/${thread.eventId ?? 1}`} className="block rounded-2xl border border-white/10 bg-white/5 p-4 transition hover:bg-white/10">
              <div className="text-xs uppercase tracking-[0.3em] text-gold">Thread</div>
              <div className="mt-2 font-semibold text-white">{thread.title}</div>
              <div className="mt-1 text-sm text-white/55">Fight thread #{thread.fightId ?? "N/A"}</div>
            </Link>
          ))}
          {threads.length === 0 ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">No threads available yet.</div> : null}
        </div>
      </SectionCard>
    </div>
  );
}