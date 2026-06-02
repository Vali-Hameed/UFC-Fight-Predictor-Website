import { Skeleton } from "@/components/ui/skeleton";
import { SectionCard } from "@/components/section-card";

export default function LeaderboardLoading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard eyebrow="Rankings" title="Loading Leaderboard..." description="Fetching the top predictors">
        <div className="space-y-3">
          {Array.from({ length: 10 }).map((_, i) => (
            <div key={i} className="flex items-center justify-between rounded-2xl border border-white/10 bg-white/5 px-4 py-3">
              <div>
                <Skeleton className="h-5 w-32 mb-2" />
                <Skeleton className="h-4 w-40" />
              </div>
              <div className="text-right">
                <Skeleton className="h-5 w-16 mb-2 ml-auto" />
                <Skeleton className="h-4 w-20 ml-auto" />
              </div>
            </div>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}
