import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { PredictionCard } from "@/components/prediction-card";
import type { FightDto, MlPredictionDto, CommunityVoteDto } from "@/lib/api";
import { toast } from "sonner";

// Mock session and api
const mockToken = { current: "test-token" };
jest.mock("@/lib/session", () => ({
  useAuth: () => ({ token: mockToken.current }),
}));

const mockApiFetch = jest.fn();
jest.mock("@/lib/api", () => ({
  ...jest.requireActual("@/lib/api"),
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
}));

jest.mock("sonner");

const baseFight: FightDto = {
  id: 1,
  eventId: 100,
  fighter1Name: "Jon Jones",
  fighter2Name: "Stipe Miocic",
  weightClass: "Heavyweight",
  isMainEvent: true,
  fightOrder: 1,
  status: "UPCOMING",
  resultWinner: null,
  resultMethod: null,
  resultRound: null,
  resultTime: null,
};

const mlPrediction: MlPredictionDto = {
  id: 1,
  fightId: 1,
  fighter1Name: "Jon Jones",
  fighter2Name: "Stipe Miocic",
  predictedWinner: "Jon Jones",
  confidenceScore: 0.85,
  cachedAt: "2026-01-01T00:00:00Z",
};

const communityVote: CommunityVoteDto = {
  id: 1,
  fightId: 1,
  fighter1Votes: 75,
  fighter2Votes: 25,
  lastUpdated: "2026-01-01T00:00:00Z",
};

beforeEach(() => {
  jest.clearAllMocks();
  mockToken.current = "test-token";
});

describe("PredictionCard", () => {
  it("renders fighter names and weight class", () => {
    render(<PredictionCard fight={baseFight} mlPrediction={null} communityVote={null} />);
    expect(screen.getAllByText(/Jon Jones/)[0]).toBeInTheDocument();
    expect(screen.getAllByText(/Stipe Miocic/)[0]).toBeInTheDocument();
    expect(screen.getByText("Heavyweight")).toBeInTheDocument();
  });

  it("shows fight status", () => {
    render(<PredictionCard fight={baseFight} mlPrediction={null} communityVote={null} />);
    expect(screen.getByText("Status: UPCOMING")).toBeInTheDocument();
  });

  it("shows ML prediction badge when provided", () => {
    render(<PredictionCard fight={baseFight} mlPrediction={mlPrediction} communityVote={null} />);
    expect(screen.getByText(/ML: Jon Jones • 85%/)).toBeInTheDocument();
  });

  it("does not show ML prediction badge when null", () => {
    render(<PredictionCard fight={baseFight} mlPrediction={null} communityVote={null} />);
    expect(screen.queryByText(/ML:/)).not.toBeInTheDocument();
  });

  it("calculates community vote percentages correctly", () => {
    render(<PredictionCard fight={baseFight} mlPrediction={null} communityVote={communityVote} />);
    expect(screen.getByText(/75% Jon Jones vs 25% Stipe Miocic/)).toBeInTheDocument();
  });

  it("shows 0% for both when no community votes", () => {
    render(<PredictionCard fight={baseFight} mlPrediction={null} communityVote={null} />);
    expect(screen.getByText(/0%.*vs.*0%/)).toBeInTheDocument();
  });

  it("disables form when fight is COMPLETED", () => {
    const completedFight = { ...baseFight, status: "COMPLETED" };
    render(<PredictionCard fight={completedFight} mlPrediction={null} communityVote={null} />);
    const button = screen.getByRole("button", { name: "Locked" });
    expect(button).toBeDisabled();
  });

  it("disables form when fight is LIVE", () => {
    const liveFight = { ...baseFight, status: "LIVE" };
    render(<PredictionCard fight={liveFight} mlPrediction={null} communityVote={null} />);
    expect(screen.getByRole("button", { name: "Locked" })).toBeDisabled();
  });

  it("disables form when fight is CANCELED", () => {
    const canceledFight = { ...baseFight, status: "CANCELED" };
    render(<PredictionCard fight={canceledFight} mlPrediction={null} communityVote={null} />);
    expect(screen.getByRole("button", { name: "Locked" })).toBeDisabled();
  });

  it("shows error toast when submitting without token", async () => {
    mockToken.current = null as any;
    const user = userEvent.setup();
    render(<PredictionCard fight={baseFight} mlPrediction={null} communityVote={null} />);
    
    const button = screen.getByRole("button", { name: "Submit prediction" });
    await user.click(button);

    expect(toast.error).toHaveBeenCalledWith("Sign in to submit a prediction.");
  });

  it("renders 5 round options for main event", () => {
    render(<PredictionCard fight={baseFight} mlPrediction={null} communityVote={null} />);
    // baseFight.isMainEvent = true, so should show 5 rounds + "Any Round"
    const roundSelect = screen.getAllByRole("combobox")[2]; // third select
    const options = roundSelect.querySelectorAll("option");
    expect(options.length).toBe(6); // Any Round + Round 1-5
  });

  it("renders 3 round options for non-main event", () => {
    const nonMainFight = { ...baseFight, isMainEvent: false, weightClass: "Lightweight" };
    render(<PredictionCard fight={nonMainFight} mlPrediction={null} communityVote={null} />);
    const roundSelect = screen.getAllByRole("combobox")[2];
    const options = roundSelect.querySelectorAll("option");
    expect(options.length).toBe(4); // Any Round + Round 1-3
  });

  it("submits prediction successfully", async () => {
    mockApiFetch.mockResolvedValueOnce({});
    const user = userEvent.setup();
    render(<PredictionCard fight={baseFight} mlPrediction={null} communityVote={null} />);

    const button = screen.getByRole("button", { name: "Submit prediction" });
    await user.click(button);

    await waitFor(() => {
      expect(mockApiFetch).toHaveBeenCalledWith(
        "/api/v1/predictions",
        expect.objectContaining({ method: "POST" }),
        "test-token"
      );
      expect(toast.success).toHaveBeenCalledWith("Prediction submitted successfully.");
    });
  });

  it("shows result when fight is completed", () => {
    const completedFight: FightDto = {
      ...baseFight,
      status: "COMPLETED",
      resultWinner: "Jon Jones",
      resultMethod: "KO/TKO",
      resultRound: 3,
    };
    render(<PredictionCard fight={completedFight} mlPrediction={null} communityVote={null} />);
    expect(screen.getByText(/Result: Jon Jones by KO\/TKO \(Round 3\)/)).toBeInTheDocument();
  });
});
