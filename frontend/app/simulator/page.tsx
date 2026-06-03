"use client";

import { useState } from "react";
import { apiFetch, ApiResponseError } from "@/lib/api";
import fightersDataJson from "./fighters.json";

// The imported JSON has shape: { Active: { 'Welterweight': ['A', 'B'], ... }, Inactive: { ... } }
const fightersData = fightersDataJson as Record<string, Record<string, string[]>>;

const WEIGHT_CLASS_ORDER = [
  "Flyweight",
  "Bantamweight",
  "Featherweight",
  "Lightweight",
  "Welterweight",
  "Middleweight",
  "Light Heavyweight",
  "Heavyweight",
  "Women's Strawweight",
  "Women's Flyweight",
  "Women's Bantamweight",
  "Women's Featherweight",
  "Catch Weight",
  "Open Weight"
];

const getWeightClassRank = (wc: string) => {
  const index = WEIGHT_CLASS_ORDER.indexOf(wc);
  return index === -1 ? 999 : index;
};

function FighterSelector({ 
  side, 
  value, 
  onChange 
}: { 
  side: string; 
  value: string; 
  onChange: (v: string) => void;
}) {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <details 
      className="group rounded-2xl border border-white/10 bg-white/5 [&_summary::-webkit-details-marker]:hidden"
      open={isOpen}
      onClick={(e) => {
        // Only toggle if they click the summary
        if ((e.target as HTMLElement).closest('summary')?.parentElement === e.currentTarget) {
          e.preventDefault();
          setIsOpen(!isOpen);
        }
      }}
    >
      <summary className="flex cursor-pointer items-center justify-between p-4 outline-none">
        <div className="text-left">
          <p className="text-xs uppercase tracking-[0.2em] text-white/50">{side}</p>
          <p className="mt-1 text-lg font-bold text-white">{value || 'Select Fighter'}</p>
        </div>
        <div className={`text-white/50 transition-transform ${isOpen ? 'rotate-180' : ''}`}>
          <svg xmlns="http://www.apache.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m6 9 6 6 6-6"/></svg>
        </div>
      </summary>
      
      <div className="p-4 pt-0 space-y-3 max-h-[500px] overflow-y-auto">
        {['Active', 'Inactive'].map((status) => (
          <details key={status} className="group/status rounded-xl border border-white/10 bg-black/20 [&_summary::-webkit-details-marker]:hidden">
            <summary className="flex cursor-pointer items-center justify-between p-3 outline-none hover:bg-white/5">
              <span className="font-semibold text-white/90">{status} Fighters</span>
              <div className="text-white/30 transition-transform group-open/status:rotate-180">
                <svg xmlns="http://www.apache.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m6 9 6 6 6-6"/></svg>
              </div>
            </summary>
            
            <div className="p-2 space-y-2">
              {Object.keys(fightersData[status] || {})
                .sort((a, b) => getWeightClassRank(a) - getWeightClassRank(b))
                .map((weightClass) => (
                <details key={weightClass} className="group/wc rounded-lg border border-white/5 bg-white/5 [&_summary::-webkit-details-marker]:hidden">
                  <summary className="flex cursor-pointer items-center justify-between p-2 text-sm text-white/70 outline-none hover:text-white">
                    <span>{weightClass || "Catch Weight"}</span>
                    <div className="text-white/30 transition-transform group-open/wc:rotate-180">
                      <svg xmlns="http://www.apache.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="m6 9 6 6 6-6"/></svg>
                    </div>
                  </summary>
                  
                  <div className="grid grid-cols-1 gap-1 p-2 bg-black/40 rounded-b-lg max-h-[250px] overflow-y-auto">
                    {fightersData[status][weightClass].map((fighter) => (
                      <button 
                        key={fighter}
                        onClick={(e) => {
                          e.preventDefault();
                          onChange(fighter);
                          setIsOpen(false);
                        }}
                        className={`text-left px-3 py-2 text-sm rounded-md transition ${value === fighter ? 'bg-accent text-white font-bold' : 'text-white/80 hover:bg-white/10 hover:text-white'}`}
                      >
                        {fighter}
                      </button>
                    ))}
                  </div>
                </details>
              ))}
            </div>
          </details>
        ))}
      </div>
    </details>
  );
}

export default function SimulatorPage() {
  const [fighter1, setFighter1] = useState("");
  const [fighter2, setFighter2] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const [result, setResult] = useState<{
    predictedWinner: string;
    confidenceScore: number;
    fighter1: string;
    fighter2: string;
  } | null>(null);

  const handlePredict = async () => {
    if (!fighter1 || !fighter2) {
      setError("Please select both fighters.");
      return;
    }
    if (fighter1 === fighter2) {
      setError("Fighters must be different.");
      return;
    }

    setLoading(true);
    setError(null);
    setResult(null);

    try {
      const data = await apiFetch<any>(`/api/v1/ml/predict?fighter1=${encodeURIComponent(fighter1)}&fighter2=${encodeURIComponent(fighter2)}`);
      
      setResult({
        predictedWinner: data.predicted_winner,
        confidenceScore: data.confidence_score,
        fighter1,
        fighter2
      });
    } catch (err: any) {
      if (err instanceof ApiResponseError && err.status === 503) {
        setError(err.message || "Our prediction model is currently unavailable. Please try again later.");
      } else {
        setError("An error occurred while fetching the prediction. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="mx-auto max-w-7xl px-4 py-10 sm:px-6 lg:px-8">
      <div className="mb-10 text-center">
        <h1 className="text-4xl font-semibold leading-tight text-white sm:text-5xl">
          Hypothetical Fight Simulator
        </h1>
        <p className="mt-4 text-lg text-white/60">
          Pit any two fighters against each other and let our ML model predict the likely winner.
        </p>
      </div>

      <div className="relative overflow-hidden rounded-[2.5rem] border border-white/10 bg-gradient-to-b from-panel to-bg shadow-2xl">
        <div className="absolute inset-0 bg-[radial-gradient(circle_at_50%_0%,rgba(201,168,76,0.1),transparent_50%)] pointer-events-none" />
        
        <div className="relative flex flex-col lg:flex-row items-start justify-between p-8 lg:p-16 gap-10">
          
          <div className="w-full flex-1">
            <FighterSelector 
              side="Fighter 1 (Red Corner)" 
              value={fighter1} 
              onChange={setFighter1} 
            />
          </div>

          <div className="w-full flex-shrink-0 lg:w-[400px] flex flex-col items-center justify-center pt-2">
            <div className="min-h-[220px] w-full rounded-3xl border border-white/10 bg-black/40 p-6 flex flex-col items-center justify-center backdrop-blur-md shadow-inner transition-all duration-500">
              {loading ? (
                <div className="flex flex-col items-center animate-pulse">
                  <div className="h-10 w-10 animate-spin rounded-full border-4 border-accent border-t-transparent" />
                  <p className="mt-4 text-sm font-semibold tracking-widest text-accent uppercase">Analyzing Data...</p>
                </div>
              ) : error ? (
                <div className="text-center text-red-400">
                  <p className="text-sm font-semibold">{error}</p>
                </div>
              ) : result ? (
                <div className="text-center animate-in fade-in zoom-in duration-500">
                  <p className="text-xs font-semibold uppercase tracking-[0.2em] text-white/50">Predicted Winner</p>
                  <p className="mt-2 text-3xl font-bold text-gold drop-shadow-[0_0_15px_rgba(201,168,76,0.5)]">
                    {result.predictedWinner}
                  </p>
                  <div className="mt-4 inline-flex items-center rounded-full bg-white/10 px-4 py-1.5 text-sm font-semibold text-white/80">
                    Confidence: {(result.confidenceScore * 100).toFixed(1)}%
                  </div>
                </div>
              ) : (
                <div className="text-center text-white/30">
                  <p className="text-sm">Select two fighters and run the simulation</p>
                </div>
              )}
            </div>

            <button
              onClick={handlePredict}
              disabled={loading || !fighter1 || !fighter2}
              className="group relative mt-6 inline-flex w-full items-center justify-center overflow-hidden rounded-full bg-accent px-8 py-4 font-bold text-white shadow-[0_0_30px_rgba(210,10,10,0.3)] transition-all hover:scale-[1.02] hover:shadow-[0_0_40px_rgba(210,10,10,0.5)] disabled:opacity-50 disabled:hover:scale-100 disabled:hover:shadow-[0_0_30px_rgba(210,10,10,0.3)]"
            >
              <div className="absolute inset-0 bg-white/20 translate-y-full transition-transform group-hover:translate-y-0" />
              <span className="relative">Run Simulation</span>
            </button>
          </div>

          <div className="w-full flex-1">
            <FighterSelector 
              side="Fighter 2 (Blue Corner)" 
              value={fighter2} 
              onChange={setFighter2} 
            />
          </div>

        </div>
      </div>
    </div>
  );
}
