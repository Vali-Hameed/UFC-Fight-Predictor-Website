"use client";

import { ProfileDto, apiFetch } from "@/lib/api";
import { useAuth } from "@/lib/session";
import { useEffect, useState } from "react";
import { ProfileEditor } from "./profile-editor";

type ProfileViewProps = {
  initialProfile: ProfileDto | null;
  username: string;
};

export function ProfileView({ initialProfile, username }: ProfileViewProps) {
  const { user, token } = useAuth();
  const [profile, setProfile] = useState<ProfileDto | null>(initialProfile);

  useEffect(() => {
    // If the viewer is the owner and we have a token, fetch the authenticated profile to get private data
    if (user?.username === username && token && !initialProfile?.leaderboardStats) {
      apiFetch<ProfileDto>(`/api/v1/users/${username}`, {}, token)
        .then((data) => setProfile(data))
        .catch(() => {});
    } else {
      setProfile(initialProfile);
    }
  }, [user, username, token, initialProfile]);

  const groupedPredictions = profile?.predictionHistory?.reduce((acc, pred) => {
    const eventId = pred.eventId ?? 0;
    if (!acc[eventId]) acc[eventId] = [];
    acc[eventId].push(pred);
    return acc;
  }, {} as Record<number, NonNullable<typeof profile.predictionHistory>>) ?? {};

  if (!profile) {
    return (
      <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
        Profile data could not be loaded.
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {!profile.leaderboardStats ? (
        <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
          This profile is private.
        </div>
      ) : (
        <div className="space-y-6">
          <div className="grid gap-4 md:grid-cols-3">
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
              <p className="text-sm text-white/50">Rank</p>
              <p className="mt-2 text-3xl font-semibold text-white">
                {profile.leaderboardStats.rank ? `#${profile.leaderboardStats.rank}` : "Unranked"}
              </p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
              <p className="text-sm text-white/50">Total Points</p>
              <p className="mt-2 text-3xl font-semibold text-white">{profile.leaderboardStats.totalPoints}</p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
              <p className="text-sm text-white/50">Win Rate</p>
              <p className="mt-2 text-3xl font-semibold text-white">
                {Math.round(profile.leaderboardStats.winRate * 100)}%
              </p>
            </div>
          </div>

          {Object.keys(groupedPredictions).length > 0 ? (
            <div className="space-y-3">
              <h4 className="text-lg font-medium text-white">Prediction History</h4>
              <div className="grid gap-3">
                {Object.entries(groupedPredictions).map(([eventId, preds]) => {
                  const eventName = preds[0]?.eventName || `Event #${eventId}`;
                  const total = preds.length;
                  const correct = preds.filter(p => p.pointsAwarded && p.pointsAwarded > 0).length;
                  const accuracy = total > 0 ? Math.round((correct / total) * 100) : 0;

                  return (
                    <details key={eventId} className="group rounded-2xl border border-white/10 bg-white/5 [&_summary::-webkit-details-marker]:hidden">
                      <summary className="flex cursor-pointer items-center justify-between p-4 outline-none">
                        <div>
                          <p className="text-base font-semibold text-white">{eventName}</p>
                          <p className="text-sm text-white/50">
                            {total} prediction{total > 1 ? 's' : ''} • {accuracy}% accuracy
                          </p>
                        </div>
                        <div className="text-white/50 transition-transform group-open:rotate-180">
                          <svg xmlns="http://www.apache.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m6 9 6 6 6-6"/></svg>
                        </div>
                      </summary>
                      <div className="grid gap-3 p-4 pt-0">
                        {preds.map((pred) => (
                          <div key={pred.fightId} className="rounded-xl border border-white/5 bg-white/5 p-4">
                            <div className="flex flex-wrap items-center justify-between gap-4">
                              <div>
                                <p className="text-sm font-medium text-white">{pred.fighter1Name} vs {pred.fighter2Name}</p>
                                <p className="mt-1 text-xs text-white/50">
                                  {pred.resultWinner 
                                    ? (["Canceled", "Draw", "No Contest", "Canceled/No Contest"].includes(pred.resultWinner) 
                                        ? `Result: ${pred.resultWinner}` 
                                        : `Result: ${pred.resultWinner} by ${pred.resultMethod || 'Decision'}${pred.resultRound ? ` (Round ${pred.resultRound})` : ''}`)
                                    : "Pending result"}
                                </p>
                              </div>
                              <div className="text-right">
                                <p className="text-sm font-semibold text-gold">
                                  {pred.predictedWinner} {pred.predictedMethod !== "Any Method" ? `by ${pred.predictedMethod}` : ""}
                                </p>
                                <p className="mt-1 text-xs text-white/50">
                                  {pred.predictedRound ? `Round ${pred.predictedRound}` : "Any Round"} • {new Date(pred.submittedAt!).toLocaleDateString()}
                                </p>
                              </div>
                            </div>
                          </div>
                        ))}
                      </div>
                    </details>
                  );
                })}
              </div>
            </div>
          ) : (
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
              No prediction history found.
            </div>
          )}
        </div>
      )}

      {user?.username === username && (
        <ProfileEditor username={username} />
      )}
    </div>
  );
}
