export const dynamic = "force-dynamic";
import type { Metadata } from "next";
import Link from "next/link";

export const metadata: Metadata = {
  title: "Global Leaderboard | FightPicks",
  description: "View the global rankings of the best fight predictors. Track prediction accuracy, total points, and win streaks.",
  openGraph: {
    title: "Global Leaderboard | FightPicks",
    description: "View the global rankings of the best fight predictors.",
  },
};
import { SectionCard } from "@/components/section-card";
import { apiFetch, LeaderboardDto } from "@/lib/api";

export default async function LeaderboardPage() {
  const leaderboard = await apiFetch<LeaderboardDto[]>("/api/v1/leaderboard").catch(() => []);

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Ranks" title="Global leaderboard" description="See who holds the crown. Rankings are based on total prediction points, accuracy, and current win streaks.">
        <div className="space-y-3">
          {leaderboard.map((row, index) => (
            <div key={row.userId} className="grid grid-cols-[auto_1fr_auto] items-center gap-4 rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
              <div className="text-lg font-semibold text-gold">#{index + 1}</div>
              <div>
                <Link href={`/profile/${row.username ?? row.userId}`} className="font-semibold text-white hover:underline">@{row.username ?? `User #${row.userId}`}</Link>
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

      <div className="mt-10">
        <SectionCard eyebrow="Rules" title="How scoring works" description="Points are awarded based on the accuracy of your predictions. But beware, predicting exact rounds or methods carries a high risk!">
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
            <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
              <h4 className="text-lg font-semibold text-white">Winner</h4>
              <p className="mt-2 text-sm text-white/70">Correctly predict the winner to earn a base of <strong className="text-gold">10 points</strong>.</p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
              <h4 className="text-lg font-semibold text-white">Method</h4>
              <p className="mt-2 text-sm text-white/70">Correctly predict the winning method for a <strong className="text-gold">+4 point</strong> bonus.</p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
              <h4 className="text-lg font-semibold text-white">Round</h4>
              <p className="mt-2 text-sm text-white/70">Correctly predict the exact round for a <strong className="text-gold">+7 point</strong> bonus.</p>
            </div>
            <div className="rounded-2xl border border-gold/30 bg-gold/5 p-5">
              <h4 className="text-lg font-semibold text-gold">Perfect Pick</h4>
              <p className="mt-2 text-sm text-white/70">Nail the winner, method, and round perfectly to earn a <strong className="text-gold">+10 point</strong> bonus (Total: <strong className="text-gold">31 points</strong>).</p>
            </div>
            <div className="rounded-2xl border border-white/10 bg-white/5 p-5">
              <h4 className="text-lg font-semibold text-white">Draw / No Contest</h4>
              <p className="mt-2 text-sm text-white/70">Correctly predicting a rare Draw or No Contest yields a massive <strong className="text-gold">20 points</strong>.</p>
            </div>
            <div className="rounded-2xl border border-red-500/30 bg-red-500/5 p-5">
              <h4 className="text-lg font-semibold text-red-400">High Risk, High Reward</h4>
              <p className="mt-2 text-sm text-white/70">If you predict the method or round, you <strong className="text-white">must</strong> get them right. If your winner is correct but your method or round guess is wrong, you get <strong className="text-red-400">0 points</strong>!</p>
            </div>
          </div>
        </SectionCard>
      </div>
    </div>
  );
}