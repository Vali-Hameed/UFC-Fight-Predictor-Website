"use client";

import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import { apiFetch, FightDto, MlPredictionDto, CommunityVoteDto } from "@/lib/api";
import { useAuth } from "@/lib/session";
import { useRouter } from "next/navigation";
import { toast } from "sonner";

type PredictionCardProps = {
  fight: FightDto;
  mlPrediction: MlPredictionDto | null;
  communityVote: CommunityVoteDto | null;
  isEventStarted?: boolean;
  isArchived?: boolean;
};

const methodOptions = ["Any Method", "KO/TKO", "Submission", "Decision"];

/** Minimum ms between actual network submissions */
const SUBMIT_COOLDOWN_MS = 2000;

export function PredictionCard({ fight, mlPrediction, communityVote, isEventStarted = false, isArchived = false }: PredictionCardProps) {
  const { token, user, refreshUser } = useAuth();
  const router = useRouter();
  const [loading, setLoading] = useState(false);
  const [displayClock, setDisplayClock] = useState(fight.currentClock);
  const [isDirty, setIsDirty] = useState(false);

  const userPrediction = user?.predictionHistory?.find(p => p.fightId === fight.id);

  const handleFormChange = (event: FormEvent<HTMLFormElement>) => {
    if (!userPrediction) {
      setIsDirty(true);
      return;
    }
    
    const formData = new FormData(event.currentTarget);
    const winner = String(formData.get("predictedWinner") ?? "");
    const method = String(formData.get("predictedMethod") ?? "");
    const round = Number(formData.get("predictedRound") ?? 0);
    const optOut = formData.get("optOutResultNotification") === "on";

    const winnerChanged = winner !== (userPrediction.predictedWinner ?? (fight.fighter1Name ?? ""));
    const methodChanged = method !== (userPrediction.predictedMethod ?? methodOptions[0]);
    const roundChanged = round !== (userPrediction.predictedRound ?? 0);
    const optOutChanged = optOut !== false;

    setIsDirty(winnerChanged || methodChanged || roundChanged || optOutChanged);
  };

  // Sync state when props change (scraper updates)
  useEffect(() => {
    setDisplayClock(fight.currentClock);
  }, [fight.currentClock]);

  // Visually tick down every 1 second if in progress
  useEffect(() => {
    if (!fight.liveStatus?.includes("IN_PROGRESS")) return;

    const interval = setInterval(() => {
      setDisplayClock(prev => {
        if (!prev || !prev.includes(":")) return prev;
        const [mStr, sStr] = prev.split(":");
        let mins = parseInt(mStr, 10);
        let secs = parseInt(sStr, 10);

        if (isNaN(mins) || isNaN(secs)) return prev;

        if (secs === 0) {
          if (mins === 0) return prev;
          mins -= 1;
          secs = 59;
        } else {
          secs -= 1;
        }

        return `${mins}:${secs.toString().padStart(2, "0")}`;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [fight.liveStatus]);

  // Tracks whether a request is currently in-flight to prevent overlap
  const inflightRef = useRef(false);
  // Timestamp of last successful submission for cooldown
  const lastSubmitRef = useRef(0);

  const locked = fight.status === "LIVE" || fight.status === "COMPLETED" || fight.status === "CANCELED" || isEventStarted;

  const handleSubmit = useCallback(async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!token) {
      toast.error("Sign in to submit a prediction.");
      return;
    }

    // Block if already in-flight
    if (inflightRef.current) {
      return;
    }

    // Enforce cooldown between submissions
    const timeSinceLast = Date.now() - lastSubmitRef.current;
    if (timeSinceLast < SUBMIT_COOLDOWN_MS) {
      toast.error("Slow down! Please wait a moment before submitting again.");
      return;
    }

    const formData = new FormData(event.currentTarget);
    const payload = {
      fightId: fight.id,
      predictedWinner: String(formData.get("predictedWinner") ?? ""),
      predictedMethod: String(formData.get("predictedMethod") ?? ""),
      predictedRound: Number(formData.get("predictedRound") ?? 0),
      optOutResultNotification: formData.get("optOutResultNotification") === "on"
    };

    inflightRef.current = true;
    setLoading(true);

    try {
      await apiFetch("/api/v1/predictions", {
        method: "POST",
        body: JSON.stringify(payload),
      }, token);
      lastSubmitRef.current = Date.now();
      toast.success("Prediction submitted successfully.");
      setIsDirty(false);
      await refreshUser();
      router.refresh();
    } catch (error) {
      console.error("Prediction submission failed:", error);
      if (error instanceof Error && error.message.includes("Too Many Requests")) {
        toast.error("Slow down! You're submitting too fast.");
      } else {
        toast.error("Could not submit prediction: " + (error instanceof Error ? error.message : "Unknown error"));
      }
    } finally {
      inflightRef.current = false;
      setLoading(false);
    }
  }, [token, fight.id, router]);

  const totalVotes = Math.max((communityVote?.fighter1Votes ?? 0) + (communityVote?.fighter2Votes ?? 0), 1);
  const percent1 = communityVote ? Math.round(((communityVote.fighter1Votes ?? 0) / totalVotes) * 100) : 0;
  const percent2 = communityVote ? Math.round(((communityVote.fighter2Votes ?? 0) / totalVotes) * 100) : 0;

  const maxRounds = fight.isMainEvent || fight.weightClass?.toLowerCase().includes("title") ? 5 : 3;

  return (
    <div className="rounded-3xl border border-white/10 bg-white/5 p-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-3">
            <p className="text-xs uppercase tracking-[0.3em] text-white/45">{fight.weightClass ?? "Fight"}</p>
            {fight.liveStatus && !fight.liveStatus.includes("SCHEDULED") && !fight.liveStatus.includes("FINAL") && !fight.liveStatus.includes("CANCELED") && fight.status !== "COMPLETED" && (
              <span className="flex items-center gap-1.5 rounded-full border border-red-500/30 bg-red-500/10 px-2 py-0.5 text-[10px] font-bold uppercase tracking-widest text-red-500 animate-pulse">
                <span className="h-1.5 w-1.5 rounded-full bg-red-500" />
                LIVE {fight.currentRound ? `- R${fight.currentRound}` : ''} {displayClock ? `(${displayClock})` : ''}
              </span>
            )}
          </div>
          <h3 className={`mt-2 text-xl font-semibold ${fight.status === "CANCELED" ? "text-white/50 line-through" : "text-white"}`}>
            {fight.fighter1Name} vs {fight.fighter2Name}
            {fight.status === "CANCELED" && <span className="ml-2 inline-block rounded-full bg-red-500/20 px-2 py-0.5 text-xs font-bold uppercase tracking-widest text-red-400 no-underline align-middle">Canceled</span>}
          </h3>
          <p className="mt-1 text-sm text-white/55">
            Status: {fight.liveStatus && fight.status !== "COMPLETED" ? fight.liveStatus.replace("STATUS_", "").replace(/_/g, " ").replace("SCHEDULED", "UPCOMING").replace(/ \d+$/, "") : (fight.status ?? "UNKNOWN")}
          </p>
        </div>
        {mlPrediction ? (
          <div className="rounded-2xl border border-gold/20 bg-gold/10 px-4 py-2 text-sm text-gold">
            ML: {mlPrediction.predictedWinner} • {Math.round((mlPrediction.confidenceScore ?? 0) * 100)}%
          </div>
        ) : null}
      </div>

      <div className="mt-4 grid gap-3 md:grid-cols-2">
        <div className="rounded-2xl border border-white/10 bg-bg/70 p-4 text-sm text-white/70">
          Community split: {percent1}% {fight.fighter1Name} vs {percent2}% {fight.fighter2Name}
          <div className="mt-3 flex h-2 w-full overflow-hidden rounded-full bg-white/10">
            <div className="h-full bg-accent transition-all duration-500" style={{ width: `${percent1}%` }} />
            <div className="h-full bg-blue-600 transition-all duration-500" style={{ width: `${percent2}%` }} />
          </div>
        </div>
        <div className="rounded-2xl border border-white/10 bg-bg/70 p-4 text-sm text-white/70">
          {fight.status === "COMPLETED"
            ? (["Canceled", "Draw", "No Contest", "Canceled/No Contest"].includes(fight.resultWinner || "") 
                ? `Result: ${fight.resultWinner}` 
                : `Result: ${fight.resultWinner ?? "TBD"}${fight.resultMethod ? ` by ${fight.resultMethod}` : ''}${fight.resultRound ? ` (Round ${fight.resultRound})` : ''}`)
            : fight.status === "CANCELED"
            ? "Result: Canceled/Draw"
            : "Predictions can be submitted until the event is live."}
        </div>
      </div>

      {!isArchived && (
        <form key={JSON.stringify(userPrediction)} className="mt-5 grid gap-3 md:grid-cols-3" onSubmit={handleSubmit} onChange={handleFormChange}>
          <select name="predictedWinner" disabled={locked} defaultValue={userPrediction?.predictedWinner ?? (fight.fighter1Name ?? "")} className="rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white disabled:opacity-60">
            <option value={fight.fighter1Name ?? ""}>{fight.fighter1Name}</option>
            <option value={fight.fighter2Name ?? ""}>{fight.fighter2Name}</option>
            <option value="Draw">Draw</option>
            <option value="Canceled/No Contest">Canceled / No Contest</option>
          </select>
          <select name="predictedMethod" disabled={locked} defaultValue={userPrediction?.predictedMethod ?? methodOptions[0]} className="rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white disabled:opacity-60">
            {methodOptions.map((method) => (
              <option key={method} value={method}>
                {method}
              </option>
            ))}
          </select>
          <select name="predictedRound" disabled={locked} defaultValue={userPrediction?.predictedRound ?? 0} className="rounded-2xl border border-white/10 bg-bg/70 px-4 py-3 text-white disabled:opacity-60">
            <option value={0}>Any Round</option>
            {Array.from({ length: maxRounds }, (_, i) => (
              <option key={i + 1} value={i + 1}>
                Round {i + 1}
              </option>
            ))}
          </select>
          <div className="md:col-span-3 flex items-center gap-2 mt-2">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                name="optOutResultNotification"
                disabled={locked}
                className="h-4 w-4 rounded border-white/10 bg-bg/70 accent-accent cursor-pointer disabled:opacity-60"
              />
              <span className="text-sm text-white/70">Opt-out of result notification for this fight</span>
            </label>
          </div>
          <button disabled={locked || loading || (!!userPrediction && !isDirty)} className="rounded-2xl bg-accent px-4 py-3 font-semibold text-white disabled:opacity-50 md:col-span-3 mt-2">
            {locked ? "Locked" : loading ? "Submitting..." : (userPrediction ? "Update prediction" : "Submit prediction")}
          </button>
        </form>
      )}
    </div>
  );
}