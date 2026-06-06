import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { NotificationCenter } from "@/components/notification-center";

jest.mock("sonner");

const mockToken = { current: null as string | null };
jest.mock("@/lib/session", () => ({
  useAuth: () => ({ token: mockToken.current }),
}));

const mockApiFetch = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
  mockToken.current = null;
});

describe("NotificationCenter", () => {
  it("shows sign-in message when no token", () => {
    render(<NotificationCenter />);
    expect(screen.getByText("Sign in to view notifications.")).toBeInTheDocument();
  });

  it("fetches and displays notifications", async () => {
    mockToken.current = "token123";
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, userId: 1, type: "RESULT", message: "Fight result posted", read: false, createdAt: "2026-01-01T00:00:00Z" },
      { id: 2, userId: 1, type: "STREAK", message: "You hit a 5 streak!", read: true, createdAt: "2026-01-02T00:00:00Z" },
    ]);

    render(<NotificationCenter />);

    await waitFor(() => {
      expect(screen.getByText("Fight result posted")).toBeInTheDocument();
      expect(screen.getByText("You hit a 5 streak!")).toBeInTheDocument();
    });
  });

  it("shows empty state when no notifications", async () => {
    mockToken.current = "token123";
    mockApiFetch.mockResolvedValueOnce([]);

    render(<NotificationCenter />);

    await waitFor(() => {
      expect(screen.getByText("No notifications yet.")).toBeInTheDocument();
    });
  });

  it("marks notification as read", async () => {
    mockToken.current = "token123";
    mockApiFetch
      .mockResolvedValueOnce([
        { id: 1, userId: 1, type: "RESULT", message: "New result", read: false, createdAt: "2026-01-01T00:00:00Z" },
      ])
      .mockResolvedValueOnce({}); // mark read response

    const user = userEvent.setup();
    render(<NotificationCenter />);

    await waitFor(() => {
      expect(screen.getByText("New result")).toBeInTheDocument();
    });

    const markReadBtn = screen.getByRole("button", { name: "Mark read" });
    await user.click(markReadBtn);

    await waitFor(() => {
      expect(mockApiFetch).toHaveBeenCalledWith(
        "/api/v1/notifications/1/read",
        expect.objectContaining({ method: "PATCH" }),
        "token123"
      );
    });
  });

  it("renders notification with link as clickable", async () => {
    mockToken.current = "token123";
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, userId: 1, type: "FORUM", message: "New reply", read: false, link: "/forum/5", createdAt: "2026-01-01T00:00:00Z" },
    ]);

    render(<NotificationCenter />);

    await waitFor(() => {
      const link = screen.getByText("New reply");
      expect(link.closest("a")).toHaveAttribute("href", "/forum/5");
    });
  });

  it("shows notification type label", async () => {
    mockToken.current = "token123";
    mockApiFetch.mockResolvedValueOnce([
      { id: 1, userId: 1, type: "LEADERBOARD", message: "Rank up!", read: false, createdAt: "2026-01-01T00:00:00Z" },
    ]);

    render(<NotificationCenter />);

    await waitFor(() => {
      expect(screen.getByText("LEADERBOARD")).toBeInTheDocument();
    });
  });
});
