"use client";

import { FormEvent, useState } from "react";
import { apiFetch, FightDto, MlPredictionDto, CommunityVoteDto } from "@/lib/api";
import { useAuth } from "@/lib/session";

type PredictionCardProps = {
  fight: FightDto;
  mlPrediction: MlPredictionDto | null;
  communityVote: CommunityVoteDto | null;
};

const methodOptions = ["KO/TKO", "Submission", "Decision"];

export function PredictionCard({ fight, mlPrediction, communityVote }: PredictionCardProps) {
  const { token } = useAuth();
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const locked = fight.status === "LIVE" || fight.status === "COMPLETED";

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!token) {
      setMessage("Sign in to submit a prediction.");
      return;
    }

    const formData = new FormData(event.currentTarget);
    const payload = {
      fightId: fight.id,
      predictedWinner: String(formData.get("predictedWinner") ?? ""),
      predictedMethod: String(formData.get("predictedMethod") ?? ""),
      predictedRound: Number(formData.get("predictedRound") ?? 1)
    };

    setLoading(true);
    setMessage(null);
    try {
      await apiFetch("/api/v1/predictions", {
        method: "POST",
        body: JSON.stringify(payload)
      }, token);
      setMessage("Prediction submitted successfully.");
    } catch {
      setMessage("Could not submit prediction.");
    } finally {
      setLoading(false);
    }
  };

  const communityPercent = communityVote
    ? Math.round(((communityVote.fighter1Votes ?? 0) / Math.max((communityVote.fighter1Votes ?? 0) + (communityVote.fighter2Votes ?? 0), 1)) * 100)
    : 0;

  const maxRounds = fight.isMainEvent || fight.weightClass?.toLowerCase().includes("title") ? 5 : 3;

  return (
    <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
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

      <div className="mt-4 grid gap-3 md:grid-cols-2">
        <div className="rounded-2xl border border-white/10 bg-bg/70 p-4 text-sm text-white/70">
          Community split: {communityPercent}% {fight.fighter1Name}
          <div className="mt-3 h-2 overflow-hidden rounded-full bg-white/10">
            <div className="h-full bg-accent" style={{ width: `${communityPercent}%` }} />
          </div>
        </div>
        <div className="rounded-2xl border border-white/10 bg-bg/70 p-4 text-sm text-white/70">
          {fight.status === "COMPLETED"
            ? `Result: ${fight.resultWinner ?? "TBD"}`
            : "Predictions can be submitted until the event is live."}
        </div>
      </div>

      <form className="mt-5 grid gap-3 md:grid-cols-3" onSubmit={handleSubmit}>
        <select name="predictedWinner" disabled={locked} defaultValue={fight.fighter1Name ?? ""} className="rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white disabled:opacity-60">
          <option value={fight.fighter1Name ?? ""}>{fight.fighter1Name}</option>
          <option value={fight.fighter2Name ?? ""}>{fight.fighter2Name}</option>
        </select>
        <select name="predictedMethod" disabled={locked} defaultValue={methodOptions[0]} className="rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white disabled:opacity-60">
          {methodOptions.map((method) => (
            <option key={method} value={method}>
              {method}
            </option>
          ))}
        </select>
        <input name="predictedRound" disabled={locked} type="number" min={1} max={maxRounds} defaultValue={1} className="rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white disabled:opacity-60" />
        <button disabled={locked || loading} className="rounded-2xl bg-accent px-4 py-3 font-semibold text-white disabled:opacity-50 md:col-span-3">
          {locked ? "Locked" : loading ? "Submitting..." : "Submit prediction"}
        </button>
      </form>
      {message ? <p className="mt-3 text-sm text-white/70">{message}</p> : null}
    </div>
  );
}