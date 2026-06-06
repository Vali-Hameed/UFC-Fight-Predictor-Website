/* eslint-disable @typescript-eslint/no-explicit-any */
/**
 * Tests for app/profile/[username]/page.tsx (server component)
 */

import { render, screen } from "@testing-library/react";

const mockApiFetch = jest.fn();
jest.mock("@/lib/api", () => ({
  apiFetch: (...args: unknown[]) => mockApiFetch(...args),
}));

jest.mock("@/components/profile-view", () => ({
  ProfileView: ({ initialProfile, username }: any) => (
    <div data-testid="profile-view">
      {initialProfile ? `Profile: ${initialProfile.username}` : "No profile"}
      {` | Username: ${username}`}
    </div>
  ),
}));

async function renderProfilePage(username: string) {
  const ProfilePage = (await import("@/app/profile/[username]/page")).default;
  const jsx = await ProfilePage({ params: Promise.resolve({ username }) });
  return render(jsx as any);
}

beforeEach(() => {
  jest.clearAllMocks();
});

describe("ProfilePage", () => {
  it("renders SectionCard with username", async () => {
    mockApiFetch.mockResolvedValueOnce({ id: 1, username: "john", firstName: "John" });

    await renderProfilePage("john");

    expect(screen.getByText("@john")).toBeInTheDocument();
  });

  it("passes profile data to ProfileView", async () => {
    mockApiFetch.mockResolvedValueOnce({ id: 1, username: "jane" });

    await renderProfilePage("jane");

    expect(screen.getByTestId("profile-view")).toHaveTextContent("Profile: jane");
  });

  it("passes null to ProfileView on API failure", async () => {
    mockApiFetch.mockRejectedValueOnce(new Error("Not found"));

    await renderProfilePage("unknown");

    expect(screen.getByTestId("profile-view")).toHaveTextContent("No profile");
  });
});
