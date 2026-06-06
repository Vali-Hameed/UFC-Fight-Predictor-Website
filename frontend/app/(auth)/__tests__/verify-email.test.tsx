import { render, screen, waitFor } from "@testing-library/react";
import VerifyEmailPage from "@/app/(auth)/verify-email/page";
import { toast } from "sonner";

jest.mock("sonner");

const mockRouter = { push: jest.fn(), refresh: jest.fn() };
let mockSearchParams = new URLSearchParams();
jest.mock("next/navigation", () => ({
  useRouter: () => mockRouter,
  useSearchParams: () => mockSearchParams,
}));

const mockApiFetch = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
}));

beforeEach(() => {
  jest.clearAllMocks();
});

describe("VerifyEmailPage", () => {
  it("shows check inbox message when no token in URL", () => {
    mockSearchParams = new URLSearchParams("");

    render(<VerifyEmailPage />);
    expect(screen.getByText("Check your inbox")).toBeInTheDocument();
    expect(screen.getByText(/Waiting for you to click the link/)).toBeInTheDocument();
  });

  it("verifies email with token and shows success", async () => {
    mockSearchParams = new URLSearchParams("?token=verify-123");
    mockApiFetch.mockResolvedValueOnce("confirmed");

    render(<VerifyEmailPage />);

    await waitFor(() => {
      expect(toast.success).toHaveBeenCalledWith("Email verified successfully! You can now log in.");
      expect(screen.getByText(/Email verified/)).toBeInTheDocument();
    });
  });

  it("shows failure message on verification error", async () => {
    mockSearchParams = new URLSearchParams("?token=expired-token");
    mockApiFetch.mockRejectedValueOnce(new Error("Invalid token"));

    render(<VerifyEmailPage />);

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith("Verification failed. The link may be expired or invalid.");
      expect(screen.getByText(/Verification failed/)).toBeInTheDocument();
    });
  });
});
