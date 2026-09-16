// Vitest global setup (A4-335, OQ-DF1). Registers jest-dom matchers
// (toBeInTheDocument, etc.) and cleans up the DOM after each test.
import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

afterEach(() => {
  cleanup();
});
