import { SectionCard } from "@/components/section-card";
import { PredictionCard } from "@/components/prediction-card";
import { apiFetch, CommunityVoteDto, EventDto, FightDto, ForumThreadDto, MlPredictionDto } from "@/lib/api";
import { notFound } from "next/navigation";

type EventPageProps = {
  params: Promise<{ id: string }>;
};

export default async function EventPage({ params }: EventPageProps) {
  const { id } = await params;
  const event = await apiFetch<EventDto>(`/api/v1/events/${id}`).catch(() => null);
  if (!event) {
    notFound();
  }
  const fights = await apiFetch<FightDto[]>(`/api/v1/events/${id}/fights`);
  const threads = await apiFetch<ForumThreadDto[]>(`/api/v1/forum/threads?eventId=${id}`).catch(() => []);

  const fightCards = await Promise.all(
    fights.map(async (fight) => ({
      fight,
      mlPrediction: await apiFetch<MlPredictionDto>(`/api/v1/ml/fight/${fight.id}`).catch(() => null),
      communityVote: await apiFetch<CommunityVoteDto>(`/api/v1/community-votes/${fight.id}`).catch(() => null)
    }))
  );

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
          <SectionCard eyebrow="Community vs AI" title="Scoreboard" description="Displayed on the event page and homepage.">
            <div className="space-y-4">
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <div className="flex items-center justify-between text-sm text-white/70">
                  <span>Community</span>
                  <span>58%</span>
                </div>
                <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/10">
                  <div className="h-full w-[58%] rounded-full bg-accent" />
                </div>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
                AI currently leads by 3 fights across completed events.
              </div>
            </div>
          </SectionCard>

          <SectionCard eyebrow="Forum" title="Event threads" description="ML winner and vote split are shown at the top of each fight thread.">
            <div className="space-y-3">
              {threads.map((thread) => (
                <div key={thread.id} className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
                  <div className="text-xs uppercase tracking-[0.3em] text-gold">Thread</div>
                  <div className="mt-2 font-semibold text-white">{thread.title}</div>
                </div>
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