import { SectionCard } from "@/components/section-card";
import { apiFetch, LeaderboardDto } from "@/lib/api";

export default async function LeaderboardPage() {
  const leaderboard = await apiFetch<LeaderboardDto[]>("/api/v1/leaderboard").catch(() => []);

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Ranks" title="Global leaderboard" description="Ranked by total points, with current streak and win rate visible.">
        <div className="space-y-3">
          {leaderboard.map((row, index) => (
            <div key={row.userId} className="grid grid-cols-[auto_1fr_auto] items-center gap-4 rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
              <div className="text-lg font-semibold text-gold">#{index + 1}</div>
              <div>
                <div className="font-semibold text-white">User #{row.userId}</div>
                <div className="text-sm text-white/50">{row.correctPredictions ?? 0} correct predictions • {Math.round(((row.correctPredictions ?? 0) / Math.max(row.totalPredictions ?? 1, 1)) * 100)}% win rate</div>
              </div>
              <div className="text-right">
                <div className="font-semibold text-white">{row.totalPoints ?? 0} pts</div>
                <div className="text-sm text-white/50">{row.currentStreak ?? 0} streak</div>
              </div>
            </div>
          ))}
          {leaderboard.length === 0 ? <div className="rounded-2xl border border-white/10 bg-white/5 p-4 text-sm text-white/70">Leaderboard has not been populated yet.</div> : null}
        </div>
      </SectionCard>
    </div>
  );
}