"use client";

import { useEffect, useState } from "react";
import { apiFetch, FightDto, MlPredictionDto, CommunityVoteDto } from "@/lib/api";
import { PredictionCard } from "@/components/prediction-card";

type FightCardData = {
  fight: FightDto;
  mlPrediction: MlPredictionDto | null;
  communityVote: CommunityVoteDto | null;
};

type LiveFightsListProps = {
  eventId: string | number;
  initialFights: FightCardData[];
  isEventStarted: boolean;
  isArchived: boolean;
};

function CollapsibleSection({ title, fightList, isEventStarted, isArchived }: { title: string, fightList: FightCardData[], isEventStarted: boolean, isArchived: boolean }) {
  const [isOpen, setIsOpen] = useState(true);

  if (fightList.length === 0) return null;

  const isMainCard = title === "Main Card";

  return (
    <div className="mb-8 rounded-3xl border border-white/10 bg-white/5 p-5 shadow-sm">
      <button 
        onClick={() => setIsOpen(!isOpen)} 
        className="flex items-center justify-between gap-2 text-2xl font-black tracking-tight mb-4 w-full text-left text-white transition-colors hover:text-red-600"
      >
        <span>{title}</span>
        <svg 
          xmlns="http://www.w3.org/2000/svg" 
          width="24" height="24" 
          viewBox="0 0 24 24" fill="none" stroke="currentColor" 
          strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" 
          className={`transition-transform duration-300 ${isOpen ? 'rotate-180' : ''} text-white/50`}
        >
          <path d="m6 9 6 6 6-6"/>
        </svg>
      </button>
      
      {isOpen && (
        <div className="space-y-4">
          {fightList.map(({ fight, mlPrediction, communityVote }) => (
            <PredictionCard 
              key={fight.id} 
              fight={fight} 
              mlPrediction={mlPrediction} 
              communityVote={communityVote} 
              isEventStarted={isEventStarted} 
              isArchived={isArchived} 
            />
          ))}
        </div>
      )}
    </div>
  );
}

export function LiveFightsList({ eventId, initialFights, isEventStarted, isArchived }: LiveFightsListProps) {
  const [fights, setFights] = useState<FightCardData[]>(initialFights);

  useEffect(() => {
    // Only poll if the event has started, is not archived, and there are fights that are not completed
    const hasIncompleteFights = fights.some(f => f.fight.status !== "COMPLETED" && f.fight.status !== "CANCELED");
    
    if (!isEventStarted || isArchived || !hasIncompleteFights) {
      return;
    }

    const interval = setInterval(async () => {
      try {
        const updatedFights = await apiFetch<FightDto[]>(`/api/v1/events/${eventId}/fights`);
        if (updatedFights && updatedFights.length > 0) {
          setFights(current => 
            current.map(card => {
              const updatedFight = updatedFights.find(f => f.id === card.fight.id);
              return updatedFight ? { ...card, fight: updatedFight } : card;
            })
          );
        }
      } catch (err) {
        // ignore polling errors
      }
    }, 30000); // 30 seconds

    return () => clearInterval(interval);
  }, [eventId, isEventStarted, isArchived, fights]);

  const mainCardFights = fights.filter(f => f.fight.status !== "CANCELED" && f.fight.cardTier === "Main Card");
  const prelimFights = fights.filter(f => f.fight.status !== "CANCELED" && f.fight.cardTier === "Prelims");
  const earlyPrelimFights = fights.filter(f => f.fight.status !== "CANCELED" && f.fight.cardTier === "Early Prelims");
  const uncategorizedFights = fights.filter(f => f.fight.status !== "CANCELED" && !f.fight.cardTier);
  const canceledFights = fights.filter(f => f.fight.status === "CANCELED");

  return (
    <div className="space-y-6">
      <CollapsibleSection title="Main Card" fightList={mainCardFights} isEventStarted={isEventStarted} isArchived={isArchived} />
      <CollapsibleSection title="Prelims" fightList={prelimFights} isEventStarted={isEventStarted} isArchived={isArchived} />
      <CollapsibleSection title="Early Prelims" fightList={earlyPrelimFights} isEventStarted={isEventStarted} isArchived={isArchived} />
      <CollapsibleSection title="Other Fights" fightList={uncategorizedFights} isEventStarted={isEventStarted} isArchived={isArchived} />
      <CollapsibleSection title="Canceled Fights" fightList={canceledFights} isEventStarted={isEventStarted} isArchived={isArchived} />
    </div>
  );
}
