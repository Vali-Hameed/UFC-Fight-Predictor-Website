import Link from "next/link";
import { SectionCard } from "@/components/section-card";
import { PredictionCard } from "@/components/prediction-card";
import { LiveFightsList } from "./live-fights-list";
import { apiFetch, ApiResponseError, CommunityVoteDto, EventDto, FightDto, ForumThreadDto, MlPredictionDto, getEventLeaderboard, getEventDisplayStatus, formatEventDate } from "@/lib/api";
import { notFound } from "next/navigation";
import type { Metadata } from "next";

export const dynamic = "force-dynamic";

export async function generateMetadata({ params }: { params: Promise<{ id: string }> }): Promise<Metadata> {
  const { id } = await params;
  try {
    const event = await apiFetch<EventDto>(`/api/v1/events/${id}`);
    if (event) {
      return {
        title: `${event.name} Predictions & Odds | FightPicks`,
        description: `Get AI and community predictions, stats, and odds for ${event.name} taking place in ${event.location ?? "TBD"}.`,
        openGraph: {
          title: `${event.name} Predictions | FightPicks`,
          description: `Get AI and community predictions, stats, and odds for ${event.name}.`,
        },
      };
    }
  } catch (err) {
    // Fallback if fetch fails
  }
  return {
    title: "Event Predictions | FightPicks",
  };
}

type EventPageProps = {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ [key: string]: string | string[] | undefined }>;
};

export default async function EventPage({ params, searchParams }: EventPageProps) {
  const { id } = await params;
  const isArchived = (await searchParams).archived === "true";

  // Fetch event — only call notFound() for genuine 404s.
  // For any other error (429, 500, network), show an error message instead
  // so events don't "disappear" when the backend is temporarily overloaded.
  let event: EventDto | null = null;
  let fetchError: string | null = null;

  try {
    event = await apiFetch<EventDto>(`/api/v1/events/${id}`);
  } catch (err) {
    if (err instanceof ApiResponseError && err.status === 404) {
      notFound();
    }
    fetchError = err instanceof Error ? err.message : "Failed to load event";
  }

  if (!event && !fetchError) {
    notFound();
  }

  if (fetchError || !event) {
    return (
      <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
        <SectionCard eyebrow="Error" title="Could not load event" description={fetchError ?? "The backend may be temporarily unavailable. Please refresh the page."}>
          <a
            href={`/events/${id}`}
            className="inline-block rounded-2xl bg-accent px-6 py-3 font-semibold text-white transition hover:bg-accent/80"
          >
            Retry
          </a>
        </SectionCard>
      </div>
    );
  }

  const fights = await apiFetch<FightDto[]>(`/api/v1/events/${id}/fights`).catch(() => [] as FightDto[]);
  const threads = await apiFetch<ForumThreadDto[]>(`/api/v1/forum/threads?eventId=${id}`).catch(() => []);
  const leaderboard = await getEventLeaderboard(id).catch(() => []);

  const fightCards: { fight: FightDto; mlPrediction: MlPredictionDto | null; communityVote: CommunityVoteDto | null }[] = [];
  for (const fight of fights) {
    const mlPrediction = await apiFetch<MlPredictionDto>(`/api/v1/ml/fight/${fight.id}`).catch(() => null);
    const communityVote = await apiFetch<CommunityVoteDto>(`/api/v1/community-votes/${fight.id}`).catch(() => null);
    fightCards.push({ fight, mlPrediction, communityVote });
  }

  fightCards.sort((a, b) => {
    if (a.fight.isMainEvent && !b.fight.isMainEvent) return -1;
    if (!a.fight.isMainEvent && b.fight.isMainEvent) return 1;
    return (a.fight.fightOrder ?? 999) - (b.fight.fightOrder ?? 999);
  });

  let aiCorrect = 0;
  let completedFightsCount = 0;

  fightCards.forEach(({ fight, mlPrediction }) => {
    if (fight.status === "COMPLETED" && fight.resultWinner) {
      completedFightsCount++;
      if (mlPrediction && mlPrediction.predictedWinner === fight.resultWinner) {
        aiCorrect++;
      }
    }
  });

  let totalEventPredictions = 0;
  let correctEventPredictions = 0;
  leaderboard.forEach(row => {
    totalEventPredictions += (row.totalPredictions ?? 0);
    correctEventPredictions += (row.correctPredictions ?? 0);
  });

  const communityAccuracy = totalEventPredictions > 0 ? Math.round((correctEventPredictions / totalEventPredictions) * 100) : 0;
  const aiAccuracy = completedFightsCount > 0 ? Math.round((aiCorrect / completedFightsCount) * 100) : 0;

  const isEventStarted = event.eventDate ? new Date(event.eventDate) < new Date() : false;
  const mainFightStatus = fightCards.find(f => f.fight.isMainEvent)?.fight.status || fightCards[0]?.fight.status;
  const displayStatus = getEventDisplayStatus(event, mainFightStatus);

  const jsonLd = {
    "@context": "https://schema.org",
    "@type": "SportsEvent",
    name: event.name,
    startDate: event.eventDate,
    location: {
      "@type": "Place",
      name: event.location || "TBD",
    },
    sport: "Mixed Martial Arts",
    competitor: fightCards
      .map((f) => [
        { "@type": "Person", name: f.fight.fighter1Name },
        { "@type": "Person", name: f.fight.fighter2Name },
      ])
      .flat(),
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: JSON.stringify(jsonLd) }}
      />
      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <SectionCard eyebrow="Event detail" title={event.name} description={`${event.location ?? "Unknown location"} • ${displayStatus}${event.eventDate ? ` • ${formatEventDate(event.eventDate)}` : ""}`}>
          <div className="mb-6 rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
            <strong>Pro Tip:</strong> Want to know how prediction scoring works? Check out the <Link href="/leaderboard" className="font-semibold text-gold hover:underline">Scoring Rules</Link> before you lock in your picks!
          </div>
          <div className="space-y-4">
            <LiveFightsList 
              eventId={id} 
              initialFights={fightCards} 
              isEventStarted={isEventStarted} 
              isArchived={isArchived} 
            />
          </div>
        </SectionCard>

        <div className="space-y-6">
          <SectionCard eyebrow="Community vs AI" title="Accuracy" description="Compare the community's overall prediction accuracy against our machine learning model for this event.">
            <div className="space-y-4">
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <div className="flex items-center justify-between text-sm text-white/70">
                  <span>Community Accuracy</span>
                  <span>{communityAccuracy}%</span>
                </div>
                <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/10">
                  <div className="h-full rounded-full bg-accent" style={{ width: `${communityAccuracy}%` }} />
                </div>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <div className="flex items-center justify-between text-sm text-white/70">
                  <span>AI</span>
                  <span className="text-gold">{aiAccuracy}%</span>
                </div>
                <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/10">
                  <div className="h-full rounded-full bg-gold" style={{ width: `${aiAccuracy}%` }} />
                </div>
              </div>
            </div>
          </SectionCard>
          
          <SectionCard eyebrow="Event Leaderboard" title="Top Predictors" description="See who is dominating the predictions for this specific event.">
            <div className="space-y-3 max-h-[400px] overflow-y-auto pr-2 custom-scrollbar">
              {leaderboard.map((row, index) => (
                <div key={row.userId} className="grid grid-cols-[auto_1fr_auto] items-center gap-4 rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
                  <div className="text-lg font-semibold text-gold">#{index + 1}</div>
                  <div>
                    <Link href={`/profile/${row.username ?? row.userId}`} className="font-semibold text-white hover:underline">@{row.username ?? `User #${row.userId}`}</Link>
                    <div className="text-sm text-white/50">{row.correctPredictions ?? 0} correct predictions • {Math.round(((row.correctPredictions ?? 0) / Math.max(row.totalPredictions ?? 1, 1)) * 100)}% win rate</div>
                  </div>
                  <div className="text-right">
                    <div className="font-semibold text-white">{row.totalPoints ?? 0} pts</div>
                  </div>
                </div>
              ))}
              {leaderboard.length === 0 ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Leaderboard has not been populated yet.</div> : null}
            </div>
          </SectionCard>

          <SectionCard eyebrow="Forum" title="Event threads" description="Discuss the fights with the community. ML predictions and vote splits are highlighted for each fight.">
            <div className="space-y-3">
              {threads.map((thread) => {
                const threadFight = fightCards.find(f => f.fight.id === thread.fightId);
                return (
                  <Link key={thread.id} href={`/forum/${thread.id}`} className="block rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70 transition hover:bg-white/10">
                    <div className="flex flex-wrap items-center justify-between gap-2">
                      <div className="text-xs uppercase tracking-[0.3em] text-gold">Thread</div>
                      {threadFight?.mlPrediction ? (
                        <div className="rounded-xl border border-gold/20 bg-gold/10 px-2 py-1 text-xs font-semibold text-gold">
                          ML: {threadFight.mlPrediction.predictedWinner} • {Math.round((threadFight.mlPrediction.confidenceScore ?? 0) * 100)}%
                        </div>
                      ) : null}
                    </div>
                    <div className="mt-2 font-semibold text-white">{thread.title}</div>
                    <div className="mt-1 text-xs text-white/45">Open discussion</div>
                  </Link>
                );
              })}
              {threads.length === 0 ? (
                <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
                  No forum threads available yet for this event.
                </div>
              ) : null}
            </div>
          </SectionCard>
        </div>
      </div>
    </div>
  );
}