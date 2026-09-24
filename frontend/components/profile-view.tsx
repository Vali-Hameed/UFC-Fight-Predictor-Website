"use client";

import { ProfileDto, apiFetch, BadgeDto } from "@/lib/api";
import { useAuth } from "@/lib/session";
import { useEffect, useMemo, useState } from "react";
import { ProfileEditor } from "./profile-editor";
import { CosmeticUsername } from "./cosmetic-username";

type ProfileViewProps = {
  initialProfile: ProfileDto | null;
  username: string;
};

type BadgeCategory = "championship" | "event" | "streak";

const BADGE_DISPLAY: Record<
  string,
  {
    emoji: string;
    color: string;
    bgColor: string;
    borderColor: string;
    description: string;
    category: BadgeCategory;
  }
> = {
  SEASON_CHAMPION: {
    emoji: "👑",
    color: "#FFD700",
    bgColor: "rgba(255, 215, 0, 0.08)",
    borderColor: "rgba(255, 215, 0, 0.3)",
    description: "Season Champion",
    category: "championship",
  },
  SEASON_SILVER: {
    emoji: "🥈",
    color: "#C0C0C0",
    bgColor: "rgba(192, 192, 192, 0.08)",
    borderColor: "rgba(192, 192, 192, 0.3)",
    description: "Season 2nd Place",
    category: "championship",
  },
  SEASON_BRONZE: {
    emoji: "🥉",
    color: "#CD7F32",
    bgColor: "rgba(205, 127, 50, 0.08)",
    borderColor: "rgba(205, 127, 50, 0.3)",
    description: "Season 3rd Place",
    category: "championship",
  },
  EVENT_WINNER: {
    emoji: "🏆",
    color: "#E53E3E",
    bgColor: "rgba(229, 62, 62, 0.08)",
    borderColor: "rgba(229, 62, 62, 0.3)",
    description: "Event Winner",
    category: "event",
  },
  PERFECT_EVENT: {
    emoji: "💎",
    color: "#00BFFF",
    bgColor: "rgba(0, 191, 255, 0.08)",
    borderColor: "rgba(0, 191, 255, 0.3)",
    description: "Perfect Event Score",
    category: "event",
  },
  STREAK_10: {
    emoji: "🔥",
    color: "#FF6B35",
    bgColor: "rgba(255, 107, 53, 0.08)",
    borderColor: "rgba(255, 107, 53, 0.3)",
    description: "10+ Win Streak",
    category: "streak",
  },
  STREAK_25: {
    emoji: "⚡",
    color: "#FF4500",
    bgColor: "rgba(255, 69, 0, 0.08)",
    borderColor: "rgba(255, 69, 0, 0.3)",
    description: "25+ Win Streak",
    category: "streak",
  },
};

type DisplayBadge = {
  key: string;
  badgeLabel: string;
  badgeType: string;
  count: number;
  config: (typeof BADGE_DISPLAY)[string];
  latestAwardedAt?: string | null;
};

export function ProfileView({ initialProfile, username }: ProfileViewProps) {
  const { user, token } = useAuth();
  const [profile, setProfile] = useState<ProfileDto | null>(initialProfile);
  const [activeTab, setActiveTab] = useState<"predictions" | "trophies" | "settings">("predictions");

  // Trophy filters
  const [badgeCategoryFilter, setBadgeCategoryFilter] = useState<string>("all");

  // Prediction filters & pagination
  const [predictionSearch, setPredictionSearch] = useState<string>("");
  const [predictionOutcomeFilter, setPredictionOutcomeFilter] = useState<"all" | "correct" | "incorrect" | "pending">("all");
  const [currentPage, setCurrentPage] = useState<number>(1);
  const EVENTS_PER_PAGE = 5;

  const isOwner = user?.username === username;

  useEffect(() => {
    // If the viewer is the owner and we have a token, fetch the authenticated profile to get private data
    if (isOwner && token && !initialProfile?.leaderboardStats) {
      apiFetch<ProfileDto>(`/api/v1/users/${username}`, {}, token)
        .then((data) => setProfile(data))
        .catch(() => {});
    } else {
      setProfile(initialProfile);
    }
  }, [user, username, token, initialProfile, isOwner]);

  // Badges sorted by most recent first, kept separate per event
  const displayedBadges = useMemo(() => {
    const badges = profile?.badges ?? [];

    // Sort badges with most recent first
    const sorted = [...badges].sort((a, b) => {
      const timeA = a.awardedAt ? new Date(a.awardedAt).getTime() : 0;
      const timeB = b.awardedAt ? new Date(b.awardedAt).getTime() : 0;
      return timeB - timeA;
    });

    // Group only exact duplicate labels, so each distinct event (e.g. "UFC 300 Winner", "UFC Freedom 250 Winner")
    // is kept as its own separate trophy card!
    return sorted.reduce<DisplayBadge[]>((acc, badge) => {
      const label = badge.badgeLabel || BADGE_DISPLAY[badge.badgeType]?.description || badge.badgeType;
      const existing = acc.find((b) => b.badgeLabel === label);
      if (existing) {
        existing.count += 1;
        if (badge.awardedAt && (!existing.latestAwardedAt || new Date(badge.awardedAt) > new Date(existing.latestAwardedAt))) {
          existing.latestAwardedAt = badge.awardedAt;
        }
      } else {
        const config = BADGE_DISPLAY[badge.badgeType] ?? {
          emoji: "🎖️",
          color: "#888",
          bgColor: "rgba(136, 136, 136, 0.08)",
          borderColor: "rgba(136, 136, 136, 0.3)",
          description: badge.badgeType,
          category: "event" as const,
        };
        acc.push({
          key: `${badge.id ?? label}-${label}`,
          badgeLabel: label,
          badgeType: badge.badgeType,
          count: 1,
          config,
          latestAwardedAt: badge.awardedAt,
        });
      }
      return acc;
    }, []);
  }, [profile?.badges]);

  const filteredBadges = useMemo(() => {
    return displayedBadges.filter((g) => {
      if (badgeCategoryFilter === "all") return true;
      return g.config.category === badgeCategoryFilter;
    });
  }, [displayedBadges, badgeCategoryFilter]);

  // Group predictions by event, sorted with upcoming (furthest away) first, then past (most recent to oldest)
  const eventEntries = useMemo(() => {
    const history = profile?.predictionHistory ?? [];
    const grouped = history.reduce((acc, pred) => {
      const eventId = pred.eventId ?? 0;
      if (!acc[eventId]) acc[eventId] = [];
      acc[eventId].push(pred);
      return acc;
    }, {} as Record<number, NonNullable<ProfileDto["predictionHistory"]>>);

    const now = Date.now();

    const entries = Object.entries(grouped).map(([eventId, preds]) => {
      const eventName = preds[0]?.eventName || `Event #${eventId}`;
      const eventDate = preds.find((p) => p.eventDate)?.eventDate || null;
      const eventStatus = preds.find((p) => p.eventStatus)?.eventStatus || null;

      // Sort fights on card: Main Event at top, then fightOrder ascending
      const sortedPreds = [...preds].sort((a, b) => {
        if (a.isMainEvent && !b.isMainEvent) return -1;
        if (!a.isMainEvent && b.isMainEvent) return 1;

        if (a.fightOrder != null && b.fightOrder != null) {
          return a.fightOrder - b.fightOrder;
        }
        if (a.fightOrder != null) return -1;
        if (b.fightOrder != null) return 1;

        return (a.fightId ?? 0) - (b.fightId ?? 0);
      });

      const completedPreds = sortedPreds.filter((p) => {
        if (!p.resultWinner) return false;
        const isFightCancelledOrNC = ["Canceled", "No Contest", "Canceled/No Contest"].includes(p.resultWinner);
        const userPredictedCancelledOrNC = ["Canceled", "No Contest", "Canceled/No Contest"].includes(p.predictedWinner || "");
        if (isFightCancelledOrNC && !userPredictedCancelledOrNC) return false;
        return true;
      });

      const totalCompleted = completedPreds.length;
      const correct = completedPreds.filter((p) => p.pointsAwarded && p.pointsAwarded > 0).length;
      const accuracyStr = totalCompleted > 0 ? `${Math.round((correct / totalCompleted) * 100)}%` : "N/A";

      // Determine whether this event is upcoming or past
      let isUpcoming = false;
      if (eventStatus === "UPCOMING" || eventStatus === "LIVE") {
        isUpcoming = true;
      } else if (eventStatus === "COMPLETED" || eventStatus === "ARCHIVED") {
        isUpcoming = false;
      } else if (eventDate) {
        isUpcoming = new Date(eventDate).getTime() >= now - 24 * 60 * 60 * 1000;
      } else {
        // Fallback: If no fight in this event has a resultWinner yet, treat as upcoming
        isUpcoming = !preds.some((p) => !!p.resultWinner);
      }

      const eventTimestamp = eventDate ? new Date(eventDate).getTime() : 0;

      return {
        eventId,
        eventName,
        eventDate,
        eventStatus,
        isUpcoming,
        eventTimestamp,
        preds: sortedPreds,
        totalCompleted,
        correct,
        accuracyStr,
      };
    });

    // Sort order:
    // 1. Upcoming events first, ordered from furthest away to soonest (descending by date / eventId)
    // 2. Past events second, ordered from most recent past to oldest (descending by date / eventId)
    return entries.sort((a, b) => {
      if (a.isUpcoming && !b.isUpcoming) return -1;
      if (!a.isUpcoming && b.isUpcoming) return 1;

      // Both upcoming OR both past:
      if (a.eventTimestamp !== b.eventTimestamp && a.eventTimestamp > 0 && b.eventTimestamp > 0) {
        return b.eventTimestamp - a.eventTimestamp;
      }
      return Number(b.eventId) - Number(a.eventId);
    });
  }, [profile?.predictionHistory]);

  // Filtered events based on search query & outcome filter
  const filteredEvents = useMemo(() => {
    const query = predictionSearch.toLowerCase().trim();

    return eventEntries
      .map((event) => {
        const matchesSearch = !query || event.eventName.toLowerCase().includes(query);
        if (!matchesSearch) return null;

        const matchingPreds = event.preds.filter((p) => {
          if (predictionOutcomeFilter === "all") return true;
          if (predictionOutcomeFilter === "pending") return !p.resultWinner;
          if (predictionOutcomeFilter === "correct") {
            return (p.pointsAwarded && p.pointsAwarded > 0) || p.isWinnerCorrect === true;
          }
          if (predictionOutcomeFilter === "incorrect") {
            const isCancelled = ["Canceled", "No Contest", "Canceled/No Contest"].includes(p.resultWinner || "");
            return !!p.resultWinner && !isCancelled && !p.isWinnerCorrect && (!p.pointsAwarded || p.pointsAwarded === 0);
          }
          return true;
        });

        if (matchingPreds.length === 0) return null;

        return {
          ...event,
          displayedPreds: matchingPreds,
        };
      })
      .filter((e): e is NonNullable<typeof e> => e !== null);
  }, [eventEntries, predictionSearch, predictionOutcomeFilter]);

  // Pagination calculation
  const totalPages = Math.max(1, Math.ceil(filteredEvents.length / EVENTS_PER_PAGE));
  const safeCurrentPage = Math.min(Math.max(1, currentPage), totalPages);
  const paginatedEvents = useMemo(() => {
    const startIndex = (safeCurrentPage - 1) * EVENTS_PER_PAGE;
    return filteredEvents.slice(startIndex, startIndex + EVENTS_PER_PAGE);
  }, [filteredEvents, safeCurrentPage]);

  if (!profile) {
    return (
      <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
        Profile data could not be loaded.
      </div>
    );
  }

  const totalBadges = profile.badges?.length ?? 0;
  const totalPredictions = profile.predictionHistory?.length ?? 0;

  return (
    <div className="space-y-6">
      {/* Profile Hero Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 sm:gap-4 rounded-3xl border border-white/10 bg-white/[0.03] p-4 sm:p-5 backdrop-blur-sm">
        <div className="flex items-center gap-3 min-w-0">
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

        {/* Quick Trophy Pill */}
        <div className="flex items-center gap-2 shrink-0 self-start sm:self-auto">
          {totalBadges > 0 ? (
            <button
              type="button"
              onClick={() => setActiveTab("trophies")}
              aria-label="View trophies"
              className="group inline-flex items-center gap-2 rounded-full border border-gold/30 bg-gold/10 px-3.5 py-1.5 text-xs font-semibold text-gold transition hover:border-gold/60 hover:bg-gold/20"
            >
              <span className="text-base group-hover:scale-110 transition-transform">🏅</span>
              <span>
                {totalBadges} {totalBadges === 1 ? "Trophy" : "Trophies"}
              </span>
              <span className="text-[10px] text-gold/70 group-hover:translate-x-0.5 transition-transform">→</span>
            </button>
          ) : (
            <span className="inline-flex items-center gap-1.5 rounded-full border border-white/10 bg-white/5 px-3 py-1 text-xs text-white/50">
              <span>🏅</span>
              <span>No trophies yet</span>
            </span>
          )}
        </div>
      </div>

      {/* Leaderboard Key Stats Banner */}
      {!profile.leaderboardStats ? (
        <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
          This profile is private.
        </div>
      ) : (
        <div className="space-y-6">
          <div className="grid gap-3 grid-cols-2 md:grid-cols-4">
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 transition hover:border-white/20">
              <p className="text-xs font-medium uppercase tracking-wider text-white/50">Rank</p>
              <p className="mt-1 text-2xl md:text-3xl font-semibold text-white">
                {profile.leaderboardStats.rank ? `#${profile.leaderboardStats.rank}` : "Unranked"}
              </p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 transition hover:border-gold/30">
              <p className="text-xs font-medium uppercase tracking-wider text-white/50">Total Points</p>
              <p className="mt-1 text-2xl md:text-3xl font-semibold text-gold">
                {profile.leaderboardStats.totalPoints}
              </p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 transition hover:border-white/20">
              <p className="text-xs font-medium uppercase tracking-wider text-white/50">Win Rate</p>
              <p className="mt-1 text-2xl md:text-3xl font-semibold text-white">
                {Math.round(profile.leaderboardStats.winRate * 100)}%
              </p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 transition hover:border-white/20">
              <p className="text-xs font-medium uppercase tracking-wider text-white/50">Total Picks</p>
              <p className="mt-1 text-2xl md:text-3xl font-semibold text-white">
                {totalPredictions}
              </p>
            </div>
          </div>

          {/* Navigation Tabs */}
          <div className="flex items-center gap-1.5 sm:gap-2 border-b border-white/10 pb-4 overflow-x-auto no-scrollbar">
            <button
              type="button"
              onClick={() => setActiveTab("predictions")}
              className={`flex items-center gap-2 rounded-xl px-3 sm:px-4 py-2 sm:py-2.5 text-xs sm:text-sm font-medium whitespace-nowrap transition shrink-0 ${
                activeTab === "predictions"
                  ? "bg-accent text-white shadow-lg shadow-accent/20"
                  : "text-white/60 hover:bg-white/5 hover:text-white"
              }`}
            >
              <span>🔮 Predictions</span>
              <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                activeTab === "predictions" ? "bg-white/20 text-white" : "bg-white/10 text-white/70"
              }`}>
                {eventEntries.length}
              </span>
            </button>

            <button
              type="button"
              onClick={() => setActiveTab("trophies")}
              className={`flex items-center gap-2 rounded-xl px-3 sm:px-4 py-2 sm:py-2.5 text-xs sm:text-sm font-medium whitespace-nowrap transition shrink-0 ${
                activeTab === "trophies"
                  ? "bg-accent text-white shadow-lg shadow-accent/20"
                  : "text-white/60 hover:bg-white/5 hover:text-white"
              }`}
            >
              <span>🏅 Trophy Case</span>
              <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${
                activeTab === "trophies" ? "bg-white/20 text-white" : "bg-white/10 text-white/70"
              }`}>
                {totalBadges}
              </span>
            </button>

            {isOwner && (
              <button
                type="button"
                onClick={() => setActiveTab("settings")}
                className={`flex items-center gap-2 rounded-xl px-3 sm:px-4 py-2 sm:py-2.5 text-xs sm:text-sm font-medium whitespace-nowrap transition shrink-0 ${
                  activeTab === "settings"
                    ? "bg-accent text-white shadow-lg shadow-accent/20"
                    : "text-white/60 hover:bg-white/5 hover:text-white"
                }`}
              >
                <span>⚙️ Account Settings</span>
              </button>
            )}
          </div>

          {/* Tab 1: Predictions */}
          <div className={activeTab === "predictions" ? "space-y-4" : "hidden"} data-tab="predictions">
            {eventEntries.length === 0 ? (
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
                No prediction history found.
              </div>
            ) : (
              <div className="space-y-4">
                {/* Search & Filter Bar */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-2xl border border-white/10 bg-white/[0.03] p-3 backdrop-blur-sm">
                  <div className="flex flex-1 items-center gap-2 min-w-0">
                    <span className="text-white/40 text-sm">🔍</span>
                    <input
                      type="text"
                      placeholder="Search events (e.g. UFC 300)..."
                      value={predictionSearch}
                      onChange={(e) => {
                        setPredictionSearch(e.target.value);
                        setCurrentPage(1);
                      }}
                      className="w-full bg-transparent text-sm text-white placeholder:text-white/40 outline-none"
                    />
                    {predictionSearch && (
                      <button
                        type="button"
                        onClick={() => {
                          setPredictionSearch("");
                          setCurrentPage(1);
                        }}
                        className="rounded-full bg-white/10 p-1 text-xs text-white/60 hover:bg-white/20 hover:text-white"
                        aria-label="Clear search"
                      >
                        ✕
                      </button>
                    )}
                  </div>

                  <div className="flex items-center justify-between sm:justify-end gap-2 w-full sm:w-auto">
                    <label htmlFor="outcome-filter" className="text-xs text-white/50 whitespace-nowrap">
                      Outcome:
                    </label>
                    <select
                      id="outcome-filter"
                      value={predictionOutcomeFilter}
                      onChange={(e) => {
                        setPredictionOutcomeFilter(e.target.value as any);
                        setCurrentPage(1);
                      }}
                      className="flex-1 sm:flex-initial rounded-xl border border-white/10 bg-bg/95 px-3 py-1.5 text-xs text-white outline-none focus:border-accent"
                    >
                      <option value="all">All Outcomes</option>
                      <option value="correct">Correct Picks Only</option>
                      <option value="incorrect">Incorrect Picks Only</option>
                      <option value="pending">Pending Results Only</option>
                    </select>
                  </div>
                </div>

                {/* Event List */}
                {filteredEvents.length === 0 ? (
                  <div className="rounded-2xl border border-white/10 bg-white/5 p-6 text-center">
                    <p className="text-sm text-white/70">No predictions match your search or filter.</p>
                    <button
                      type="button"
                      onClick={() => {
                        setPredictionSearch("");
                        setPredictionOutcomeFilter("all");
                        setCurrentPage(1);
                      }}
                      className="mt-3 rounded-xl border border-white/20 bg-white/5 px-4 py-1.5 text-xs font-medium text-white hover:bg-white/10"
                    >
                      Reset Filters
                    </button>
                  </div>
                ) : (
                  <div className="grid gap-3">
                    {paginatedEvents.map((event) => (
                      <details
                        key={event.eventId}
                        className="group rounded-2xl border border-white/10 bg-white/5 transition hover:border-white/20 [&_summary::-webkit-details-marker]:hidden"
                      >
                        <summary className="flex cursor-pointer items-center justify-between p-3.5 sm:p-4 outline-none hover:bg-white/[0.02]">
                          <div className="min-w-0 pr-2">
                            <p className="text-base font-semibold text-white break-words">{event.eventName}</p>
                            <p className="text-xs sm:text-sm text-white/50">
                              {event.preds.length} prediction{event.preds.length !== 1 ? "s" : ""} • {event.accuracyStr} accuracy
                              {predictionOutcomeFilter !== "all" && event.displayedPreds.length !== event.preds.length && (
                                <span className="ml-1 text-gold">({event.displayedPreds.length} matching)</span>
                              )}
                            </p>
                          </div>
                          <div className="text-white/50 transition-transform duration-200 group-open:rotate-180 shrink-0">
                            <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                              <path d="m6 9 6 6 6-6" />
                            </svg>
                          </div>
                        </summary>
                        <div className="grid gap-3 p-3.5 sm:p-4 pt-0">
                          {event.displayedPreds.map((pred) => (
                            <div
                              key={pred.fightId}
                              className={`rounded-xl border p-3.5 sm:p-4 transition ${
                                pred.isWinnerCorrect
                                  ? "border-emerald-500/25 bg-emerald-500/[0.03]"
                                  : pred.resultWinner && !["Canceled", "No Contest", "Canceled/No Contest"].includes(pred.resultWinner)
                                  ? "border-red-500/20 bg-red-500/[0.03]"
                                  : "border-white/5 bg-white/5"
                              }`}
                            >
                              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2.5 sm:gap-4">
                                <div className="min-w-0">
                                  <div className="flex flex-wrap items-center gap-1.5 sm:gap-2">
                                    <p className="text-sm font-medium text-white break-words">
                                      {pred.fighter1Name} vs {pred.fighter2Name}
                                    </p>
                                    {pred.isMainEvent && (
                                      <span className="rounded-full bg-gold/15 px-2 py-0.5 text-[10px] font-semibold text-gold border border-gold/30 uppercase tracking-wider shrink-0">
                                        Main Event
                                      </span>
                                    )}
                                    {pred.isWinnerCorrect ? (
                                      <span className="rounded-full bg-emerald-500/15 px-2 py-0.5 text-[10px] font-semibold text-emerald-400 border border-emerald-500/30 shrink-0">
                                        +{pred.pointsAwarded ?? 0} pts
                                      </span>
                                    ) : pred.resultWinner && !["Canceled", "No Contest", "Canceled/No Contest"].includes(pred.resultWinner) ? (
                                      <span className="rounded-full bg-red-500/15 px-2 py-0.5 text-[10px] font-semibold text-red-400 border border-red-500/30 shrink-0">
                                        0 pts
                                      </span>
                                    ) : null}
                                  </div>
                                  <p className="mt-1 text-xs text-white/50">
                                    {pred.resultWinner
                                      ? ["Canceled", "Draw", "No Contest", "Canceled/No Contest"].includes(pred.resultWinner)
                                        ? `Result: ${pred.resultWinner}`
                                        : `Result: ${pred.resultWinner} by ${pred.resultMethod || "Decision"}${pred.resultRound ? ` (Round ${pred.resultRound})` : ""}`
                                      : "Pending result"}
                                  </p>
                                </div>
                                <div className="text-left sm:text-right border-t border-white/5 pt-2 sm:border-0 sm:pt-0 shrink-0">
                                  <p className="text-sm font-semibold text-gold">
                                    {pred.predictedWinner} {pred.predictedMethod !== "Any Method" ? `by ${pred.predictedMethod}` : ""}
                                  </p>
                                  <p className="mt-0.5 text-xs text-white/50">
                                    {pred.predictedRound ? `Round ${pred.predictedRound}` : "Any Round"} • {pred.submittedAt ? new Date(pred.submittedAt).toLocaleDateString() : ""}
                                  </p>
                                </div>
                              </div>
                            </div>
                          ))}
                        </div>
                      </details>
                    ))}
                  </div>
                )}

                {/* Pagination Controls */}
                {totalPages > 1 && (
                  <div className="flex items-center justify-between gap-2 sm:gap-4 border-t border-white/10 pt-4">
                    <button
                      type="button"
                      onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                      disabled={safeCurrentPage === 1}
                      aria-label="← Previous"
                      className="inline-flex w-24 sm:w-28 items-center justify-center gap-1 sm:gap-1.5 rounded-xl border border-white/20 bg-white/5 py-2 text-xs font-semibold text-white transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      <span>←</span>
                      <span className="sm:hidden">Prev</span>
                      <span className="hidden sm:inline">Previous</span>
                    </button>

                    <div className="flex flex-col items-center justify-center text-center px-1">
                      <span className="text-xs font-medium text-white/80 whitespace-nowrap">
                        Page <strong className="text-white font-bold">{safeCurrentPage}</strong> of {totalPages}
                      </span>
                      <span className="text-[10px] text-white/40 whitespace-nowrap">
                        {filteredEvents.length} {filteredEvents.length === 1 ? "event" : "events"}
                      </span>
                    </div>

                    <button
                      type="button"
                      onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                      disabled={safeCurrentPage >= totalPages}
                      aria-label="Next →"
                      className="inline-flex w-24 sm:w-28 items-center justify-center gap-1 sm:gap-1.5 rounded-xl border border-white/20 bg-white/5 py-2 text-xs font-semibold text-white transition hover:bg-white/10 disabled:cursor-not-allowed disabled:opacity-40"
                    >
                      <span>Next</span>
                      <span>→</span>
                    </button>
                  </div>
                )}
              </div>
            )}
          </div>

          {/* Tab 2: Trophy Case */}
          <div className={activeTab === "trophies" ? "space-y-4" : "hidden"} data-tab="trophies">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 rounded-2xl border border-white/10 bg-white/[0.03] p-3 backdrop-blur-sm">
              <div className="flex items-center gap-2">
                <span className="text-lg">🏅</span>
                <h4 className="text-sm sm:text-base font-semibold text-white">Trophy Showcase</h4>
                <span className="rounded-full bg-white/10 px-2 py-0.5 text-xs font-medium text-white/60">
                  {filteredBadges.length} {filteredBadges.length === 1 ? "badge" : "badges"}
                </span>
              </div>

              {/* Category Dropdown Filter */}
              <div className="flex items-center justify-between sm:justify-end gap-2 w-full sm:w-auto">
                <label htmlFor="badge-category-filter" className="text-xs text-white/50 whitespace-nowrap">
                  Category:
                </label>
                <select
                  id="badge-category-filter"
                  value={badgeCategoryFilter}
                  onChange={(e) => setBadgeCategoryFilter(e.target.value)}
                  className="flex-1 sm:flex-initial rounded-xl border border-white/10 bg-bg/95 px-3 py-1.5 text-xs text-white outline-none focus:border-accent"
                >
                  <option value="all">All Trophies ({displayedBadges.length})</option>
                  <option value="event">Event Wins ({displayedBadges.filter(b => b.config.category === "event").length})</option>
                  <option value="championship">Championships ({displayedBadges.filter(b => b.config.category === "championship").length})</option>
                  <option value="streak">Win Streaks ({displayedBadges.filter(b => b.config.category === "streak").length})</option>
                </select>
              </div>
            </div>

            {filteredBadges.length === 0 ? (
              <div className="rounded-2xl border border-white/10 bg-white/5 p-6 sm:p-8 text-center">
                <span className="text-4xl">🏆</span>
                <h4 className="mt-3 text-base font-semibold text-white">No Trophies in this Category</h4>
                <p className="mt-1 text-sm text-white/50 max-w-md mx-auto">
                  {displayedBadges.length === 0
                    ? "Trophies and badges are unlocked by winning fight predictions, achieving win streaks, and finishing at the top of season leaderboards."
                    : "No unlocked trophies match the selected filter."}
                </p>
                {badgeCategoryFilter !== "all" && (
                  <button
                    type="button"
                    onClick={() => setBadgeCategoryFilter("all")}
                    className="mt-4 rounded-xl border border-white/20 bg-white/5 px-4 py-1.5 text-xs font-medium text-white hover:bg-white/10"
                  >
                    View All Trophies
                  </button>
                )}
              </div>
            ) : (
              <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
                {filteredBadges.map((badge) => {
                  const { config } = badge;
                  return (
                    <div
                      key={badge.key}
                      className="group relative w-full max-w-full overflow-hidden rounded-2xl border p-3.5 sm:p-4 transition-all sm:hover:scale-[1.01]"
                      style={{
                        borderColor: config.borderColor,
                        backgroundColor: config.bgColor,
                      }}
                    >
                      <div className="flex items-start gap-3 min-w-0">
                        <span
                          className="text-2xl shrink-0"
                          style={{ filter: `drop-shadow(0 0 8px ${config.color}60)` }}
                        >
                          {config.emoji}
                        </span>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center gap-2 min-w-0">
                            <p className="text-sm font-semibold truncate min-w-0 flex-1" style={{ color: config.color }}>
                              {badge.badgeLabel}
                            </p>
                            {badge.count > 1 && (
                              <span
                                className="rounded-full px-2 py-0.5 text-xs font-bold shrink-0"
                                style={{
                                  backgroundColor: `${config.color}25`,
                                  color: config.color,
                                  border: `1px solid ${config.color}50`,
                                }}
                              >
                                ×{badge.count}
                              </span>
                            )}
                          </div>
                          {config.description !== badge.badgeLabel && (
                            <p className="text-xs text-white/50 mt-0.5 line-clamp-1">{config.description}</p>
                          )}
                          {badge.latestAwardedAt && (
                            <p className="text-[10px] text-white/35 mt-1.5 truncate">
                              {badge.count > 1
                                ? `Earned ${badge.count}× • Latest ${new Date(badge.latestAwardedAt).toLocaleDateString()}`
                                : `Earned ${new Date(badge.latestAwardedAt).toLocaleDateString()}`}
                            </p>
                          )}
                        </div>
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Tab 3: Account Settings (Owner Only) */}
      {isOwner && (
        <div className={activeTab === "settings" ? "block" : "hidden"} data-tab="settings">
          <ProfileEditor username={username} />
        </div>
      )}
    </div>
  );
}
