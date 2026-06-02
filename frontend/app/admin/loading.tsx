import { Skeleton } from "@/components/ui/skeleton";
import { SectionCard } from "@/components/section-card";

export default function AdminLoading() {
  return (
    <div className="mx-auto max-w-6xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="space-y-6">
        <SectionCard eyebrow="Admin" title="Operations panel" description="Manage events, fights, scrape runs, predictions, and moderation.">
          <div className="mb-6 flex flex-wrap gap-4">
            <Skeleton className="h-12 w-40 rounded-2xl" />
            <Skeleton className="h-12 w-40 rounded-2xl" />
          </div>
          <div className="space-y-3">
            {Array.from({ length: 3 }).map((_, i) => (
              <div key={i} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <Skeleton className="h-5 w-24 mb-2" />
                <Skeleton className="h-4 w-48" />
              </div>
            ))}
          </div>
        </SectionCard>

        <SectionCard eyebrow="Users" title="Moderation tools" description="Inspect accounts, adjust roles, and lock or unlock users.">
          <div className="space-y-3">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="rounded-2xl border border-white/10 bg-white/5 p-4">
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <Skeleton className="h-3 w-16 mb-2" />
                    <Skeleton className="h-6 w-32 mb-2" />
                    <Skeleton className="h-4 w-48 mb-2" />
                    <Skeleton className="h-3 w-24" />
                  </div>
                  <Skeleton className="h-10 w-20 rounded-2xl" />
                </div>
                <div className="mt-4 grid gap-3 md:grid-cols-[1fr_auto] md:items-end">
                  <div className="space-y-2">
                    <Skeleton className="h-4 w-12" />
                    <Skeleton className="h-12 w-full rounded-2xl" />
                  </div>
                  <Skeleton className="h-12 w-28 rounded-2xl" />
                </div>
              </div>
            ))}
          </div>
        </SectionCard>
      </div>
    </div>
  );
}
