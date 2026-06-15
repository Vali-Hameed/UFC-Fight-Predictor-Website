export const dynamic = "force-dynamic";
import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Event Schedule & Results | FightPicks",
  description: "Explore upcoming and past fights. Dive into detailed fight cards to submit your predictions and analyze community trends.",
  openGraph: {
    title: "Event Schedule | FightPicks",
    description: "Explore upcoming and past fights and fight cards.",
  },
};
import { SectionCard } from "@/components/section-card";
import { apiFetch, EventDto, FightDto, MlPredictionDto, getEventDisplayStatus, getArchivedEvents } from "@/lib/api";
import { ArchivedEventsDropdown } from "./archived-events-dropdown";

export default async function EventsPage() {
  const events = await apiFetch<EventDto[]>("/api/v1/events").catch((e) => {
    console.error("Events fetch failed:", e);
    return [];
  });

  // Pre-classify events to determine schedule vs archive
  const classifiedEvents = events.map(e => ({
    ...e,
    displayStatus: getEventDisplayStatus(e)
  }));

  const upcomingEventsRaw = classifiedEvents.filter((e) => e.displayStatus === "UPCOMING" || e.displayStatus === "LIVE");
  const completedEventsRaw = classifiedEvents.filter((e) => e.displayStatus === "COMPLETED");

  // Sort upcoming events ascending (soonest first)
  upcomingEventsRaw.sort((a, b) => new Date(a.eventDate || 0).getTime() - new Date(b.eventDate || 0).getTime());

  // Sort completed events descending (most recent first)
  completedEventsRaw.sort((a, b) => new Date(b.eventDate || 0).getTime() - new Date(a.eventDate || 0).getTime());

  const scheduleEventsRaw = [...completedEventsRaw.slice(0, 1), ...upcomingEventsRaw];

  const excludeIds = scheduleEventsRaw.map(e => e.id);

  const firstArchivedPage = await getArchivedEvents(0, 20, excludeIds).catch(() => ({
    content: [] as EventDto[],
    last: true,
    totalPages: 0,
    totalElements: 0,
    pageable: { pageNumber: 0, pageSize: 20 }
  }));

  // Fetch predictions ONLY for schedule events
  const scheduleEvents = await Promise.all(
    scheduleEventsRaw.map(async (event) => {
      let mainPrediction: MlPredictionDto | null = null;
      let mainFightStatus: string | null = null;
      try {
        const fights = await apiFetch<FightDto[]>(`/api/v1/events/${event.id}/fights`);
        const mainFight = fights.find((f) => f.isMainEvent) || fights[0];
        if (mainFight) {
          mainFightStatus = mainFight.status;
          mainPrediction = await apiFetch<MlPredictionDto>(`/api/v1/ml/fight/${mainFight.id}`).catch(() => null);
        }
      } catch {
        // Ignore
      }
      const displayStatus = getEventDisplayStatus(event, mainFightStatus);
      return { ...event, mainPrediction, displayStatus };
    })
  );

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8 space-y-8">
      <SectionCard
        eyebrow="Schedule"
        title="Event listing"
        description="Explore upcoming and past UFC events. Dive into detailed fight cards to submit your predictions and analyze community trends."
      >
        <div className="grid gap-4 lg:grid-cols-2">
          {scheduleEvents.map((event) => (
            <Link key={event.id} href={`/events/${event.id}`} className="rounded-3xl border border-white/10 bg-white/5 p-5 transition hover:-translate-y-0.5 hover:bg-white/8">
              <p className="text-xs uppercase tracking-[0.3em] text-gold">{event.displayStatus}</p>
              <h3 className="mt-3 text-xl font-semibold text-white">{event.name}</h3>
              <p className="mt-2 text-sm text-white/58">{event.location}</p>
              {event.mainPrediction ? (
                <div className="mt-4 inline-block rounded-xl border border-gold/20 bg-gold/10 px-3 py-1.5 text-xs font-semibold text-gold">
                  Main Event ML: {event.mainPrediction.predictedWinner} • {Math.round((event.mainPrediction.confidenceScore ?? 0) * 100)}%
                </div>
              ) : null}
            </Link>
          ))}
          {scheduleEvents.length === 0 ? <div className="rounded-3xl border border-white/10 bg-white/5 p-5 text-sm text-white/70">No events available yet.</div> : null}
        </div>
      </SectionCard>

      <ArchivedEventsDropdown initialPage={firstArchivedPage} excludeIds={excludeIds} />
    </div>
  );
}