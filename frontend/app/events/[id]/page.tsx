import Link from "next/link";
import { SectionCard } from "@/components/section-card";
import { PredictionCard } from "@/components/prediction-card";
import { apiFetch, ApiResponseError, CommunityVoteDto, EventDto, FightDto, ForumThreadDto, MlPredictionDto } from "@/lib/api";
import { notFound } from "next/navigation";

export const dynamic = "force-dynamic";

type EventPageProps = {
  params: Promise<{ id: string }>;
};

export default async function EventPage({ params }: EventPageProps) {
  const { id } = await params;

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

  const fightCards = [];
  for (const fight of fights) {
    const mlPrediction = await apiFetch<MlPredictionDto>(`/api/v1/ml/fight/${fight.id}`).catch(() => null);
    const communityVote = await apiFetch<CommunityVoteDto>(`/api/v1/community-votes/${fight.id}`).catch(() => null);
    fightCards.push({ fight, mlPrediction, communityVote });
  }

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

  const communityAccuracy = completedFightsCount > 0 ? Math.round((communityCorrect / completedFightsCount) * 100) : 0;
  const aiAccuracy = completedFightsCount > 0 ? Math.round((aiCorrect / completedFightsCount) * 100) : 0;

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <SectionCard eyebrow="Event detail" title={event.name} description={`${event.location ?? "Unknown location"} • ${event.status ?? "UNKNOWN"}`}>
          <div className="space-y-4">
            {fightCards.map(({ fight, mlPrediction, communityVote }) => (
              <PredictionCard key={fight.id} fight={fight} mlPrediction={mlPrediction} communityVote={communityVote} />
            ))}
          </div>
        </SectionCard>

        <div className="space-y-6">
          <SectionCard eyebrow="Community vs AI" title="Accuracy" description="Prediction accuracy for this event.">
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

          <SectionCard eyebrow="Forum" title="Event threads" description="ML winner and vote split are shown at the top of each fight thread.">
            <div className="space-y-3">
              {threads.map((thread) => (
                <Link key={thread.id} href={`/forum/${thread.id}`} className="block rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70 transition hover:bg-white/10">
                  <div className="text-xs uppercase tracking-[0.3em] text-gold">Thread</div>
                  <div className="mt-2 font-semibold text-white">{thread.title}</div>
                  <div className="mt-1 text-xs text-white/45">Open discussion</div>
                </Link>
              ))}
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