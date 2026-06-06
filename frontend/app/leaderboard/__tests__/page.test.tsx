/**
 * Tests for app/leaderboard/page.tsx (server component)
 */

import { render, screen } from "@testing-library/react";

const mockApiFetch = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
}));

async function renderLeaderboardPage() {
  const LeaderboardPage = (await import("@/app/leaderboard/page")).default;
  const jsx = await LeaderboardPage();
  return render(jsx);
}

beforeEach(() => {
  jest.clearAllMocks();
});

describe("LeaderboardPage", () => {
  it("renders leaderboard rows", async () => {
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, userId: 1, username: "champion", totalPoints: 500, correctPredictions: 40, totalPredictions: 50, currentStreak: 5, bestStreak: 10, lastUpdated: null },
      { id: 2, userId: 2, username: "contender", totalPoints: 350, correctPredictions: 30, totalPredictions: 50, currentStreak: 2, bestStreak: 8, lastUpdated: null },
    ]);

    await renderLeaderboardPage();

    expect(screen.getByText("#1")).toBeInTheDocument();
    expect(screen.getByText("@champion")).toBeInTheDocument();
    expect(screen.getByText("500 pts")).toBeInTheDocument();
    expect(screen.getByText("#2")).toBeInTheDocument();
    expect(screen.getByText("@contender")).toBeInTheDocument();
    expect(screen.getByText("350 pts")).toBeInTheDocument();
  });

  it("shows win rate percentage", async () => {
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, userId: 1, username: "user1", totalPoints: 100, correctPredictions: 8, totalPredictions: 10, currentStreak: 1, bestStreak: 3, lastUpdated: null },
    ]);

    await renderLeaderboardPage();
    expect(screen.getByText(/80% win rate/)).toBeInTheDocument();
  });

  it("shows empty state when no data", async () => {
    mockApiFetch.mockResolvedValueOnce([]);

    await renderLeaderboardPage();
    expect(screen.getByText("Leaderboard has not been populated yet.")).toBeInTheDocument();
  });

  it("links to user profiles", async () => {
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, userId: 1, username: "champ", totalPoints: 100, correctPredictions: 5, totalPredictions: 10, currentStreak: 0, bestStreak: 0, lastUpdated: null },
    ]);

    await renderLeaderboardPage();
    const link = screen.getByText("@champ");
    expect(link.closest("a")).toHaveAttribute("href", "/profile/champ");
  });
});
