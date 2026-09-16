import { readFileSync } from 'node:fs';
import path from 'node:path';

import { describe, expect, it } from 'vitest';

import { apiClient } from '@/lib/api-client';

// A4-335 AC#5 (shared client targets /api/v1) + AC#6 (CSP present).
describe('api client', () => {
  it('targets the /api/v1 base path (AC#5)', () => {
    expect(apiClient.defaults.baseURL).toBe('/api/v1');
  });

  it('sends JSON by default', () => {
    expect(apiClient.defaults.headers['Content-Type']).toBe('application/json');
  });
});

describe('content security policy (AC#6)', () => {
  const html = readFileSync(path.resolve(process.cwd(), 'index.html'), 'utf8');

  it('declares a Content-Security-Policy meta tag', () => {
    expect(html).toContain('http-equiv="Content-Security-Policy"');
  });

  it('forbids inline scripts (script-src self, no unsafe-inline for scripts)', () => {
    expect(html).toContain("script-src 'self'");
    expect(html).not.toContain("script-src 'self' 'unsafe-inline'");
  });

  it('allowlists the backend origin for connections', () => {
    expect(html).toContain("connect-src 'self' http://localhost:8080");
  });
});
