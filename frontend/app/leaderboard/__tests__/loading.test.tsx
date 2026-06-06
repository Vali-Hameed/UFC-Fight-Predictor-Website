import { render } from "@testing-library/react";
import LeaderboardLoading from "@/app/leaderboard/loading";

describe("LeaderboardLoading", () => {
  it("renders skeleton placeholders", () => {
    const { container } = render(<LeaderboardLoading />);
    const skeletons = container.querySelectorAll(".animate-pulse");
    expect(skeletons.length).toBeGreaterThanOrEqual(10);
  });

  it("renders correct title", () => {
    const { getByText } = render(<LeaderboardLoading />);
    expect(getByText("Loading Leaderboard...")).toBeInTheDocument();
  });
});
