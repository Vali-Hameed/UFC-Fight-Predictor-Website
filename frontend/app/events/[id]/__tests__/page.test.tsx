/**
 * Tests for app/events/[id]/page.tsx (server component)
 */

import { render, screen } from "@testing-library/react";

const mockApiFetch = jest.fn();
const mockNotFound = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
  ApiResponseError: class extends Error {
    status: number;
    constructor(status: number, message: string) {
      super(message);
      this.status = status;
      this.name = "ApiResponseError";
    }
  },
  getEventLeaderboard: (...args: unknown[]) => mockApiFetch("/api/v1/leaderboard/event/" + args[0]),
}));

jest.mock("next/navigation", () => ({
  notFound: () => mockNotFound(),
}));

jest.mock("@/components/prediction-card", () => ({
  PredictionCard: ({ fight }: any) => <div data-testid="prediction-card">{fight.fighter1Name} vs {fight.fighter2Name}</div>,
}));

async function renderEventPage(id: string) {
  const EventPage = (await import("@/app/events/[id]/page")).default;
  const jsx = await EventPage({ params: Promise.resolve({ id }) });
  return render(jsx as any);
}

beforeEach(() => {
  jest.clearAllMocks();
});

describe("EventPage", () => {
  it("renders event details and fight cards", async () => {
    mockApiFetch
      .mockResolvedValueOnce({ id: 1, name: "UFC 300", location: "Las Vegas", status: "UPCOMING" }) // event
      .mockResolvedValueOnce([ // fights
        { id: 10, eventId: 1, fighter1Name: "Fighter A", fighter2Name: "Fighter B", status: "UPCOMING", isMainEvent: true },
      ])
      .mockResolvedValueOnce([]) // threads
      .mockResolvedValueOnce([]) // leaderboard
      .mockResolvedValueOnce(null) // ml prediction
      .mockResolvedValueOnce(null); // community vote

    await renderEventPage("1");

    expect(screen.getByText("UFC 300")).toBeInTheDocument();
    expect(screen.getByTestId("prediction-card")).toBeInTheDocument();
  });

  it("shows error with retry for non-404 failures", async () => {
    const { ApiResponseError } = await import("@/lib/api");
    mockApiFetch.mockRejectedValueOnce(new (ApiResponseError as any)(500, "Server error"));

    await renderEventPage("1");

    expect(screen.getByText("Could not load event")).toBeInTheDocument();
    expect(screen.getByText("Retry")).toBeInTheDocument();
  });

  it("shows empty state messages", async () => {
    mockApiFetch
      .mockResolvedValueOnce({ id: 1, name: "UFC 301", location: "NYC", status: "UPCOMING" })
      .mockResolvedValueOnce([]) // no fights
      .mockResolvedValueOnce([]) // no threads
      .mockResolvedValueOnce([]); // no leaderboard

    await renderEventPage("1");

    expect(screen.getByText(/No forum threads available/)).toBeInTheDocument();
    expect(screen.getByText(/Leaderboard has not been populated/)).toBeInTheDocument();
  });
});
