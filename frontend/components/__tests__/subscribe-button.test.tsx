import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SubscribeButton } from "@/components/subscribe-button";
import { toast } from "sonner";

jest.mock("sonner");

const mockToken = { current: null as string | null };
jest.mock("@/lib/session", () => ({
  useAuth: () => ({ token: mockToken.current, user: { username: "testuser" } }),
}));

const mockGetSubscriptionStatus = jest.fn();
const mockToggleThreadSubscription = jest.fn();
jest.mock("@/lib/api", () => ({
  getSubscriptionStatus: (...args: unknown[]) => mockGetSubscriptionStatus(...args),
  toggleThreadSubscription: (...args: unknown[]) => mockToggleThreadSubscription(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
  mockToken.current = null;
});

describe("SubscribeButton", () => {
  it("renders nothing when no token", () => {
    const { container } = render(<SubscribeButton threadId={1} />);
    expect(container.innerHTML).toBe("");
  });

  it("renders nothing while loading", () => {
    mockToken.current = "token123";
    mockGetSubscriptionStatus.mockReturnValue(new Promise(() => {})); // never resolves
    const { container } = render(<SubscribeButton threadId={1} />);
    expect(container.querySelector("button")).toBeNull();
  });

  it("shows Subscribe when not subscribed", async () => {
    mockToken.current = "token123";
    mockGetSubscriptionStatus.mockResolvedValueOnce(false);

    render(<SubscribeButton threadId={1} />);

    await waitFor(() => {
      expect(screen.getByText("Subscribe")).toBeInTheDocument();
    });
  });

  it("shows Unsubscribe when subscribed", async () => {
    mockToken.current = "token123";
    mockGetSubscriptionStatus.mockResolvedValueOnce(true);

    render(<SubscribeButton threadId={1} />);

    await waitFor(() => {
      expect(screen.getByText("Unsubscribe")).toBeInTheDocument();
    });
  });

  it("toggles subscription and updates text", async () => {
    mockToken.current = "token123";
    mockGetSubscriptionStatus.mockResolvedValueOnce(false);
    mockToggleThreadSubscription.mockResolvedValueOnce(true);

    const user = userEvent.setup();
    render(<SubscribeButton threadId={1} />);

    await waitFor(() => {
      expect(screen.getByText("Subscribe")).toBeInTheDocument();
    });

    await user.click(screen.getByText("Subscribe"));

    await waitFor(() => {
      expect(mockToggleThreadSubscription).toHaveBeenCalledWith(1, "token123");
      expect(toast.success).toHaveBeenCalledWith("Subscribed to thread notifications.");
      expect(screen.getByText("Unsubscribe")).toBeInTheDocument();
    });
  });

  it("shows error toast on toggle failure", async () => {
    mockToken.current = "token123";
    mockGetSubscriptionStatus.mockResolvedValueOnce(false);
    mockToggleThreadSubscription.mockRejectedValueOnce(new Error("Failed"));

    const user = userEvent.setup();
    render(<SubscribeButton threadId={1} />);

    await waitFor(() => {
      expect(screen.getByText("Subscribe")).toBeInTheDocument();
    });

    await user.click(screen.getByText("Subscribe"));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Failed to update subscription status.");
    });
  });
});
