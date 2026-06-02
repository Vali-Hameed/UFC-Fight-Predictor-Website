import { Skeleton } from "@/components/ui/skeleton";
import { SectionCard } from "@/components/section-card";

export default function EventDetailLoading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <SectionCard eyebrow="Event detail" title="Loading Event..." description="Fetching event details and fights">
          <div className="space-y-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="rounded-3xl border border-white/10 bg-white/5 p-6">
                <div className="flex justify-between items-center mb-6">
                  <Skeleton className="h-6 w-32" />
                  <Skeleton className="h-4 w-12" />
                  <Skeleton className="h-6 w-32" />
                </div>
                <div className="space-y-2">
                  <Skeleton className="h-10 w-full rounded-2xl" />
                  <Skeleton className="h-10 w-full rounded-2xl" />
                </div>
              </div>
            ))}
          </div>
        </SectionCard>

        <div className="space-y-6">
          <SectionCard eyebrow="Community vs AI" title="Accuracy" description="Prediction accuracy for this event.">
            <div className="space-y-4">
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <Skeleton className="h-4 w-32 mb-3" />
                <Skeleton className="h-2 w-full rounded-full" />
              </div>
              <div className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <Skeleton className="h-4 w-24 mb-3" />
                <Skeleton className="h-2 w-full rounded-full" />
              </div>
            </div>
          </SectionCard>

          <SectionCard eyebrow="Forum" title="Event threads" description="Loading forum threads...">
            <div className="space-y-3">
              {Array.from({ length: 3 }).map((_, i) => (
                <div key={i} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                  <Skeleton className="h-3 w-16 mb-2" />
                  <Skeleton className="h-5 w-3/4 mb-2" />
                  <Skeleton className="h-3 w-24" />
                </div>
              ))}
            </div>
          </SectionCard>
        </div>
      </div>
    </div>
  );
}
