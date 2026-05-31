import Link from "next/link";
import { SectionCard } from "@/components/section-card";
import { apiFetch, EventDto } from "@/lib/api";

export default async function EventsPage() {
  const events = await apiFetch<EventDto[]>("/api/v1/events").catch((e) => {
    console.error("Events fetch failed:", e);
    return [];
  });

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard
        eyebrow="Schedule"
        title="Event listing"
        description="Each event page includes fights, community votes, ML predictions, and forum threads."
      >
        <div className="grid gap-4 lg:grid-cols-2">
          {events.map((event) => (
            <Link key={event.id} href={`/events/${event.id}`} className="rounded-3xl border border-white/10 bg-white/5 p-5 transition hover:-translate-y-0.5 hover:bg-white/8">
              <p className="text-xs uppercase tracking-[0.3em] text-gold">{event.status ?? "UPCOMING"}</p>
              <h3 className="mt-3 text-xl font-semibold text-white">{event.name}</h3>
              <p className="mt-2 text-sm text-white/58">{event.location}</p>
            </Link>
          ))}
          {events.length === 0 ? <div className="rounded-3xl border border-white/10 bg-white/5 p-5 text-sm text-white/70">No events available yet.</div> : null}
        </div>
      </SectionCard>
    </div>
  );
}