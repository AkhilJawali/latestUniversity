import { create } from 'zustand';

// A4-340 PD-76 — selected campus context shared across the config admin panel.
// The header campus selector writes campusId; every config query reads it and
// stays disabled until a campus is chosen. Persisted to sessionStorage so a
// reload keeps the selection within the tab.
const STORAGE_KEY = 'utms.campusId';

function readInitial() {
  const raw = typeof sessionStorage !== 'undefined' ? sessionStorage.getItem(STORAGE_KEY) : null;
  return raw ? Number(raw) : null;
}

export const useCampusStore = create((set) => ({
  campusId: readInitial(),
  setCampusId: (campusId) => {
    if (typeof sessionStorage !== 'undefined') {
      if (campusId == null) sessionStorage.removeItem(STORAGE_KEY);
      else sessionStorage.setItem(STORAGE_KEY, String(campusId));
    }
    set({ campusId });
  },
}));
