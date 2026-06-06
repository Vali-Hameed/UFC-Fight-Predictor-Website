import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { AdminModerationMenu } from "@/components/admin-moderation-menu";
import { toast } from "sonner";

jest.mock("sonner");

const mockRouter = { push: jest.fn(), refresh: jest.fn() };
jest.mock("next/navigation", () => ({
  useRouter: () => mockRouter,
}));

const mockAuth = { token: null as string | null, user: null as any };
jest.mock("@/lib/session", () => ({
  useAuth: () => mockAuth,
}));

const mockWarnUser = jest.fn();
const mockBanUser = jest.fn();
const mockDeletePost = jest.fn();
jest.mock("@/lib/api", () => ({
  warnUser: (...args: unknown[]) => mockWarnUser(...args),
  banUser: (...args: unknown[]) => mockBanUser(...args),
  deletePost: (...args: unknown[]) => mockDeletePost(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
  mockAuth.token = null;
  mockAuth.user = null;
  (global.confirm as jest.Mock).mockReturnValue(true);
});

describe("AdminModerationMenu", () => {
  it("returns null for non-admin non-owner users", () => {
    mockAuth.token = "token";
    mockAuth.user = { id: 999, username: "other", role: "ROLE_USER" };
    const { container } = render(<AdminModerationMenu postId={1} postUserId={5} />);
    expect(container.innerHTML).toBe("");
  });

  it("returns null when no token and not owner", () => {
    const { container } = render(<AdminModerationMenu postId={1} postUserId={5} />);
    expect(container.innerHTML).toBe("");
  });

  it("shows Delete button for post author (non-admin)", () => {
    mockAuth.token = "token";
    mockAuth.user = { id: 5, username: "author", role: "ROLE_USER" };
    render(<AdminModerationMenu postId={1} postUserId={5} />);
    expect(screen.getByText("Delete")).toBeInTheDocument();
    expect(screen.queryByText("Warn User")).not.toBeInTheDocument();
  });

  it("admin sees all moderation buttons for other users", () => {
    mockAuth.token = "token";
    mockAuth.user = { id: 1, username: "admin", role: "ROLE_ADMIN" };
    render(<AdminModerationMenu postId={1} postUserId={5} />);
    expect(screen.getByText("Delete Post")).toBeInTheDocument();
    expect(screen.getByText("Warn User")).toBeInTheDocument();
    expect(screen.getByText("Ban 7d")).toBeInTheDocument();
    expect(screen.getByText("Ban Perm")).toBeInTheDocument();
  });

  it("admin does not see warn/ban for own posts", () => {
    mockAuth.token = "token";
    mockAuth.user = { id: 5, username: "admin", role: "ROLE_ADMIN" };
    render(<AdminModerationMenu postId={1} postUserId={5} />);
    expect(screen.getByText("Delete Post")).toBeInTheDocument();
    expect(screen.queryByText("Warn User")).not.toBeInTheDocument();
    expect(screen.queryByText("Ban 7d")).not.toBeInTheDocument();
  });

  it("calls warnUser API when Warn User clicked", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { id: 1, username: "admin", role: "ROLE_ADMIN" };
    mockWarnUser.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();

    render(<AdminModerationMenu postId={1} postUserId={5} />);
    await user.click(screen.getByText("Warn User"));

    await waitFor(() => {
      expect(mockWarnUser).toHaveBeenCalledWith(5, "admin-token");
      expect(toast.success).toHaveBeenCalledWith("User warned.");
    });
  });

  it("calls banUser API with duration when Ban 7d clicked", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { id: 1, username: "admin", role: "ROLE_ADMIN" };
    mockBanUser.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();

    render(<AdminModerationMenu postId={1} postUserId={5} />);
    await user.click(screen.getByText("Ban 7d"));

    await waitFor(() => {
      expect(mockBanUser).toHaveBeenCalledWith(5, "admin-token", 7);
    });
  });

  it("calls banUser API without duration for Ban Perm", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { id: 1, username: "admin", role: "ROLE_ADMIN" };
    mockBanUser.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();

    render(<AdminModerationMenu postId={1} postUserId={5} />);
    await user.click(screen.getByText("Ban Perm"));

    await waitFor(() => {
      expect(mockBanUser).toHaveBeenCalledWith(5, "admin-token", undefined);
    });
  });

  it("calls deletePost API when Delete Post clicked", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { id: 1, username: "admin", role: "ROLE_ADMIN" };
    mockDeletePost.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();

    render(<AdminModerationMenu postId={42} postUserId={5} />);
    await user.click(screen.getByText("Delete Post"));

    await waitFor(() => {
      expect(mockDeletePost).toHaveBeenCalledWith(42, "admin-token");
      expect(toast.success).toHaveBeenCalledWith("Post deleted.");
    });
  });

  it("does not call API when confirm is cancelled", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { id: 1, username: "admin", role: "ROLE_ADMIN" };
    (global.confirm as jest.Mock).mockReturnValue(false);
    const user = userEvent.setup();

    render(<AdminModerationMenu postId={1} postUserId={5} />);
    await user.click(screen.getByText("Warn User"));

    expect(mockWarnUser).not.toHaveBeenCalled();
  });
});
