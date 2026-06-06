/**
 * Tests for app/events/page.tsx (server component)
 * We test this by mocking apiFetch and rendering the async component.
 */

import { render, screen } from "@testing-library/react";

const mockApiFetch = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
}));

// Async server components need special handling
async function renderAsyncComponent() {
  const EventsPage = (await import("@/app/events/page")).default;
  const jsx = await EventsPage();
  return render(jsx);
}

beforeEach(() => {
  jest.clearAllMocks();
});

describe("EventsPage", () => {
  it("renders events list", async () => {
    mockApiFetch
      .mockResolvedValueOnce([ // events
        { id: 1, name: "UFC 300", location: "Las Vegas", status: "UPCOMING", eventDate: null, scrapedAt: null },
      ])
      .mockResolvedValueOnce([ // fights for event 1
        { id: 10, eventId: 1, fighter1Name: "F1", fighter2Name: "F2", isMainEvent: true, status: "UPCOMING" },
      ])
      .mockResolvedValueOnce({ // ml prediction for main fight
        id: 1, fightId: 10, predictedWinner: "F1", confidenceScore: 0.75, cachedAt: null,
      });

    await renderAsyncComponent();

    expect(screen.getByText("UFC 300")).toBeInTheDocument();
    expect(screen.getByText("Las Vegas")).toBeInTheDocument();
    expect(screen.getByText("UPCOMING")).toBeInTheDocument();
  });

  it("shows ML prediction badge for main event", async () => {
    mockApiFetch
      .mockResolvedValueOnce([
        { id: 1, name: "UFC 300", location: "Vegas", status: "UPCOMING", eventDate: null, scrapedAt: null },
      ])
      .mockResolvedValueOnce([
        { id: 10, eventId: 1, fighter1Name: "A", fighter2Name: "B", isMainEvent: true, status: "UPCOMING" },
      ])
      .mockResolvedValueOnce({
        id: 1, fightId: 10, predictedWinner: "A", confidenceScore: 0.9, cachedAt: null,
      });

    await renderAsyncComponent();
    expect(screen.getByText(/Main Event ML: A • 90%/)).toBeInTheDocument();
  });

  it("shows empty state when no events", async () => {
    mockApiFetch.mockResolvedValueOnce([]);

    await renderAsyncComponent();
    expect(screen.getByText("No events available yet.")).toBeInTheDocument();
  });
});
