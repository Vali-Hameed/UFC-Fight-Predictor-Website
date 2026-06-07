"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { SectionCard } from "@/components/section-card";
import { EventDto, FightDto, MlPredictionDto, getEventDisplayStatus, apiFetch } from "@/lib/api";

type EventWithPrediction = EventDto & {
  mainPrediction: MlPredictionDto | null;
  displayStatus: string;
};

export function ArchivedEventsDropdown({ archivedEvents }: { archivedEvents: EventDto[] }) {
  const [isOpen, setIsOpen] = useState(false);
  const [eventsData, setEventsData] = useState<EventWithPrediction[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    if (isOpen && eventsData.length === 0) {
      setLoading(true);
      
      const fetchPredictions = async () => {
        const enriched = await Promise.all(
          archivedEvents.map(async (event) => {
            let mainPrediction: MlPredictionDto | null = null;
            let mainFightStatus: string | null = null;
            try {
              const fights = await apiFetch<FightDto[]>(`/api/v1/events/${event.id}/fights`);
              const mainFight = fights.find((f) => f.isMainEvent) || fights[0];
              if (mainFight) {
                mainFightStatus = mainFight.status;
                mainPrediction = await apiFetch<MlPredictionDto>(`/api/v1/ml/fight/${mainFight.id}`).catch(() => null);
              }
            } catch {
              // Ignore
            }
            const displayStatus = getEventDisplayStatus(event, mainFightStatus);
            return { ...event, mainPrediction, displayStatus };
          })
        );
        setEventsData(enriched);
        setLoading(false);
      };

      fetchPredictions();
    }
  }, [isOpen, archivedEvents, eventsData.length]);

  if (archivedEvents.length === 0) return null;

  return (
    <SectionCard
      eyebrow="Archive"
      title="Past events"
      description="View fight results and prediction statistics for older UFC events. Predictions and discussions are closed for archived events."
    >
      <button 
        onClick={() => setIsOpen(!isOpen)}
        className="mb-4 flex items-center justify-between w-full rounded-xl border border-white/10 bg-white/5 px-4 py-3 text-sm font-medium text-white transition hover:bg-white/10"
      >
        <span>{isOpen ? "Hide Archived Events" : "Show Archived Events"}</span>
        <svg 
          className={`h-5 w-5 transform transition-transform ${isOpen ? "rotate-180" : ""}`} 
          fill="none" 
          viewBox="0 0 24 24" 
          stroke="currentColor"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {isOpen && (
        <div className="grid gap-4 lg:grid-cols-2 mt-4">
          {loading ? (
            <div className="col-span-2 text-center text-white/50 py-8">Loading archived events...</div>
          ) : (
            eventsData.map((event) => (
              <Link key={event.id} href={`/events/${event.id}?archived=true`} className="rounded-3xl border border-white/10 bg-white/5 p-5 transition hover:-translate-y-0.5 hover:bg-white/8 opacity-80 hover:opacity-100">
                <p className="text-xs uppercase tracking-[0.3em] text-white/45">{event.displayStatus}</p>
                <h3 className="mt-3 text-xl font-semibold text-white">{event.name}</h3>
                <p className="mt-2 text-sm text-white/58">{event.location}</p>
                {event.mainPrediction ? (
                  <div className="mt-4 inline-block rounded-xl border border-white/10 bg-white/5 px-3 py-1.5 text-xs font-semibold text-white/70">
                    Main Event ML: {event.mainPrediction.predictedWinner} • {Math.round((event.mainPrediction.confidenceScore ?? 0) * 100)}%
                  </div>
                ) : null}
              </Link>
            ))
          )}
        </div>
      )}
    </SectionCard>
  );
}
