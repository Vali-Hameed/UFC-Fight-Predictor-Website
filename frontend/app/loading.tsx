import { Skeleton } from "@/components/ui/skeleton";

export default function Loading() {
  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <section className="grid gap-6 rounded-[2rem] border border-white/10 bg-gradient-to-br from-panel via-bg to-panelSoft p-8 shadow-2xl shadow-black/30 lg:grid-cols-[1.3fr_0.7fr] lg:p-12">
        <div className="space-y-6">
          <Skeleton className="h-6 w-64 rounded-full" />
          <div className="space-y-4">
            <Skeleton className="h-14 w-full max-w-2xl" />
            <Skeleton className="h-14 w-3/4" />
            <Skeleton className="h-6 w-full max-w-lg mt-4" />
          </div>
          <div className="flex flex-wrap gap-3">
            <Skeleton className="h-12 w-32 rounded-full" />
            <Skeleton className="h-12 w-40 rounded-full" />
          </div>
        </div>
        <div className="grid gap-4">
          <Skeleton className="h-32 w-full rounded-3xl" />
          <div className="grid grid-cols-2 gap-4">
            <Skeleton className="h-28 w-full rounded-3xl" />
            <Skeleton className="h-28 w-full rounded-3xl" />
          </div>
        </div>
      </section>

      <div className="mt-8 grid gap-6 lg:grid-cols-2">
        <div className="space-y-4">
          <Skeleton className="h-8 w-40 mb-2" />
          <Skeleton className="h-20 w-full rounded-2xl" />
          <Skeleton className="h-20 w-full rounded-2xl" />
          <Skeleton className="h-20 w-full rounded-2xl" />
        </div>
        <div className="space-y-4">
          <Skeleton className="h-8 w-48 mb-2" />
          <Skeleton className="h-16 w-full rounded-2xl" />
          <Skeleton className="h-16 w-full rounded-2xl" />
          <Skeleton className="h-16 w-full rounded-2xl" />
          <Skeleton className="h-16 w-full rounded-2xl" />
        </div>
      </div>
    </div>
  );
}
