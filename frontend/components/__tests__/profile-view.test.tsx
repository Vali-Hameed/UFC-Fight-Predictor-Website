import { render, screen, act, fireEvent } from "@testing-library/react";
import { ProfileView } from "@/components/profile-view";
import { apiFetch, type ProfileDto } from "@/lib/api";

jest.mock("@/lib/session", () => ({
  useAuth: () => ({ user: { username: "john" }, token: "token123" }),
}));

jest.mock("@/lib/api", () => ({
  apiFetch: jest.fn(),
}));

jest.mock("@/components/profile-editor", () => ({
  ProfileEditor: ({ username }: { username: string }) => (
    <div data-testid="profile-editor">Editor for {username}</div>
  ),
}));

beforeEach(() => {
  jest.clearAllMocks();
  (apiFetch as jest.Mock).mockResolvedValue(null);
});

const publicProfile: ProfileDto = {
  id: 1,
  username: "john",
  firstName: "John",
  lastName: "Doe",
  profileImageUrl: null,
  role: "ROLE_USER",
  enabled: true,
  publicProfile: true,
  leaderboardStats: {
    rank: 3,
    totalPoints: 450,
    winRate: 0.72,
  },
  badges: [
    {
      id: 101,
      badgeType: "EVENT_WINNER",
      badgeLabel: "Event Winner",
      awardedAt: "2026-01-10T00:00:00Z",
    },
    {
      id: 102,
      badgeType: "EVENT_WINNER",
      badgeLabel: "Event Winner",
      awardedAt: "2026-02-15T00:00:00Z",
    },
    {
      id: 103,
      badgeType: "SEASON_CHAMPION",
      badgeLabel: "Season Champion",
      awardedAt: "2026-03-01T00:00:00Z",
    },
  ],
  predictionHistory: [
    {
      fightId: 1,
      fighter1Name: "Fighter A",
      fighter2Name: "Fighter B",
      eventId: 10,
      eventName: "UFC 300",
      predictedWinner: "Fighter A",
      predictedMethod: "KO/TKO",
      predictedRound: 2,
      resultWinner: "Fighter A",
      resultMethod: "KO/TKO",
      resultRound: 2,
      submittedAt: "2026-01-01T00:00:00Z",
      locked: true,
      pointsAwarded: 10,
      isWinnerCorrect: true,
    },
    {
      fightId: 2,
      fighter1Name: "Fighter C",
      fighter2Name: "Fighter D",
      eventId: 10,
      eventName: "UFC 300",
      predictedWinner: "Fighter D",
      predictedMethod: "Decision",
      predictedRound: 0,
      resultWinner: "Fighter C",
      submittedAt: "2026-01-01T00:00:00Z",
      locked: true,
      pointsAwarded: 0,
      isWinnerCorrect: false,
    },
  ],
};

describe("ProfileView", () => {
  it("shows error when profile is null", async () => {
    await act(async () => {
      render(<ProfileView initialProfile={null} username="john" />);
    });
    expect(screen.getByText("Profile data could not be loaded.")).toBeInTheDocument();
  });

  it("shows private message when no leaderboard stats", async () => {
    const privateProfile: ProfileDto = {
      ...publicProfile,
      leaderboardStats: undefined,
      predictionHistory: undefined,
    };
    await act(async () => {
      render(<ProfileView initialProfile={privateProfile} username="other" />);
    });
    expect(screen.getByText("This profile is private.")).toBeInTheDocument();
  });

  it("displays rank, total points, win rate, and total picks", async () => {
    await act(async () => {
      render(<ProfileView initialProfile={publicProfile} username="john" />);
    });
    expect(screen.getByText("#3")).toBeInTheDocument();
    expect(screen.getByText("450")).toBeInTheDocument();
    expect(screen.getByText("72%")).toBeInTheDocument();
    expect(screen.getByText("2")).toBeInTheDocument(); // 2 Total Picks
  });

  it("shows Unranked when rank is null", async () => {
    const unrankedProfile = {
      ...publicProfile,
      leaderboardStats: { rank: null, totalPoints: 0, winRate: 0 },
    };
    await act(async () => {
      render(<ProfileView initialProfile={unrankedProfile} username="john" />);
    });
    expect(screen.getByText("Unranked")).toBeInTheDocument();
  });

  it("groups and displays prediction history by event", async () => {
    await act(async () => {
      render(<ProfileView initialProfile={publicProfile} username="john" />);
    });
    expect(screen.getByText("UFC 300")).toBeInTheDocument();
    expect(screen.getByText(/2 predictions/)).toBeInTheDocument();
    expect(screen.getByText(/50% accuracy/)).toBeInTheDocument();
  });

  it("shows empty prediction history message", async () => {
    const emptyHistory = {
      ...publicProfile,
      predictionHistory: [],
    };
    await act(async () => {
      render(<ProfileView initialProfile={emptyHistory} username="john" />);
    });
    expect(screen.getByText("No prediction history found.")).toBeInTheDocument();
  });

  it("shows ProfileEditor when viewing own profile", async () => {
    await act(async () => {
      render(<ProfileView initialProfile={publicProfile} username="john" />);
    });
    expect(screen.getByTestId("profile-editor")).toBeInTheDocument();
  });

  it("hides ProfileEditor for other users", async () => {
    await act(async () => {
      render(<ProfileView initialProfile={publicProfile} username="other" />);
    });
    expect(screen.queryByTestId("profile-editor")).not.toBeInTheDocument();
  });

  it("switches tabs between predictions, trophies, and settings", async () => {
    await act(async () => {
      render(<ProfileView initialProfile={publicProfile} username="john" />);
    });

    // Predictions tab is active by default
    const predictionsPanel = screen.getByText("UFC 300").closest("[data-tab='predictions']");
    expect(predictionsPanel).not.toHaveClass("hidden");

    // Click Trophy Case tab
    const trophyTabBtn = screen.getByRole("button", { name: /Trophy Case/i });
    await act(async () => {
      fireEvent.click(trophyTabBtn);
    });

    expect(screen.getByText(/Trophy Showcase/i)).toBeInTheDocument();
    expect(predictionsPanel).toHaveClass("hidden");

    // Click Account Settings tab
    const settingsTabBtn = screen.getByRole("button", { name: /Account Settings/i });
    await act(async () => {
      fireEvent.click(settingsTabBtn);
    });

    const settingsPanel = screen.getByTestId("profile-editor").closest("[data-tab='settings']");
    expect(settingsPanel).not.toHaveClass("hidden");
  });

  it("clicking the header trophy pill switches to Trophy Case tab", async () => {
    await act(async () => {
      render(<ProfileView initialProfile={publicProfile} username="john" />);
    });

    const trophyPill = screen.getByRole("button", { name: /View trophies/i });
    await act(async () => {
      fireEvent.click(trophyPill);
    });

    expect(screen.getByText(/Trophy Showcase/i)).toBeInTheDocument();
  });

  it("groups duplicate badges and displays multiplier counters", async () => {
    await act(async () => {
      render(<ProfileView initialProfile={publicProfile} username="john" />);
    });

    // Switch to trophy case
    const trophyTabBtn = screen.getByRole("button", { name: /Trophy Case/i });
    await act(async () => {
      fireEvent.click(trophyTabBtn);
    });

    // 2x EVENT_WINNER should display multiplier "×2"
    expect(screen.getByText("×2")).toBeInTheDocument();
    expect(screen.getByText("Season Champion")).toBeInTheDocument();
  });

  it("filters trophies by category dropdown", async () => {
    await act(async () => {
      render(<ProfileView initialProfile={publicProfile} username="john" />);
    });

    // Switch to trophy tab
    const trophyTabBtn = screen.getByRole("button", { name: /Trophy Case/i });
    await act(async () => {
      fireEvent.click(trophyTabBtn);
    });

    const categorySelect = screen.getByLabelText(/Category:/i);

    // Filter to Championships only
    await act(async () => {
      fireEvent.change(categorySelect, { target: { value: "championship" } });
    });

    expect(screen.getByText("Season Champion")).toBeInTheDocument();
    expect(screen.queryByText("Event Winner")).not.toBeInTheDocument();

    // Filter to Streaks (user has 0 streaks)
    await act(async () => {
      fireEvent.change(categorySelect, { target: { value: "streak" } });
    });

    expect(screen.getByText("No Trophies in this Category")).toBeInTheDocument();
  });

  it("filters predictions by search query and outcome", async () => {
    await act(async () => {
      render(<ProfileView initialProfile={publicProfile} username="john" />);
    });

    const searchInput = screen.getByPlaceholderText(/Search events/i);

    // Search for non-matching event
    await act(async () => {
      fireEvent.change(searchInput, { target: { value: "UFC 299" } });
    });

    expect(screen.getByText("No predictions match your search or filter.")).toBeInTheDocument();

    // Reset via clear button or search reset
    const resetBtn = screen.getByRole("button", { name: /Reset Filters/i });
    await act(async () => {
      fireEvent.click(resetBtn);
    });

    expect(screen.getByText("UFC 300")).toBeInTheDocument();

    // Filter by outcome
    const outcomeSelect = screen.getByLabelText(/Outcome:/i);
    await act(async () => {
      fireEvent.change(outcomeSelect, { target: { value: "correct" } });
    });

    expect(screen.getByText("Fighter A vs Fighter B")).toBeInTheDocument();
    expect(screen.queryByText("Fighter C vs Fighter D")).not.toBeInTheDocument();
  });

  it("paginates prediction events when there are more than 5 events", async () => {
    // Generate 7 events
    const manyEvents = Array.from({ length: 7 }, (_, i) => ({
      fightId: i + 1,
      fighter1Name: `Fighter ${i}A`,
      fighter2Name: `Fighter ${i}B`,
      eventId: i + 1,
      eventName: `UFC Event ${i + 1}`,
      predictedWinner: `Fighter ${i}A`,
      predictedMethod: "Decision",
      predictedRound: 0,
      submittedAt: "2026-01-01T00:00:00Z",
      locked: true,
      resultWinner: `Fighter ${i}A`,
      pointsAwarded: 10,
      isWinnerCorrect: true,
    }));

    const profileWithManyEvents: ProfileDto = {
      ...publicProfile,
      predictionHistory: manyEvents,
    };

    await act(async () => {
      render(<ProfileView initialProfile={profileWithManyEvents} username="john" />);
    });

    // Page 1 should show Event 1 through 5
    expect(screen.getByText("UFC Event 1")).toBeInTheDocument();
    expect(screen.getByText("UFC Event 5")).toBeInTheDocument();
    expect(screen.queryByText("UFC Event 6")).not.toBeInTheDocument();

    // Next button should be enabled, Previous should be disabled
    const prevBtn = screen.getByRole("button", { name: /← Previous/i });
    const nextBtn = screen.getByRole("button", { name: /Next →/i });
    expect(prevBtn).toBeDisabled();
    expect(nextBtn).toBeEnabled();

    // Go to Page 2
    await act(async () => {
      fireEvent.click(nextBtn);
    });

    expect(screen.queryByText("UFC Event 1")).not.toBeInTheDocument();
    expect(screen.getByText("UFC Event 6")).toBeInTheDocument();
    expect(screen.getByText("UFC Event 7")).toBeInTheDocument();
    expect(prevBtn).toBeEnabled();
    expect(nextBtn).toBeDisabled();
  });
});
