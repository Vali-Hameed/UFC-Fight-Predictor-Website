import { render } from "@testing-library/react";
import { Providers } from "@/app/providers";

jest.mock("@/lib/session", () => ({
  AuthProvider: ({ children }: { children: React.ReactNode }) => (
    <div data-testid="auth-provider">{children}</div>
  ),
}));

jest.mock("sonner", () => ({
  Toaster: () => <div data-testid="toaster" />,
}));

describe("Providers", () => {
  it("wraps children in AuthProvider", () => {
    const { getByTestId, getByText } = render(
      <Providers>
        <span>Child</span>
      </Providers>
    );
    expect(getByTestId("auth-provider")).toBeInTheDocument();
    expect(getByText("Child")).toBeInTheDocument();
  });

  it("renders Toaster component", () => {
    const { getByTestId } = render(
      <Providers>
        <span>Child</span>
      </Providers>
    );
    expect(getByTestId("toaster")).toBeInTheDocument();
  });
});
