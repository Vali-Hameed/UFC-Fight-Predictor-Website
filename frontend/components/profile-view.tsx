"use client";

import { ProfileDto, apiFetch, BadgeDto } from "@/lib/api";
import { useAuth } from "@/lib/session";
import { useEffect, useState } from "react";
import { ProfileEditor } from "./profile-editor";
import { CosmeticUsername } from "./cosmetic-username";

type ProfileViewProps = {
  initialProfile: ProfileDto | null;
  username: string;
};

const BADGE_DISPLAY: Record<string, { emoji: string; color: string; bgColor: string; borderColor: string; description: string }> = {
  SEASON_CHAMPION: { emoji: "👑", color: "#FFD700", bgColor: "rgba(255, 215, 0, 0.08)", borderColor: "rgba(255, 215, 0, 0.3)", description: "Season Champion" },
  SEASON_SILVER: { emoji: "🥈", color: "#C0C0C0", bgColor: "rgba(192, 192, 192, 0.08)", borderColor: "rgba(192, 192, 192, 0.3)", description: "Season 2nd Place" },
  SEASON_BRONZE: { emoji: "🥉", color: "#CD7F32", bgColor: "rgba(205, 127, 50, 0.08)", borderColor: "rgba(205, 127, 50, 0.3)", description: "Season 3rd Place" },
  EVENT_WINNER: { emoji: "🏆", color: "#E53E3E", bgColor: "rgba(229, 62, 62, 0.08)", borderColor: "rgba(229, 62, 62, 0.3)", description: "Event Winner" },
  PERFECT_EVENT: { emoji: "💎", color: "#00BFFF", bgColor: "rgba(0, 191, 255, 0.08)", borderColor: "rgba(0, 191, 255, 0.3)", description: "Perfect Event Score" },
  STREAK_10: { emoji: "🔥", color: "#FF6B35", bgColor: "rgba(255, 107, 53, 0.08)", borderColor: "rgba(255, 107, 53, 0.3)", description: "10+ Win Streak" },
  STREAK_25: { emoji: "⚡", color: "#FF4500", bgColor: "rgba(255, 69, 0, 0.08)", borderColor: "rgba(255, 69, 0, 0.3)", description: "25+ Win Streak" },
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
      {/* Profile Header with Cosmetic Username */}
      <div className="flex items-center gap-3">
        <CosmeticUsername
          username={profile.username ?? username}
          cosmeticGlowColor={profile.cosmeticGlowColor}
          cosmeticTitle={profile.cosmeticTitle}
          badges={profile.badges}
          size="lg"
          linkToProfile={false}
          showBadges={true}
          showTitle={true}
        />
      </div>

      {/* Trophy Case / Badges */}
      {profile.badges && profile.badges.length > 0 && (
        <div className="space-y-3">
          <h4 className="text-lg font-medium text-white flex items-center gap-2">
            <span>🏅</span> Trophy Case
          </h4>
          <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
            {profile.badges.map((badge) => {
              const config = BADGE_DISPLAY[badge.badgeType] ?? {
                emoji: "🎖️",
                color: "#888",
                bgColor: "rgba(136, 136, 136, 0.08)",
                borderColor: "rgba(136, 136, 136, 0.3)",
                description: badge.badgeType,
              };
              return (
                <div
                  key={badge.id}
                  className="group relative rounded-2xl border p-4 transition-all hover:scale-[1.02]"
                  style={{
                    borderColor: config.borderColor,
                    backgroundColor: config.bgColor,
                  }}
                >
                  <div className="flex items-start gap-3">
                    <span
                      className="text-2xl"
                      style={{ filter: `drop-shadow(0 0 6px ${config.color}50)` }}
                    >
                      {config.emoji}
                    </span>
                    <div>
                      <p className="text-sm font-semibold" style={{ color: config.color }}>
                        {badge.badgeLabel}
                      </p>
                      <p className="text-xs text-white/40 mt-0.5">{config.description}</p>
                      {badge.awardedAt && (
                        <p className="text-[10px] text-white/30 mt-1">
                          Earned {new Date(badge.awardedAt).toLocaleDateString()}
                        </p>
                      )}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

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
                  
                  const completedPreds = preds.filter(p => {
                    if (!p.resultWinner) return false;
                    const isFightCancelledOrNC = ["Canceled", "No Contest", "Canceled/No Contest"].includes(p.resultWinner);
                    const userPredictedCancelledOrNC = ["Canceled", "No Contest", "Canceled/No Contest"].includes(p.predictedWinner || "");
                    if (isFightCancelledOrNC && !userPredictedCancelledOrNC) return false;
                    return true;
                  });

                  const totalCompleted = completedPreds.length;
                  const correct = completedPreds.filter(p => p.pointsAwarded && p.pointsAwarded > 0).length;
                  const accuracyStr = totalCompleted > 0 ? `${Math.round((correct / totalCompleted) * 100)}%` : "N/A";

                  return (
                    <details key={eventId} className="group rounded-2xl border border-white/10 bg-white/5 [&_summary::-webkit-details-marker]:hidden">
                      <summary className="flex cursor-pointer items-center justify-between p-4 outline-none">
                        <div>
                          <p className="text-base font-semibold text-white">{eventName}</p>
                          <p className="text-sm text-white/50">
                            {preds.length} prediction{preds.length !== 1 ? 's' : ''} • {accuracyStr} accuracy
                          </p>
                        </div>
                        <div className="text-white/50 transition-transform group-open:rotate-180">
                          <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m6 9 6 6 6-6"/></svg>
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
