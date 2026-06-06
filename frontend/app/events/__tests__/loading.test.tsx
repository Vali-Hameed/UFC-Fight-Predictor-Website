import { render } from "@testing-library/react";
import EventsLoading from "@/app/events/loading";

describe("EventsLoading", () => {
  it("renders skeleton placeholders", () => {
    const { container } = render(<EventsLoading />);
    const skeletons = container.querySelectorAll(".animate-pulse");
    expect(skeletons.length).toBeGreaterThanOrEqual(6);
  });

  it("renders SectionCard with correct title", () => {
    const { getByText } = render(<EventsLoading />);
    expect(getByText("Event listing")).toBeInTheDocument();
  });
});
