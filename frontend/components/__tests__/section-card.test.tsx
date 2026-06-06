import { render, screen } from "@testing-library/react";
import { SectionCard } from "@/components/section-card";

describe("SectionCard", () => {
  it("renders title and children", () => {
    render(
      <SectionCard title="Test Title">
        <p>Child content</p>
      </SectionCard>
    );
    expect(screen.getByText("Test Title")).toBeInTheDocument();
    expect(screen.getByText("Child content")).toBeInTheDocument();
  });

  it("renders eyebrow when provided", () => {
    render(
      <SectionCard eyebrow="Label" title="Title">
        <div />
      </SectionCard>
    );
    expect(screen.getByText("Label")).toBeInTheDocument();
  });

  it("renders description when provided", () => {
    render(
      <SectionCard title="Title" description="Some description">
        <div />
      </SectionCard>
    );
    expect(screen.getByText("Some description")).toBeInTheDocument();
  });

  it("omits eyebrow when not provided", () => {
    const { container } = render(
      <SectionCard title="Title">
        <div />
      </SectionCard>
    );
    // No element with tracking-[0.3em] class for eyebrow
    const eyebrows = container.querySelectorAll(".tracking-\\[0\\.3em\\]");
    expect(eyebrows.length).toBe(0);
  });

  it("omits description when not provided", () => {
    const { container } = render(
      <SectionCard title="Title">
        <div />
      </SectionCard>
    );
    const descriptions = container.querySelectorAll("p");
    expect(descriptions.length).toBe(0);
  });
});
