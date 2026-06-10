import { Skeleton } from "@/components/ui/skeleton";
import { SectionCard } from "@/components/section-card";

export default function EventDetailLoading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
        <SectionCard eyebrow="Event detail" title="Loading Event..." description="Fetching event details and fights">
          <div className="space-y-4">
            {Array.from({ length: 5 }).map((_, i) => (
              <div key={i} className="rounded-3xl border border-white/10 bg-white/5 p-5">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div className="flex-1">
                    <Skeleton className="h-3 w-16 mb-2" />
                    <Skeleton className="h-6 w-48 max-w-full mb-2" />
                    <Skeleton className="h-4 w-32 max-w-[80%]" />
                  </div>
                  <Skeleton className="h-8 w-24 rounded-2xl" />
                </div>
                <div className="mt-4 grid gap-3 md:grid-cols-2">
                  <Skeleton className="h-20 w-full rounded-2xl" />
                  <Skeleton className="h-20 w-full rounded-2xl" />
                </div>
                <div className="mt-5 grid gap-3 md:grid-cols-3">
                  <Skeleton className="h-12 w-full rounded-2xl" />
                  <Skeleton className="h-12 w-full rounded-2xl" />
                  <Skeleton className="h-12 w-full rounded-2xl" />
                  <div className="md:col-span-3 flex items-center gap-2 mt-2">
                    <Skeleton className="h-4 w-4 rounded" />
                    <Skeleton className="h-4 w-48 max-w-full" />
                  </div>
                  <Skeleton className="h-12 w-full rounded-2xl md:col-span-3 mt-2" />
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
