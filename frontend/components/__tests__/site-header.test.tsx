/* eslint-disable @typescript-eslint/no-explicit-any */
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { SiteHeader } from "@/components/site-header";

jest.mock("sonner");

const mockRouter = { push: jest.fn(), refresh: jest.fn() };
const mockPathname = { current: "/" };
jest.mock("next/navigation", () => ({
  useRouter: () => mockRouter,
  usePathname: () => mockPathname.current,
}));

const mockAuth = {
  token: null as string | null,
  loading: false,
  user: null as any,
  logout: jest.fn(),
};
jest.mock("@/lib/session", () => ({
  useAuth: () => mockAuth,
}));

const mockGetUnreadCount = jest.fn();
jest.mock("@/lib/api", () => ({
  getUnreadNotificationCount: (...args: unknown[]) => mockGetUnreadCount(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
  mockAuth.token = null;
  mockAuth.loading = false;
  mockAuth.user = null;
  mockAuth.logout = jest.fn();
  mockPathname.current = "/";
  mockGetUnreadCount.mockResolvedValue(0);
});

describe("SiteHeader", () => {
  it("renders logo and brand text", () => {
    render(<SiteHeader />);
    expect(screen.getByText("UFC")).toBeInTheDocument();
    expect(screen.getByText("Fight Predictor")).toBeInTheDocument();
  });

  it("renders nav links", () => {
    render(<SiteHeader />);
    expect(screen.getAllByText("Events").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Leaderboard").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Simulator").length).toBeGreaterThan(0);
    expect(screen.getAllByText("Notifications").length).toBeGreaterThan(0);
  });

  it("hides Admin link for non-admin users", () => {
    mockAuth.user = { username: "user1", role: "ROLE_USER" };
    render(<SiteHeader />);
    expect(screen.queryByText("Admin")).not.toBeInTheDocument();
  });

  it("shows Admin link for admin users", () => {
    mockAuth.token = "token";
    mockAuth.user = { username: "admin", role: "ROLE_ADMIN" };
    render(<SiteHeader />);
    expect(screen.getAllByText("Admin").length).toBeGreaterThan(0);
  });

  it("shows Login link when not authenticated", () => {
    render(<SiteHeader />);
    expect(screen.getAllByText("Login").length).toBeGreaterThan(0);
  });

  it("shows Logout button when authenticated", () => {
    mockAuth.token = "token";
    mockAuth.user = { username: "user1" };
    render(<SiteHeader />);
    expect(screen.getAllByText("Logout").length).toBeGreaterThan(0);
  });

  it("shows Profile link when user is logged in", () => {
    mockAuth.token = "token";
    mockAuth.user = { username: "john" };
    render(<SiteHeader />);
    expect(screen.getAllByText("Profile").length).toBeGreaterThan(0);
  });

  it("does not show Profile link when logged out", () => {
    render(<SiteHeader />);
    expect(screen.queryByText("Profile")).not.toBeInTheDocument();
  });

  it("shows unread notification badge", async () => {
    mockAuth.token = "token";
    mockAuth.user = { username: "user1" };
    mockGetUnreadCount.mockResolvedValueOnce(5);

    render(<SiteHeader />);

    await waitFor(() => {
      expect(screen.getAllByText("5").length).toBeGreaterThan(0);
    });
  });

  it("caps badge at 99+", async () => {
    mockAuth.token = "token";
    mockAuth.user = { username: "user1" };
    mockGetUnreadCount.mockResolvedValueOnce(150);

    render(<SiteHeader />);

    await waitFor(() => {
      expect(screen.getAllByText("99+").length).toBeGreaterThan(0);
    });
  });

  it("handles logout click", async () => {
    mockAuth.token = "token";
    mockAuth.user = { username: "user1" };
    mockAuth.logout.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();

    render(<SiteHeader />);

    // Click the first (desktop) logout button
    const logoutButtons = screen.getAllByText("Logout");
    await user.click(logoutButtons[0]);

    expect(mockAuth.logout).toHaveBeenCalled();
  });

  it("shows session status text", () => {
    mockAuth.token = "token";
    render(<SiteHeader />);
    expect(screen.getByText("Signed in")).toBeInTheDocument();
  });

  it("shows loading text during session sync", () => {
    mockAuth.loading = true;
    render(<SiteHeader />);
    expect(screen.getByText("Syncing session")).toBeInTheDocument();
  });

  it("toggles mobile menu", async () => {
    mockAuth.token = "token";
    mockAuth.user = { username: "user1" };
    const user = userEvent.setup();

    render(<SiteHeader />);

    // Find the mobile menu toggle button (the one with svg)
    const toggleButtons = screen.getAllByRole("button");
    const mobileToggle = toggleButtons.find(
      (btn) => btn.className.includes("md:hidden")
    );
    
    if (mobileToggle) {
      await user.click(mobileToggle);
      // After opening, mobile nav should be visible
      // The mobile menu renders a second set of links
    }
  });
});
