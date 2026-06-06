/**
 * Tests for lib/session.tsx
 *
 * Covers: AuthProvider, useAuth hook, login, logout, refreshSession.
 */
import { render, screen, act, waitFor } from "@testing-library/react";
import { AuthProvider, useAuth } from "@/lib/session";
import * as api from "@/lib/api";

jest.mock("@/lib/api", () => ({
  apiFetch: jest.fn(),
}));

const mockApiFetch = api.apiFetch as jest.MockedFunction<typeof api.apiFetch>;

function TestConsumer() {
  const { token, loading, user, login, logout } = useAuth();
  return (
    <div>
      <span data-testid="token">{token ?? "null"}</span>
      <span data-testid="loading">{String(loading)}</span>
      <span data-testid="user">{user ? user.username : "null"}</span>
      <button onClick={() => login("testuser", "password123")}>Login</button>
      <button onClick={() => logout()}>Logout</button>
    </div>
  );
}

beforeEach(() => {
  jest.clearAllMocks();
});

describe("useAuth", () => {
  it("throws when used outside AuthProvider", () => {
    // Suppress console.error for expected error
    const spy = jest.spyOn(console, "error").mockImplementation(() => {});
    expect(() => render(<TestConsumer />)).toThrow("useAuth must be used within AuthProvider");
    spy.mockRestore();
  });
});

describe("AuthProvider", () => {
  it("calls refresh on mount and sets token on success", async () => {
    mockApiFetch
      .mockResolvedValueOnce({ accessToken: "abc123", expiresInSeconds: 3600 }) // refresh
      .mockResolvedValueOnce({ username: "john", role: "ROLE_USER" }); // /users/me

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("token").textContent).toBe("abc123");
    });

    await waitFor(() => {
      expect(screen.getByTestId("user").textContent).toBe("john");
    });
  });

  it("sets token to null when refresh fails", async () => {
    mockApiFetch.mockRejectedValueOnce(new Error("Unauthorized"));

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("token").textContent).toBe("null");
      expect(screen.getByTestId("loading").textContent).toBe("false");
    });
  });

  it("login sets token on success", async () => {
    // Initial refresh fails
    mockApiFetch.mockRejectedValueOnce(new Error("No session"));

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("loading").textContent).toBe("false");
    });

    // Set up login response
    mockApiFetch
      .mockResolvedValueOnce({ accessToken: "login-token", expiresInSeconds: 3600 }) // login
      .mockResolvedValueOnce({ username: "testuser", role: "ROLE_USER" }); // /users/me

    await act(async () => {
      screen.getByText("Login").click();
    });

    await waitFor(() => {
      expect(screen.getByTestId("token").textContent).toBe("login-token");
    });
  });

  it("logout clears token", async () => {
    mockApiFetch
      .mockResolvedValueOnce({ accessToken: "abc123", expiresInSeconds: 3600 }) // refresh
      .mockResolvedValueOnce({ username: "john", role: "ROLE_USER" }); // /users/me

    render(
      <AuthProvider>
        <TestConsumer />
      </AuthProvider>
    );

    await waitFor(() => {
      expect(screen.getByTestId("token").textContent).toBe("abc123");
    });

    // Set up logout
    mockApiFetch.mockResolvedValueOnce(undefined); // logout endpoint

    await act(async () => {
      screen.getByText("Logout").click();
    });

    await waitFor(() => {
      expect(screen.getByTestId("token").textContent).toBe("null");
      expect(screen.getByTestId("user").textContent).toBe("null");
    });
  });
});
