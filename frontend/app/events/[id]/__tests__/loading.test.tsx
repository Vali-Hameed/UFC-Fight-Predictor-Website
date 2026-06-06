import { render } from "@testing-library/react";
import EventDetailLoading from "@/app/events/[id]/loading";

describe("EventDetailLoading", () => {
  it("renders skeleton placeholders", () => {
    const { container } = render(<EventDetailLoading />);
    const skeletons = container.querySelectorAll(".animate-pulse");
    expect(skeletons.length).toBeGreaterThan(10);
  });

  it("renders section cards", () => {
    const { getByText } = render(<EventDetailLoading />);
    expect(getByText("Loading Event...")).toBeInTheDocument();
    expect(getByText("Accuracy")).toBeInTheDocument();
  });
});
