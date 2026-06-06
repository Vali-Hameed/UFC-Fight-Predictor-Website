import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import LoginPage from "@/app/(auth)/login/page";
import { toast } from "sonner";

jest.mock("sonner");

const mockRouter = { push: jest.fn(), refresh: jest.fn() };
jest.mock("next/navigation", () => ({
  useRouter: () => mockRouter,
}));

const mockLogin = jest.fn();
jest.mock("@/lib/session", () => ({
  useAuth: () => ({ login: mockLogin }),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

describe("LoginPage", () => {
  it("renders login form", () => {
    render(<LoginPage />);
    expect(screen.getByText("Sign in")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Username or email")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Password")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Login" })).toBeInTheDocument();
  });

  it("renders forgot password and sign up links", () => {
    render(<LoginPage />);
    expect(screen.getByText("Forgot password?")).toBeInTheDocument();
    expect(screen.getByText("Sign up")).toBeInTheDocument();
  });

  it("successful login redirects to home", async () => {
    mockLogin.mockResolvedValueOnce(undefined);
    const user = userEvent.setup();

    render(<LoginPage />);

    await user.type(screen.getByPlaceholderText("Username or email"), "john");
    await user.type(screen.getByPlaceholderText("Password"), "pass123");
    await user.click(screen.getByRole("button", { name: "Login" }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith("john", "pass123");
      expect(toast.success).toHaveBeenCalledWith("Successfully logged in.");
      expect(mockRouter.push).toHaveBeenCalledWith("/");
    });
  });

  it("shows resend UI for USER_DISABLED error", async () => {
    mockLogin.mockRejectedValueOnce({ errorCode: "USER_DISABLED" });
    const user = userEvent.setup();

    render(<LoginPage />);

    await user.type(screen.getByPlaceholderText("Username or email"), "unverified");
    await user.type(screen.getByPlaceholderText("Password"), "pass");
    await user.click(screen.getByRole("button", { name: "Login" }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Account is not verified. Please verify your email.");
      expect(screen.getByText("Resend Verification Email")).toBeInTheDocument();
    });
  });

  it("shows locked message for USER_LOCKED error", async () => {
    mockLogin.mockRejectedValueOnce({ errorCode: "USER_LOCKED" });
    const user = userEvent.setup();

    render(<LoginPage />);

    await user.type(screen.getByPlaceholderText("Username or email"), "locked");
    await user.type(screen.getByPlaceholderText("Password"), "pass");
    await user.click(screen.getByRole("button", { name: "Login" }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Your account is locked. Please contact an administrator.");
    });
  });

  it("shows generic error for other failures", async () => {
    mockLogin.mockRejectedValueOnce(new Error("Network error"));
    const user = userEvent.setup();

    render(<LoginPage />);

    await user.type(screen.getByPlaceholderText("Username or email"), "user");
    await user.type(screen.getByPlaceholderText("Password"), "pass");
    await user.click(screen.getByRole("button", { name: "Login" }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Login failed. Check your credentials and try again.");
    });
  });
});
