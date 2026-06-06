import { render, screen, waitFor, act } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import AdminPage from "@/app/admin/page";
import { toast } from "sonner";

jest.mock("sonner");

const mockNotFound = jest.fn();
jest.mock("next/navigation", () => ({
  notFound: () => mockNotFound(),
}));

const mockAuth = {
  token: null as string | null,
  loading: false,
  user: null as any,
};
jest.mock("@/lib/session", () => ({
  useAuth: () => mockAuth,
}));

const mockApiFetch = jest.fn();
const mockWarnUser = jest.fn();
const mockBanUser = jest.fn();
const mockUnbanUser = jest.fn();
const mockDeleteUser = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
  warnUser: (...args: unknown[]) => mockWarnUser(...args),
  banUser: (...args: unknown[]) => mockBanUser(...args),
  unbanUser: (...args: unknown[]) => mockUnbanUser(...args),
  deleteUser: (...args: unknown[]) => mockDeleteUser(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
  mockAuth.token = null;
  mockAuth.loading = false;
  mockAuth.user = null;
  (global.confirm as jest.Mock).mockReturnValue(true);
});

describe("AdminPage", () => {
  it("calls notFound for non-admin users", async () => {
    mockAuth.user = { username: "user", role: "ROLE_USER" };
    await act(async () => {
      render(<AdminPage />);
    });
    expect(mockNotFound).toHaveBeenCalled();
  });

  it("returns null while loading", async () => {
    mockAuth.loading = true;
    let container: HTMLElement;
    await act(async () => {
      const result = render(<AdminPage />);
      container = result.container;
    });
    expect(container!.innerHTML).toBe("");
  });

  it("renders admin panel for admin user", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { username: "admin", role: "ROLE_ADMIN" };
    mockApiFetch
      .mockResolvedValueOnce([]) // users
      .mockResolvedValueOnce([]); // logs

    await act(async () => {
      render(<AdminPage />);
    });

    expect(screen.getByText("Operations panel")).toBeInTheDocument();
    expect(screen.getByText("Moderation tools")).toBeInTheDocument();
  });

  it("loads and displays scraper logs", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { username: "admin", role: "ROLE_ADMIN" };
    mockApiFetch
      .mockResolvedValueOnce([]) // users
      .mockResolvedValueOnce([ // logs
        { id: 1, startedAt: null, completedAt: null, eventsFound: 5, fightsUpdated: 20, status: "SUCCESS", errorMessage: null },
      ]);

    await act(async () => {
      render(<AdminPage />);
    });

    await waitFor(() => {
      expect(screen.getByText("SUCCESS")).toBeInTheDocument();
      expect(screen.getByText(/Events: 5/)).toBeInTheDocument();
      expect(screen.getByText(/Fights: 20/)).toBeInTheDocument();
    });
  });

  it("loads and displays users", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { username: "admin", role: "ROLE_ADMIN" };
    mockApiFetch
      .mockResolvedValueOnce([ // users
        { id: 5, username: "testuser", email: "test@test.com", locked: false, enabled: true, role: { id: 2, name: "ROLE_USER" } },
      ])
      .mockResolvedValueOnce([]); // logs

    await act(async () => {
      render(<AdminPage />);
    });

    await waitFor(() => {
      expect(screen.getByText("testuser")).toBeInTheDocument();
      expect(screen.getByText("test@test.com")).toBeInTheDocument();
    });
  });

  it("triggers ML prewarm", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { username: "admin", role: "ROLE_ADMIN" };
    mockApiFetch
      .mockResolvedValueOnce([]) // users
      .mockResolvedValueOnce([]) // logs
      .mockResolvedValueOnce(undefined); // prewarm response

    const user = userEvent.setup();
    await act(async () => {
      render(<AdminPage />);
    });

    await user.click(screen.getByText("Trigger ML prewarm"));

    await waitFor(() => {
      expect(mockApiFetch).toHaveBeenCalledWith(
        "/api/v1/admin/prewarm/trigger",
        expect.objectContaining({ method: "POST" }),
        "admin-token"
      );
      expect(toast.success).toHaveBeenCalledWith("Prewarm triggered.");
    });
  });

  it("triggers scraper", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { username: "admin", role: "ROLE_ADMIN" };
    mockApiFetch
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce(undefined); // scraper response

    const user = userEvent.setup();
    await act(async () => {
      render(<AdminPage />);
    });

    await user.click(screen.getByText("Trigger Scraper"));

    await waitFor(() => {
      expect(mockApiFetch).toHaveBeenCalledWith(
        "/api/v1/admin/scraper/trigger",
        expect.objectContaining({ method: "POST" }),
        "admin-token"
      );
    });
  });

  it("shows empty states when no data", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { username: "admin", role: "ROLE_ADMIN" };
    mockApiFetch
      .mockResolvedValueOnce([]) // no users
      .mockResolvedValueOnce([]); // no logs

    await act(async () => {
      render(<AdminPage />);
    });

    await waitFor(() => {
      expect(screen.getByText("No scraper logs loaded yet.")).toBeInTheDocument();
      expect(screen.getByText("No users loaded yet.")).toBeInTheDocument();
    });
  });

  it("toggles user lock state", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { username: "admin", role: "ROLE_ADMIN" };
    const testUser = { id: 5, username: "testuser", email: "test@test.com", locked: false, enabled: true, role: { id: 2, name: "ROLE_USER" } };
    mockApiFetch
      .mockResolvedValueOnce([testUser]) // users
      .mockResolvedValueOnce([]) // logs
      .mockResolvedValueOnce({ ...testUser, locked: true }); // lock response

    const user = userEvent.setup();
    await act(async () => {
      render(<AdminPage />);
    });

    await waitFor(() => {
      expect(screen.getByText("Lock")).toBeInTheDocument();
    });

    await user.click(screen.getByText("Lock"));

    await waitFor(() => {
      expect(mockApiFetch).toHaveBeenCalledWith(
        "/api/v1/admin/users/5/ban?locked=true",
        expect.objectContaining({ method: "PATCH" }),
        "admin-token"
      );
    });
  });

  it("warns user", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { username: "admin", role: "ROLE_ADMIN" };
    mockApiFetch
      .mockResolvedValueOnce([
        { id: 5, username: "testuser", email: "test@test.com", locked: false, enabled: true, role: { id: 2, name: "ROLE_USER" } },
      ])
      .mockResolvedValueOnce([]);
    mockWarnUser.mockResolvedValueOnce(undefined);

    const user = userEvent.setup();
    await act(async () => {
      render(<AdminPage />);
    });

    await waitFor(() => {
      expect(screen.getByText("Warn")).toBeInTheDocument();
    });

    await user.click(screen.getByText("Warn"));

    await waitFor(() => {
      expect(mockWarnUser).toHaveBeenCalledWith(5, "admin-token");
      expect(toast.success).toHaveBeenCalledWith("User warned.");
    });
  });

  it("deletes user and removes from list", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { username: "admin", role: "ROLE_ADMIN" };
    mockApiFetch
      .mockResolvedValueOnce([
        { id: 5, username: "testuser", email: "test@test.com", locked: false, enabled: true, role: { id: 2, name: "ROLE_USER" } },
      ])
      .mockResolvedValueOnce([]);
    mockDeleteUser.mockResolvedValueOnce(undefined);

    const user = userEvent.setup();
    await act(async () => {
      render(<AdminPage />);
    });

    await waitFor(() => {
      expect(screen.getByText("Delete User")).toBeInTheDocument();
    });

    await user.click(screen.getByText("Delete User"));

    await waitFor(() => {
      expect(mockDeleteUser).toHaveBeenCalledWith(5, "admin-token");
      expect(toast.success).toHaveBeenCalledWith("User deleted.");
    });
  });

  it("disables moderation buttons for admin-role users", async () => {
    mockAuth.token = "admin-token";
    mockAuth.user = { username: "admin", role: "ROLE_ADMIN" };
    mockApiFetch
      .mockResolvedValueOnce([
        { id: 99, username: "otheradmin", email: "admin2@test.com", locked: false, enabled: true, role: { id: 1, name: "ROLE_ADMIN" } },
      ])
      .mockResolvedValueOnce([]);

    await act(async () => {
      render(<AdminPage />);
    });

    await waitFor(() => {
      const warnBtn = screen.getByText("Warn");
      expect(warnBtn).toBeDisabled();
    });
  });
});
