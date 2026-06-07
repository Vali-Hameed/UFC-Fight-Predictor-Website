import { SectionCard } from "@/components/section-card";
import { ForumReplyForm } from "@/components/forum-reply-form";
import { apiFetch, ForumPostDto, ForumThreadDto, MlPredictionDto, FightDto, CommunityVoteDto, EventDto } from "@/lib/api";
import { SubscribeButton } from "@/components/subscribe-button";
import { AdminModerationMenu } from "@/components/admin-moderation-menu";
import Link from "next/link";

type ForumThreadPageProps = {
  params: Promise<{ id: string }>;
};

export default async function ForumThreadPage({ params }: ForumThreadPageProps) {
  const { id } = await params;
  const threadId = Number(id);

  if (Number.isNaN(threadId)) {
    return (
      <div className="mx-auto max-w-5xl px-4 py-10 sm:px-6 lg:px-8">
        <SectionCard eyebrow="Forum" title="Thread not found" description="The requested forum thread id is invalid.">
          <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Use the forum list to open a valid thread.</div>
        </SectionCard>
      </div>
    );
  }

  const [thread, posts] = await Promise.all([
    apiFetch<ForumThreadDto>(`/api/v1/forum/threads/${threadId}`).catch(() => null),
    apiFetch<ForumPostDto[]>(`/api/v1/forum/posts?threadId=${threadId}`).catch(() => [])
  ]);

  let mlPrediction: MlPredictionDto | null = null;
  if (thread && thread.fightId) {
    mlPrediction = await apiFetch<MlPredictionDto>(`/api/v1/ml/fight/${thread.fightId}`).catch(() => null);
  }

  let event: EventDto | null = null;
  if (thread && thread.eventId) {
    event = await apiFetch<EventDto>(`/api/v1/events/${thread.eventId}`).catch(() => null);
  }
  const isArchived = event?.status === "ARCHIVED";

  const isEventThread = thread && thread.eventId && !thread.fightId;
  let fights: FightDto[] = [];
  let eventThreads: ForumThreadDto[] = [];
  const fightCardsData: { fight: FightDto; mlPrediction: MlPredictionDto | null; communityVote: CommunityVoteDto | null; threadId?: number }[] = [];

  if (isEventThread) {
    fights = await apiFetch<FightDto[]>(`/api/v1/events/${thread.eventId}/fights`).catch(() => []);
    eventThreads = await apiFetch<ForumThreadDto[]>(`/api/v1/forum/threads?eventId=${thread.eventId}`).catch(() => []);

    for (const fight of fights) {
      const ml = await apiFetch<MlPredictionDto>(`/api/v1/ml/fight/${fight.id}`).catch(() => null);
      const vote = await apiFetch<CommunityVoteDto>(`/api/v1/community-votes/${fight.id}`).catch(() => null);
      const fightThread = eventThreads.find(t => t.fightId === fight.id);
      fightCardsData.push({ fight, mlPrediction: ml, communityVote: vote, threadId: fightThread?.id });
    }
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="space-y-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-white">{thread?.title ?? `Thread #${threadId}`}</h1>
            <p className="mt-1 text-sm text-white/60">
              {thread ? `Discussion for ${thread.eventId ? `event #${thread.eventId}` : "the general forum"}${thread.fightId ? ` and fight #${thread.fightId}` : ""}.` : "This thread could not be loaded."}
            </p>
          </div>
          {thread && <SubscribeButton threadId={thread.id} />}
        </div>

        <SectionCard
          eyebrow="Forum"
          title="Details"
          description="Thread information."
        >
          {thread ? (
            mlPrediction ? (
              <div className="rounded-2xl border border-gold/20 bg-gold/10 p-4">
                <p className="text-sm text-gold/70">ML Prediction</p>
                <p className="mt-2 text-2xl font-semibold text-gold">
                  {mlPrediction.predictedWinner} ({Math.round((mlPrediction.confidenceScore ?? 0) * 100)}%)
                </p>
              </div>
            ) : null
          ) : (
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Open another thread from the forum list.</div>
          )}
        </SectionCard>

        <SectionCard eyebrow="Posts" title="Conversation" description="Read the latest replies and jump into the discussion.">
          <div className="space-y-3">
            {posts.map((post) => {
              // Highlight @mentions in post content
              const renderedContent = post.content ? post.content.split(/(@[a-zA-Z0-9_]+)/g).map((part, i) => {
                if (part.startsWith("@")) {
                  return <span key={i} className="text-accent font-semibold">{part}</span>;
                }
                return part;
              }) : "";

              return (
                <article key={post.id} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <p className="text-xs uppercase tracking-[0.3em] text-gold">{post.username ?? `User #${post.userId ?? "N/A"}`}</p>
                    <p className="text-xs text-white/40">{post.createdAt ? new Date(post.createdAt).toLocaleString() : ""}</p>
                  </div>
                  <p className="mt-3 whitespace-pre-wrap text-sm leading-6 text-white/75">{renderedContent}</p>
                  {post.isDeleted ? <p className="mt-3 text-xs uppercase tracking-[0.3em] text-white/35">Deleted</p> : null}
                  
                  {post.userId && !post.isDeleted && (
                    <AdminModerationMenu postId={post.id} postUserId={post.userId} isArchived={isArchived} />
                  )}
                </article>
              );
            })}
            {posts.length === 0 ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">No replies yet. Be the first to respond.</div> : null}
          </div>
        </SectionCard>

        {thread && !isArchived ? <ForumReplyForm threadId={thread.id} /> : null}
        {isArchived ? (
          <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
            This event is archived. New replies cannot be posted.
          </div>
        ) : null}

        {isEventThread && fights.length > 0 && (
          <SectionCard eyebrow="Event Fights" title="Fight Card" description="Discuss individual fights by visiting their specific threads.">
            <div className="space-y-6">
              {fightCardsData.map(({ fight, mlPrediction, threadId }) => (
                <div key={fight.id} className="relative rounded-3xl border border-white/10 bg-white/5 p-5">
                  <div className="flex flex-wrap items-start justify-between gap-4">
                    <div>
                      <p className="text-xs uppercase tracking-[0.3em] text-white/45">{fight.weightClass ?? "Fight"}</p>
                      <h3 className="mt-2 text-xl font-semibold text-white">
                        {fight.fighter1Name} vs {fight.fighter2Name}
                      </h3>
                      <p className="mt-1 text-sm text-white/55">Status: {fight.status ?? "UNKNOWN"}</p>
                    </div>
                    {mlPrediction ? (
                      <div className="rounded-2xl border border-gold/20 bg-gold/10 px-4 py-2 text-sm text-gold">
                        ML: {mlPrediction.predictedWinner} • {Math.round((mlPrediction.confidenceScore ?? 0) * 100)}%
                      </div>
                    ) : null}
                  </div>
                  {threadId ? (
                    <div className="mt-4 flex justify-end border-t border-white/10 pt-4">
                      <Link href={`/forum/${threadId}`} className="inline-flex items-center rounded-xl bg-white/10 px-4 py-2 text-sm font-semibold text-white transition hover:bg-white/20">
                        Discuss Fight →
                      </Link>
                    </div>
                  ) : null}
                </div>
              ))}
            </div>
          </SectionCard>
        )}
      </div>
    </div>
  );
}