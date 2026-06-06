import { render } from "@testing-library/react";
import Loading from "@/app/loading";

describe("Loading (home)", () => {
  it("renders skeleton placeholders", () => {
    const { container } = render(<Loading />);
    const skeletons = container.querySelectorAll(".animate-pulse");
    expect(skeletons.length).toBeGreaterThan(5);
  });
});
