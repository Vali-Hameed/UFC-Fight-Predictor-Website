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

  return (
    <div className="space-y-4">
      {fights.map(({ fight, mlPrediction, communityVote }) => (
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
  );
}
