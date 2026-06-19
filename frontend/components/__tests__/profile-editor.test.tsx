import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { ProfileEditor } from "@/components/profile-editor";
import { toast } from "sonner";

jest.mock("sonner");

const mockAuth = { token: null as string | null };
jest.mock("@/lib/session", () => ({
  useAuth: () => ({ token: mockAuth.token }),
}));

const mockApiFetch = jest.fn();
const mockGetAvailableTitles = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
  getAvailableTitles: (...args: unknown[]) => mockGetAvailableTitles(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
  mockAuth.token = null;
  mockGetAvailableTitles.mockResolvedValue([]);
});

const mockProfile = {
  id: 1,
  username: "john",
  firstName: "John",
  lastName: "Doe",
  profileImageUrl: "https://example.com/avatar.jpg",
  role: "ROLE_USER",
  enabled: true,
  publicProfile: true,
};

describe("ProfileEditor", () => {
  it("shows sign-in message when no token", () => {
    render(<ProfileEditor username="john" />);
    expect(screen.getByText("Sign in to edit your profile.")).toBeInTheDocument();
  });

  it("shows loading state while fetching profile", () => {
    mockAuth.token = "token123";
    mockApiFetch.mockReturnValue(new Promise(() => {})); // never resolves
    render(<ProfileEditor username="john" />);
    expect(screen.getByText("Loading profile editor...")).toBeInTheDocument();
  });

  it("loads and displays profile data", async () => {
    mockAuth.token = "token123";
    mockApiFetch.mockResolvedValueOnce(mockProfile);

    render(<ProfileEditor username="john" />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("John")).toBeInTheDocument();
      expect(screen.getByDisplayValue("Doe")).toBeInTheDocument();
    });
  });

  it("shows message when viewing another user's profile", async () => {
    mockAuth.token = "token123";
    mockApiFetch.mockResolvedValueOnce({ ...mockProfile, username: "other" });

    render(<ProfileEditor username="john" />);

    await waitFor(() => {
      expect(screen.getByText(/This is a public profile view/)).toBeInTheDocument();
    });
  });

  it("saves profile on form submit", async () => {
    mockAuth.token = "token123";
    mockApiFetch
      .mockResolvedValueOnce(mockProfile) // initial load
      .mockResolvedValueOnce({ ...mockProfile, firstName: "Jane" }); // save response

    const user = userEvent.setup();
    render(<ProfileEditor username="john" />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("John")).toBeInTheDocument();
    });

    const firstNameInput = screen.getByDisplayValue("John");
    await user.clear(firstNameInput);
    await user.type(firstNameInput, "Jane");
    await user.click(screen.getByRole("button", { name: "Save profile" }));

    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith("Profile updated.");
    });
  });

  it("shows error toast on save failure", async () => {
    mockAuth.token = "token123";
    mockApiFetch
      .mockResolvedValueOnce(mockProfile) // initial load
      .mockRejectedValueOnce(new Error("Server error")); // save failure

    const user = userEvent.setup();
    render(<ProfileEditor username="john" />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("John")).toBeInTheDocument();
    });

    await user.click(screen.getByRole("button", { name: "Save profile" }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Could not update your profile.");
    });
  });

  it("toggles profile visibility", async () => {
    mockAuth.token = "token123";
    mockApiFetch.mockResolvedValueOnce(mockProfile);

    const user = userEvent.setup();
    render(<ProfileEditor username="john" />);

    await waitFor(() => {
      expect(screen.getByRole("switch")).toBeInTheDocument();
    });

    const toggle = screen.getByRole("switch");
    expect(toggle).toHaveAttribute("aria-checked", "true");

    await user.click(toggle);
    expect(toggle).toHaveAttribute("aria-checked", "false");
  });

  it("handles password reset request", async () => {
    mockAuth.token = "token123";
    mockApiFetch
      .mockResolvedValueOnce(mockProfile) // initial load
      .mockResolvedValueOnce(undefined); // password reset

    const user = userEvent.setup();
    render(<ProfileEditor username="john" />);

    await waitFor(() => {
      expect(screen.getByText("Reset password")).toBeInTheDocument();
    });

    await user.click(screen.getByText("Reset password"));

    await waitFor(() => {
      expect(mockApiFetch).toHaveBeenCalledWith(
        "/api/v1/password/request-me",
        expect.objectContaining({ method: "POST" }),
        "token123"
      );
      expect(toast.success).toHaveBeenCalledWith("Password reset link sent to your email.");
    });
  });

  it("renders available titles and allows selection", async () => {
    mockAuth.token = "token123";
    mockApiFetch.mockResolvedValueOnce(mockProfile);
    mockGetAvailableTitles.mockResolvedValueOnce([
      { id: "SS25 Champion", label: "SS25 Champion", type: "SEASON_CHAMPION" },
      { id: "2x Event Winner", label: "2x Event Winner", type: "EVENT_WINNER" },
    ]);

    render(<ProfileEditor username="john" />);

    await waitFor(() => {
      expect(screen.getByText("SS25 Champion")).toBeInTheDocument();
      expect(screen.getByText("2x Event Winner")).toBeInTheDocument();
    });

    const select = screen.getByRole("combobox");
    expect(select).toBeInTheDocument();
  });

  it("hides cosmetic title dropdown when titles fail to load", async () => {
    mockAuth.token = "token123";
    mockApiFetch.mockResolvedValueOnce(mockProfile);
    mockGetAvailableTitles.mockRejectedValueOnce(new Error("Failed to load titles"));

    render(<ProfileEditor username="john" />);

    await waitFor(() => {
      expect(screen.getByDisplayValue("John")).toBeInTheDocument();
    });

    // The dropdown should not be rendered
    expect(screen.queryByText("Cosmetic Title")).not.toBeInTheDocument();
  });
});
