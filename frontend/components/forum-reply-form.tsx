"use client";

import { FormEvent, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";
import { useAuth } from "@/lib/session";

type ForumReplyFormProps = {
  threadId: number;
};

export function ForumReplyForm({ threadId }: ForumReplyFormProps) {
  const router = useRouter();
  const { token } = useAuth();
  const [content, setContent] = useState("");
  const [message, setMessage] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  const submitReply = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token || !content.trim()) {
      return;
    }

    setSaving(true);
    setMessage(null);

    try {
      await apiFetch(
        "/api/v1/forum/posts",
        {
          method: "POST",
          body: JSON.stringify({
            threadId,
            content: content.trim()
          })
        },
        token
      );
      setContent("");
      setMessage("Reply posted.");
      router.refresh();
    } catch {
      setMessage("Could not post reply.");
    } finally {
      setSaving(false);
    }
  };

  if (!token) {
    return <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Sign in to reply to this thread.</div>;
  }

  return (
    <form onSubmit={submitReply} className="rounded-3xl border border-white/10 bg-white/5 p-5">
      <div className="mb-4 flex items-start justify-between gap-3">
        <div>
          <p className="text-xs uppercase tracking-[0.3em] text-gold">Reply</p>
          <h3 className="mt-2 text-lg font-semibold text-white">Join the discussion</h3>
        </div>
        {message ? <div className="rounded-full border border-white/10 bg-bg/70 px-4 py-2 text-xs text-white/70">{message}</div> : null}
      </div>

      <textarea
        value={content}
        onChange={(event) => setContent(event.target.value)}
        rows={5}
        className="w-full rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-sm text-white outline-none placeholder:text-white/35"
        placeholder="Write a reply..."
      />

      <div className="mt-4 flex items-center gap-3">
        <button
          type="submit"
          disabled={saving || !content.trim()}
          className="rounded-2xl bg-accent px-5 py-3 text-sm font-semibold text-white transition hover:brightness-110 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {saving ? "Posting..." : "Post reply"}
        </button>
        <p className="text-xs text-white/45">Your reply will appear after the thread refreshes.</p>
      </div>
    </form>
  );
}