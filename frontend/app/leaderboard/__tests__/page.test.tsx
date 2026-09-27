/**
 * Tests for app/leaderboard/page.tsx (server component)
 */

import { render, screen, fireEvent } from "@testing-library/react";

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
  mockApiFetch.mockImplementation(async (url: string) => {
    if (url.includes("/filters")) {
      return { seasons: [], recentEvents: [] };
    }
    return [];
  });
});

describe("LeaderboardPage", () => {
  it("renders leaderboard rows", async () => {
    mockApiFetch.mockImplementation(async (url: string) => {
      if (url.includes("/filters")) {
        return { seasons: [], recentEvents: [] };
      }
      return [
        { id: 1, userId: 1, username: "champion", totalPoints: 500, correctPredictions: 40, totalPredictions: 50, currentStreak: 5, bestStreak: 10, lastUpdated: null },
        { id: 2, userId: 2, username: "contender", totalPoints: 350, correctPredictions: 30, totalPredictions: 50, currentStreak: 2, bestStreak: 8, lastUpdated: null },
      ];
    });

    await renderLeaderboardPage();

    expect(screen.getByText("#1")).toBeInTheDocument();
    expect(screen.getByText("@champion")).toBeInTheDocument();
    expect(screen.getByText("500 pts")).toBeInTheDocument();
    expect(screen.getByText("#2")).toBeInTheDocument();
    expect(screen.getByText("@contender")).toBeInTheDocument();
    expect(screen.getByText("350 pts")).toBeInTheDocument();
  });

  it("shows win rate percentage", async () => {
    mockApiFetch.mockImplementation(async (url: string) => {
      if (url.includes("/filters")) {
        return { seasons: [], recentEvents: [] };
      }
      return [
        { id: 1, userId: 1, username: "user1", totalPoints: 100, correctPredictions: 8, totalPredictions: 10, currentStreak: 1, bestStreak: 3, lastUpdated: null },
      ];
    });

    await renderLeaderboardPage();
    expect(screen.getByText(/80% win rate/)).toBeInTheDocument();
  });

  it("shows empty state when no data", async () => {
    mockApiFetch.mockImplementation(async (url: string) => {
      if (url.includes("/filters")) {
        return { seasons: [], recentEvents: [] };
      }
      return [];
    });

    await renderLeaderboardPage();
    expect(screen.getByText("Leaderboard has not been populated yet.")).toBeInTheDocument();
  });

  it("links to user profiles", async () => {
    mockApiFetch.mockImplementation(async (url: string) => {
      if (url.includes("/filters")) {
        return { seasons: [], recentEvents: [] };
      }
      return [
        { id: 1, userId: 1, username: "champ", totalPoints: 100, correctPredictions: 5, totalPredictions: 10, currentStreak: 0, bestStreak: 0, lastUpdated: null },
      ];
    });

    await renderLeaderboardPage();
    const link = screen.getByText("@champ");
    expect(link.closest("a")).toHaveAttribute("href", "/profile/champ");
  });

  it("paginates predictors at 10 per page and allows navigation with Prev/Next", async () => {
    const mockUsers = Array.from({ length: 15 }, (_, i) => ({
      id: i + 1,
      userId: i + 1,
      username: `user_${i + 1}`,
      totalPoints: 100 - i * 5,
      correctPredictions: 10,
      totalPredictions: 15,
      currentStreak: 2,
      bestStreak: 5,
      lastUpdated: null,
    }));

    mockApiFetch.mockImplementation(async (url: string) => {
      if (url.includes("/filters")) {
        return { seasons: [], recentEvents: [] };
      }
      return mockUsers;
    });

    await renderLeaderboardPage();

    // Page 1 should show ranks #1 to #10
    expect(screen.getByText("#1")).toBeInTheDocument();
    expect(screen.getByText("@user_1")).toBeInTheDocument();
    expect(screen.getByText("#10")).toBeInTheDocument();
    expect(screen.getByText("@user_10")).toBeInTheDocument();
    expect(screen.queryByText("@user_11")).not.toBeInTheDocument();

    // Pagination info
    expect(screen.getByText(/Page/)).toBeInTheDocument();
    expect(screen.getByText("15 predictors")).toBeInTheDocument();

    // Prev should be disabled on Page 1
    const prevButton = screen.getByRole("button", { name: "← Previous" });
    const nextButton = screen.getByRole("button", { name: "Next →" });
    expect(prevButton).toBeDisabled();
    expect(nextButton).not.toBeDisabled();

    // Click Next to navigate to Page 2
    fireEvent.click(nextButton);

    // Page 2 should show ranks #11 to #15
    expect(screen.getByText("#11")).toBeInTheDocument();
    expect(screen.getByText("@user_11")).toBeInTheDocument();
    expect(screen.getByText("#15")).toBeInTheDocument();
    expect(screen.getByText("@user_15")).toBeInTheDocument();
    expect(screen.queryByText("@user_1")).not.toBeInTheDocument();

    // On Page 2, Next should be disabled and Prev should be enabled
    expect(nextButton).toBeDisabled();
    expect(prevButton).not.toBeDisabled();

    // Click Prev to return to Page 1
    fireEvent.click(prevButton);
    expect(screen.getByText("#1")).toBeInTheDocument();
    expect(screen.getByText("@user_1")).toBeInTheDocument();
  });

  it("resets page to 1 when changing mode", async () => {
    const mockUsers = Array.from({ length: 15 }, (_, i) => ({
      id: i + 1,
      userId: i + 1,
      username: `user_${i + 1}`,
      totalPoints: 100 - i * 5,
      correctPredictions: 10,
      totalPredictions: 15,
      currentStreak: 2,
      bestStreak: 5,
      lastUpdated: null,
    }));

    mockApiFetch.mockImplementation(async (url: string) => {
      if (url.includes("/filters")) {
        return {
          seasons: [{ id: 10, name: "Season 1", active: true, championUsername: null }],
          recentEvents: [{ id: 101, name: "UFC 300" }],
        };
      }
      return mockUsers;
    });

    await renderLeaderboardPage();

    // Go to Page 2
    const nextButton = screen.getByRole("button", { name: "Next →" });
    fireEvent.click(nextButton);
    expect(screen.getByText("#11")).toBeInTheDocument();

    // Click Season tab
    const seasonTab = screen.getByRole("button", { name: "Season" });
    fireEvent.click(seasonTab);

    // Should reset to page 1 (#1 displayed)
    expect(await screen.findByText("#1")).toBeInTheDocument();
  });
});
