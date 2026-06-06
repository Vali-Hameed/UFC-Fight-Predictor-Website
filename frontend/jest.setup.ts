import "@testing-library/jest-dom";

// Global mock for window.confirm
global.confirm = jest.fn(() => true);

// Suppress act() warnings in tests
const originalError = console.error;
beforeAll(() => {
  console.error = (...args: unknown[]) => {
    if (typeof args[0] === "string" && args[0].includes("Not implemented: HTMLFormElement.prototype.submit")) {
      return;
    }
    originalError.call(console, ...args);
  };
});

afterAll(() => {
  console.error = originalError;
});
