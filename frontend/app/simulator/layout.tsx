import type { Metadata } from "next";

export const metadata: Metadata = {
  title: "Fight Simulator | FightPicks",
  description: "Simulate hypothetical fights across any weight class using our advanced machine learning models.",
  openGraph: {
    title: "Fight Simulator | FightPicks",
    description: "Simulate hypothetical fights and get AI predictions.",
  },
};

export default function SimulatorLayout({ children }: { children: React.ReactNode }) {
  return <>{children}</>;
}
