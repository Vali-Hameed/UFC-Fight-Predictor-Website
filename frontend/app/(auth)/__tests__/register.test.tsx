import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import RegisterPage from "@/app/(auth)/register/page";
import { toast } from "sonner";

jest.mock("sonner");

const mockRouter = { push: jest.fn(), refresh: jest.fn() };
jest.mock("next/navigation", () => ({
  useRouter: () => mockRouter,
}));

const mockApiFetch = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

describe("RegisterPage", () => {
  it("renders all form fields", () => {
    render(<RegisterPage />);
    expect(screen.getByText("Create account")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("First name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Last name")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Username")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Email")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Password")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("Confirm password")).toBeInTheDocument();
  });

  it("shows error when passwords don't match", async () => {
    const user = userEvent.setup();
    render(<RegisterPage />);

    await user.type(screen.getByPlaceholderText("Password"), "pass1");
    await user.type(screen.getByPlaceholderText("Confirm password"), "pass2");
    await user.click(screen.getByRole("button", { name: "Register" }));

    expect(toast.error).toHaveBeenCalledWith("Passwords do not match.");
    expect(mockApiFetch).not.toHaveBeenCalled();
  });

  it("redirects to verify-email on success", async () => {
    mockApiFetch.mockResolvedValueOnce("Registered");
    const user = userEvent.setup();

    render(<RegisterPage />);

    await user.type(screen.getByPlaceholderText("First name"), "John");
    await user.type(screen.getByPlaceholderText("Last name"), "Doe");
    await user.type(screen.getByPlaceholderText("Username"), "johnd");
    await user.type(screen.getByPlaceholderText("Email"), "john@test.com");
    await user.type(screen.getByPlaceholderText("Password"), "pass123");
    await user.type(screen.getByPlaceholderText("Confirm password"), "pass123");
    await user.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith("Account created! Please check your email.");
      expect(mockRouter.push).toHaveBeenCalledWith("/verify-email");
    });
  });

  it("shows resend UI for 'registered but not verified' error", async () => {
    mockApiFetch.mockRejectedValueOnce({ message: "Account is registered but not verified" });
    const user = userEvent.setup();

    render(<RegisterPage />);

    await user.type(screen.getByPlaceholderText("Password"), "pass123");
    await user.type(screen.getByPlaceholderText("Confirm password"), "pass123");
    await user.type(screen.getByPlaceholderText("Email"), "test@test.com");
    await user.click(screen.getByRole("button", { name: "Register" }));

    await waitFor(() => {
      expect(screen.getByText("Resend Verification Email")).toBeInTheDocument();
    });
  });
});
