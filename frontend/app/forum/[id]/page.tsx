import { SectionCard } from "@/components/section-card";
import { ForumReplyForm } from "@/components/forum-reply-form";
import { apiFetch, ForumPostDto, ForumThreadDto, MlPredictionDto } from "@/lib/api";

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

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="space-y-6">
        <SectionCard
          eyebrow="Forum"
          title={thread?.title ?? `Thread #${threadId}`}
          description={thread ? `Discussion for ${thread.eventId ? `event #${thread.eventId}` : "the general forum"}${thread.fightId ? ` and fight #${thread.fightId}` : ""}.` : "This thread could not be loaded."}
        >
          {thread ? (
            <div className="grid gap-4 md:grid-cols-3">
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <p className="text-sm text-white/50">Thread</p>
                <p className="mt-2 text-2xl font-semibold text-white">#{thread.id}</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <p className="text-sm text-white/50">Event</p>
                <p className="mt-2 text-2xl font-semibold text-white">{thread.eventId ?? "General"}</p>
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <p className="text-sm text-white/50">Fight</p>
                <p className="mt-2 text-2xl font-semibold text-white">{thread.fightId ?? "N/A"}</p>
              </div>
              {mlPrediction ? (
                <div className="rounded-2xl border border-gold/20 bg-gold/10 p-4 md:col-span-3">
                  <p className="text-sm text-gold/70">ML Prediction</p>
                  <p className="mt-2 text-2xl font-semibold text-gold">
                    {mlPrediction.predictedWinner} ({Math.round((mlPrediction.confidenceScore ?? 0) * 100)}%)
                  </p>
                </div>
              ) : null}
            </div>
          ) : (
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Open another thread from the forum list.</div>
          )}
        </SectionCard>

        {thread ? <ForumReplyForm threadId={thread.id} /> : null}

        <SectionCard eyebrow="Posts" title="Conversation" description="Read the latest replies and jump into the discussion.">
          <div className="space-y-3">
            {posts.map((post) => (
              <article key={post.id} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <p className="text-xs uppercase tracking-[0.3em] text-gold">User #{post.userId ?? "N/A"}</p>
                  <p className="text-xs text-white/40">{post.createdAt ? new Date(post.createdAt).toLocaleString() : ""}</p>
                </div>
                <p className="mt-3 whitespace-pre-line text-sm leading-6 text-white/75">{post.content}</p>
                {post.isDeleted ? <p className="mt-3 text-xs uppercase tracking-[0.3em] text-white/35">Deleted</p> : null}
              </article>
            ))}
            {posts.length === 0 ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">No replies yet. Be the first to respond.</div> : null}
          </div>
        </SectionCard>
      </div>
    </div>
  );
}