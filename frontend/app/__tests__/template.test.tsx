/* eslint-disable @typescript-eslint/no-explicit-any */
import { render, screen } from "@testing-library/react";
import Template from "@/app/template";

jest.mock("framer-motion", () => ({
  motion: {
    div: ({ children, ...props }: any) => <div {...props}>{children}</div>,
  },
}));

describe("Template", () => {
  it("renders children", () => {
    render(
      <Template>
        <p>Page content</p>
      </Template>
    );
    expect(screen.getByText("Page content")).toBeInTheDocument();
  });
});
