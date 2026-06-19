export const dynamic = "force-dynamic";
import Link from "next/link";
import { SectionCard } from "@/components/section-card";
import { LocalTime } from "@/components/local-time";
import { CosmeticUsername } from "@/components/cosmetic-username";
import { apiFetch, EventDto, LeaderboardDto, FightDto, MlPredictionDto, CommunityVoteDto, getEventLeaderboard, getEventDisplayStatus, getGlobalAccuracy, formatEventDate } from "@/lib/api";

export default async function HomePage() {
  const events = await apiFetch<EventDto[]>("/api/v1/events").catch(() => []);
  const leaderboard = await apiFetch<LeaderboardDto[]>("/api/v1/leaderboard").catch(() => []);
  const eventsWithStatus = events.map(e => ({ ...e, displayStatus: getEventDisplayStatus(e) }));
  
  const upcomingEventsRaw = eventsWithStatus.filter((e) => e.displayStatus === "UPCOMING" || e.displayStatus === "LIVE");
  const completedEventsRaw = eventsWithStatus.filter((e) => e.displayStatus === "COMPLETED");

  // Sort upcoming events ascending (soonest first)
  upcomingEventsRaw.sort((a, b) => new Date(a.eventDate || 0).getTime() - new Date(b.eventDate || 0).getTime());

  const liveEvent = upcomingEventsRaw.find(e => e.displayStatus === "LIVE");
  const nextUpcomingEvent = upcomingEventsRaw.find(e => e.displayStatus === "UPCOMING");
  const featuredEvent = liveEvent ?? nextUpcomingEvent ?? eventsWithStatus[0] ?? null;

  const globalStats = await getGlobalAccuracy().catch(() => ({ aiAccuracy: 0, communityAccuracy: 0, totalAiFights: 0, totalCommunityPredictions: 0 }));
  let communityAccuracy = globalStats.communityAccuracy;
  let aiAccuracy = globalStats.aiAccuracy;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <section className="grid gap-6 rounded-[2rem] border border-white/10 bg-gradient-to-br from-panel via-bg to-panelSoft p-8 shadow-2xl shadow-black/30 lg:grid-cols-[1.3fr_0.7fr] lg:p-12">
        <div className="space-y-6">
          <div className="inline-flex rounded-full border border-accent/30 bg-accent/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.35em] text-accent">
            Live predictions • community vs AI • simulator • leaderboard
          </div>
          <div className="space-y-4">
            <h1 className="max-w-3xl text-4xl font-semibold leading-tight text-white sm:text-5xl lg:text-6xl">
              Outsmart the Octagon: Make your UFC predictions and see how you stack up.
            </h1>
            <p className="max-w-2xl text-base leading-7 text-white/68 sm:text-lg">
              Predict the winner, method, and round. Compete with thousands of MMA fans and test your knowledge against our advanced machine learning models.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Link href="/events" className="rounded-full bg-accent px-5 py-3 text-sm font-semibold text-white transition hover:opacity-90">
              View events
            </Link>
            <Link href="/simulator" className="rounded-full border border-accent/50 bg-accent/10 px-5 py-3 text-sm font-semibold text-accent transition hover:bg-accent/20">
              Try simulator
            </Link>
            <Link href="/leaderboard" className="rounded-full border border-white/10 bg-white/5 px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/10">
              Open leaderboard
            </Link>
          </div>
        </div>
        <div className="grid gap-4">
          {featuredEvent ? (
            <Link href={`/events/${featuredEvent.id}`} className="block rounded-3xl border border-white/10 bg-white/5 p-5 transition hover:bg-white/10">
              <p className="text-xs uppercase tracking-[0.3em] text-white/45">Featured event</p>
              <p className="mt-3 text-2xl font-semibold text-white">{featuredEvent.name}</p>
              <p className="mt-2 text-sm text-white/60">{featuredEvent.location}</p>
              {featuredEvent.eventDate && <p className="mt-1 text-sm text-white/60"><LocalTime dateStr={featuredEvent.eventDate} /></p>}
            </Link>
          ) : (
            <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
              <p className="text-xs uppercase tracking-[0.3em] text-white/45">Featured event</p>
              <p className="mt-3 text-2xl font-semibold text-white">No events loaded</p>
              <p className="mt-2 text-sm text-white/60">Backend data unavailable</p>
            </div>
          )}
          <div className="grid grid-cols-2 gap-4">
            <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
              <p className="text-sm text-white/50">Community Accuracy</p>
              <p className="mt-2 text-3xl font-semibold text-white">{communityAccuracy}%</p>
            </div>
            <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
              <p className="text-sm text-white/50">AI Accuracy</p>
              <p className="mt-2 text-3xl font-semibold text-gold">{aiAccuracy}%</p>
            </div>
          </div>
        </div>
      </section>

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <SectionCard
          eyebrow="Events"
          title="Upcoming cards"
          description="Stay up to date with the latest UFC events, access detailed fight cards, and submit your predictions."
        >
          <div className="space-y-4">
            {upcomingEventsRaw.map((event) => (
              <Link key={event.id} href={`/events/${event.id}`} className="block rounded-2xl border border-white/10 bg-white/5 p-4 transition hover:bg-white/10">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <p className="font-semibold text-white">{event.name}</p>
                    <p className="text-sm text-white/55">{event.location}</p>
                    {event.eventDate && <p className="mt-1 text-sm text-white/55"><LocalTime dateStr={event.eventDate} /></p>}
                  </div>
                  <span className="rounded-full border border-gold/30 bg-gold/10 px-3 py-1 text-xs font-semibold text-gold">
                    {event.displayStatus}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        </SectionCard>

        <SectionCard
          eyebrow="Leaderboard"
          title="Top ranked predictors"
          description="Climb the global ranks by making accurate predictions and building your winning streak."
        >
          <div className="space-y-3">
            {leaderboard.map((row, index) => (
              <div key={row.userId} className="flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm">
                <div>
                  <div className="font-semibold text-white flex items-center gap-2">
                    <span>#{index + 1}</span>
                    <CosmeticUsername 
                      username={row.username ?? `User #${row.userId}`}
                      cosmeticGlowColor={row.cosmeticGlowColor}
                      cosmeticTitle={row.cosmeticTitle}
                      size="sm"
                      showTitle={index < 3}
                    />
                  </div>
                  <div className="text-white/50">{row.correctPredictions ?? 0} correct • {Math.round(((row.correctPredictions ?? 0) / Math.max(row.totalPredictions ?? 1, 1)) * 100)}% win rate</div>
                </div>
                <div className="text-right text-white/75">
                  <div className="font-semibold text-white">{row.totalPoints ?? 0} pts</div>
                  <div className="text-white/50">{row.currentStreak ?? 0} streak</div>
                </div>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>
    </div>
  );
}