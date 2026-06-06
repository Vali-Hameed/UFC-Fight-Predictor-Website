import { render, screen, act } from "@testing-library/react";
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

  it("displays rank, total points, and win rate", async () => {
    await act(async () => {
      render(<ProfileView initialProfile={publicProfile} username="john" />);
    });
    expect(screen.getByText("#3")).toBeInTheDocument();
    expect(screen.getByText("450")).toBeInTheDocument();
    expect(screen.getByText("72%")).toBeInTheDocument();
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
});
