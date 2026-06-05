import Link from "next/link";
import { SectionCard } from "@/components/section-card";
import { apiFetch, EventDto, LeaderboardDto, FightDto, MlPredictionDto, CommunityVoteDto } from "@/lib/api";

export default async function HomePage() {
  const events = await apiFetch<EventDto[]>("/api/v1/events").catch(() => []);
  const leaderboard = await apiFetch<LeaderboardDto[]>("/api/v1/leaderboard").catch(() => []);
  const featuredEvent = events.find(e => e.status === "UPCOMING") ?? events[0] ?? null;

  let communityAccuracy = 0;
  let aiAccuracy = 0;

  const lastCompletedEvent = events.find(e => e.status === "COMPLETED");
  
  if (lastCompletedEvent) {
    const fights = await apiFetch<FightDto[]>(`/api/v1/events/${lastCompletedEvent.id}/fights`).catch(() => []);
    const fightCards = await Promise.all(
      fights.map(async (fight) => ({
        fight,
        mlPrediction: await apiFetch<MlPredictionDto>(`/api/v1/ml/fight/${fight.id}`).catch(() => null),
        communityVote: await apiFetch<CommunityVoteDto>(`/api/v1/community-votes/${fight.id}`).catch(() => null)
      }))
    );
    let communityCorrect = 0;
    let aiCorrect = 0;
    let completedFightsCount = 0;

    fightCards.forEach(({ fight, mlPrediction, communityVote }) => {
      if (fight.status === "COMPLETED" && fight.resultWinner) {
        completedFightsCount++;
        if (mlPrediction && mlPrediction.predictedWinner === fight.resultWinner) {
          aiCorrect++;
        }
        if (communityVote) {
          const f1Votes = communityVote.fighter1Votes ?? 0;
          const f2Votes = communityVote.fighter2Votes ?? 0;
          const communityWinner = f1Votes > f2Votes ? fight.fighter1Name : (f2Votes > f1Votes ? fight.fighter2Name : null);
          if (communityWinner === fight.resultWinner) {
            communityCorrect++;
          }
        }
      }
    });

    communityAccuracy = completedFightsCount > 0 ? Math.round((communityCorrect / completedFightsCount) * 100) : 0;
    aiAccuracy = completedFightsCount > 0 ? Math.round((aiCorrect / completedFightsCount) * 100) : 0;
  }

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <section className="grid gap-6 rounded-[2rem] border border-white/10 bg-gradient-to-br from-panel via-bg to-panelSoft p-8 shadow-2xl shadow-black/30 lg:grid-cols-[1.3fr_0.7fr] lg:p-12">
        <div className="space-y-6">
          <div className="inline-flex rounded-full border border-accent/30 bg-accent/10 px-4 py-2 text-xs font-semibold uppercase tracking-[0.35em] text-accent">
            Live predictions • community vs AI • leaderboard
          </div>
          <div className="space-y-4">
            <h1 className="max-w-3xl text-4xl font-semibold leading-tight text-white sm:text-5xl lg:text-6xl">
              Predict every UFC fight with a clean, dark, premium interface.
            </h1>
            <p className="max-w-2xl text-base leading-7 text-white/68 sm:text-lg">
              Lock in winner, method, and round picks, compare them against the ML model, and track your rank across events.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Link href="/events" className="rounded-full bg-accent px-5 py-3 text-sm font-semibold text-white transition hover:opacity-90">
              View events
            </Link>
            <Link href="/leaderboard" className="rounded-full border border-white/10 bg-white/5 px-5 py-3 text-sm font-semibold text-white transition hover:bg-white/10">
              Open leaderboard
            </Link>
          </div>
        </div>
        <div className="grid gap-4">
          <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
            <p className="text-xs uppercase tracking-[0.3em] text-white/45">Featured event</p>
            <p className="mt-3 text-2xl font-semibold text-white">{featuredEvent?.name ?? "No events loaded"}</p>
            <p className="mt-2 text-sm text-white/60">{featuredEvent?.location ?? "Backend data unavailable"}</p>
          </div>
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
          description="Browse event pages, fights, and prediction cards in one place."
        >
          <div className="space-y-4">
            {events.filter(e => e.status === "UPCOMING").map((event) => (
              <Link key={event.id} href={`/events/${event.id}`} className="block rounded-2xl border border-white/10 bg-white/5 p-4 transition hover:bg-white/10">
                <div className="flex items-center justify-between gap-4">
                  <div>
                    <p className="font-semibold text-white">{event.name}</p>
                    <p className="text-sm text-white/55">{event.location}</p>
                  </div>
                  <span className="rounded-full border border-gold/30 bg-gold/10 px-3 py-1 text-xs font-semibold text-gold">
                    {event.status}
                  </span>
                </div>
              </Link>
            ))}
          </div>
        </SectionCard>

        <SectionCard
          eyebrow="Leaderboard"
          title="Top ranked predictors"
          description="Ranks update from prediction result processing and streak tracking."
        >
          <div className="space-y-3">
            {leaderboard.map((row, index) => (
              <div key={row.userId} className="flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 px-4 py-3 text-sm">
                <div>
                  <div className="font-semibold text-white">#{index + 1} {row.userId}</div>
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