"use client";

import { useState, useEffect } from "react";
import {
  LeaderboardDto,
  LeaderboardFiltersDto,
  SeasonFilterDto,
  EventFilterDto,
  apiFetch,
} from "@/lib/api";
import { CosmeticUsername } from "@/components/cosmetic-username";

type LeaderboardViewProps = {
  initialLeaderboard: LeaderboardDto[];
  initialFilters: LeaderboardFiltersDto | null;
};

type FilterMode = "all-time" | "season" | "event";

export function LeaderboardView({ initialLeaderboard, initialFilters }: LeaderboardViewProps) {
  const [mode, setMode] = useState<FilterMode>("all-time");
  const [leaderboard, setLeaderboard] = useState<LeaderboardDto[]>(initialLeaderboard);
  const [loading, setLoading] = useState(false);
  const [selectedSeason, setSelectedSeason] = useState<SeasonFilterDto | null>(
    initialFilters?.seasons?.find((s) => s.active) ?? null
  );
  const [selectedEvent, setSelectedEvent] = useState<EventFilterDto | null>(
    initialFilters?.recentEvents?.[0] ?? null
  );
  const filters = initialFilters;

  const fetchLeaderboard = async (newMode: FilterMode, seasonId?: number, eventId?: number) => {
    setLoading(true);
    try {
      let data: LeaderboardDto[];
      switch (newMode) {
        case "season":
          if (!seasonId) return;
          data = await apiFetch<LeaderboardDto[]>(`/api/v1/leaderboard/season/${seasonId}`);
          break;
        case "event":
          if (!eventId) return;
          data = await apiFetch<LeaderboardDto[]>(`/api/v1/leaderboard/event/${eventId}`);
          break;
        default:
          data = await apiFetch<LeaderboardDto[]>("/api/v1/leaderboard");
      }
      setLeaderboard(data);
    } catch {
      setLeaderboard([]);
    } finally {
      setLoading(false);
    }
  };

  const handleModeChange = (newMode: FilterMode) => {
    setMode(newMode);
    if (newMode === "all-time") {
      setLeaderboard(initialLeaderboard);
    } else if (newMode === "season" && selectedSeason) {
      fetchLeaderboard("season", selectedSeason.id);
    } else if (newMode === "event" && selectedEvent) {
      fetchLeaderboard("event", undefined, selectedEvent.id);
    }
  };

  const handleSeasonChange = (seasonId: number) => {
    const season = filters?.seasons.find((s) => s.id === seasonId) ?? null;
    setSelectedSeason(season);
    if (season) fetchLeaderboard("season", season.id);
  };

  const handleEventChange = (eventId: number) => {
    const event = filters?.recentEvents.find((e) => e.id === eventId) ?? null;
    setSelectedEvent(event);
    if (event) fetchLeaderboard("event", undefined, event.id);
  };

  return (
    <div className="space-y-6">
      {/* Mode Tabs */}
      <div className="flex flex-wrap gap-2">
        {(["all-time", "season", "event"] as FilterMode[]).map((m) => (
          <button
            key={m}
            onClick={() => handleModeChange(m)}
            className={`rounded-xl px-4 py-2 text-sm font-semibold transition-all ${
              mode === m
                ? "bg-accent text-white shadow-glow"
                : "bg-white/5 text-white/60 hover:bg-white/10 hover:text-white"
            }`}
          >
            {m === "all-time" ? "All-Time" : m === "season" ? "Season" : "Event"}
          </button>
        ))}
      </div>

      {/* Season/Event Selector */}
      {mode === "season" && filters?.seasons && filters.seasons.length > 0 && (
        <div className="flex flex-wrap items-center gap-3">
          <label className="text-sm text-white/50">Season:</label>
          <select
            value={selectedSeason?.id ?? ""}
            onChange={(e) => handleSeasonChange(Number(e.target.value))}
            className="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none focus:border-accent"
          >
            {filters.seasons.map((s) => (
              <option key={s.id} value={s.id} className="bg-panel">
                {s.name} {s.active ? "(Current)" : ""} {s.championUsername ? `🏆 ${s.championUsername}` : ""}
              </option>
            ))}
          </select>
        </div>
      )}

      {mode === "event" && filters?.recentEvents && filters.recentEvents.length > 0 && (
        <div className="flex flex-wrap items-center gap-3">
          <label className="text-sm text-white/50">Event:</label>
          <select
            value={selectedEvent?.id ?? ""}
            onChange={(e) => handleEventChange(Number(e.target.value))}
            className="rounded-xl border border-white/10 bg-white/5 px-3 py-2 text-sm text-white outline-none focus:border-accent"
          >
            {filters.recentEvents.map((e) => (
              <option key={e.id} value={e.id} className="bg-panel">
                {e.name}
              </option>
            ))}
          </select>
        </div>
      )}

      {mode === "season" && (!filters?.seasons || filters.seasons.length === 0) && (
        <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
          No seasons have been created yet. Seasons are automatically created when predictions are scored.
        </div>
      )}

      {mode === "event" && (!filters?.recentEvents || filters.recentEvents.length === 0) && (
        <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
          No completed events found.
        </div>
      )}

      {/* Leaderboard Header */}
      <div className="text-xs uppercase tracking-widest text-white/40">
        {mode === "all-time" && "All-Time Rankings"}
        {mode === "season" && selectedSeason && `${selectedSeason.name} Season Rankings`}
        {mode === "event" && selectedEvent && `${selectedEvent.name} Rankings`}
      </div>

      {/* Leaderboard Rows */}
      {loading ? (
        <div className="flex items-center justify-center py-12">
          <div className="h-8 w-8 animate-spin rounded-full border-2 border-white/20 border-t-accent" />
        </div>
      ) : (
        <div className="space-y-3">
          {leaderboard.map((row, index) => {
            const isTop3 = index < 3;
            const rankColors = ["text-gold", "text-silver", "text-bronze"];
            const rankBorders = [
              "border-gold/30 bg-gold/5",
              "border-[#C0C0C0]/30 bg-[#C0C0C0]/5",
              "border-[#CD7F32]/30 bg-[#CD7F32]/5",
            ];

            return (
              <div
                key={`${row.userId}-${index}`}
                className={`grid grid-cols-[auto_1fr_auto] items-center gap-4 rounded-2xl border px-4 py-3 transition-all hover:bg-white/[0.08] ${
                  isTop3
                    ? rankBorders[index]
                    : "border-white/10 bg-white/5"
                }`}
              >
                <div
                  className={`text-lg font-bold ${
                    isTop3 ? rankColors[index] : "text-white/50"
                  }`}
                >
                  #{index + 1}
                </div>
                <div>
                  <CosmeticUsername
                    username={row.username ?? `User #${row.userId}`}
                    cosmeticGlowColor={row.cosmeticGlowColor}
                    cosmeticTitle={row.cosmeticTitle}
                    badges={row.badges}
                    size="md"
                    showTitle={isTop3}
                  />
                  <div className="text-sm text-white/50">
                    {row.correctPredictions ?? 0} correct predictions •{" "}
                    {Math.round(
                      ((row.correctPredictions ?? 0) /
                        Math.max(row.totalPredictions ?? 1, 1)) *
                        100
                    )}
                    % win rate
                  </div>
                </div>
                <div className="text-right">
                  <div className="font-semibold text-white">
                    {row.totalPoints ?? 0} pts
                  </div>
                  <div className="text-sm text-white/50">
                    {row.currentStreak ?? 0} streak
                  </div>
                </div>
              </div>
            );
          })}
          {leaderboard.length === 0 && (
            <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">
              {mode === "all-time"
                ? "Leaderboard has not been populated yet."
                : "No ranking data for this selection yet."}
            </div>
          )}
        </div>
      )}
    </div>
  );
}
