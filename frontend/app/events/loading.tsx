import { Skeleton } from "@/components/ui/skeleton";
import { SectionCard } from "@/components/section-card";

export default function EventsLoading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <SectionCard
        eyebrow="Schedule"
        title="Event listing"
        description="Each event page includes fights, community votes, ML predictions, and forum threads."
      >
        <div className="grid gap-4 lg:grid-cols-2">
          {Array.from({ length: 6 }).map((_, i) => (
            <div key={i} className="rounded-3xl border border-white/10 bg-white/5 p-5">
              <Skeleton className="h-4 w-24 mb-3" />
              <Skeleton className="h-6 w-3/4 mb-2" />
              <Skeleton className="h-4 w-1/2" />
            </div>
          ))}
        </div>
      </SectionCard>
    </div>
  );
}
