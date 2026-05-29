"use client";

import { FormEvent, useEffect, useState } from "react";
import { SectionCard } from "@/components/section-card";
import { apiFetch, ScrapeLogDto } from "@/lib/api";
import { useAuth } from "@/lib/session";

export default function AdminPage() {
  const { token } = useAuth();
  const [logs, setLogs] = useState<ScrapeLogDto[]>([]);
  const [message, setMessage] = useState<string | null>(null);

  useEffect(() => {
    if (!token) return;
    void apiFetch<ScrapeLogDto[]>("/api/v1/internal/scraper/logs", {}, token)
      .then(setLogs)
      .catch(() => setMessage("Could not load scraper logs."));
  }, [token]);

  const triggerPrewarm = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token) return;
    try {
      await apiFetch("/api/v1/admin/prewarm/trigger", { method: "POST" }, token);
      setMessage("Prewarm triggered.");
    } catch {
      setMessage("Could not trigger prewarm.");
    }
  };

  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Admin" title="Operations panel" description="Manage events, fights, scrape runs, predictions, and moderation.">
        {!token ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Sign in as an admin to use this panel.</div> : null}
        {message ? <div className="mb-4 rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">{message}</div> : null}
        <form onSubmit={triggerPrewarm} className="mb-6">
          <button className="rounded-2xl bg-accent px-4 py-3 text-sm font-semibold text-white">Trigger ML prewarm</button>
        </form>
        <div className="space-y-3">
          {logs.map((log) => (
            <div key={log.id} className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/75">
              <div className="font-semibold text-white">{log.status}</div>
              <div className="mt-1 text-white/55">Events: {log.eventsFound ?? 0} • Fights: {log.fightsUpdated ?? 0}</div>
            </div>
          ))}
          {logs.length === 0 ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">No scraper logs loaded yet.</div> : null}
        </div>
      </SectionCard>
    </div>
  );
}