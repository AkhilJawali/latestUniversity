import { useState } from 'react';

import { useCampuses } from '@/features/master-data/campus-hierarchy/api/useCampuses';
import { useCampusStore } from '@/stores/campusStore';
import CommonSlotTab from '../components/CommonSlotTab';
import DerivationRuleTab from '../components/DerivationRuleTab';
import SoftWeightTab from '../components/SoftWeightTab';
import '../scheduling-config.css';

// A4-340 section 5.1 — config admin page: campus guard + tabs for the three entities.
// PD-76 called for a header campus selector owned by the shell; A4-335 ships no
// such selector, so this page provides a lightweight campus picker and writes it
// to campusStore. TODO: move to a shared header selector when the shell adds one.
const TABS = [
  { id: 'rules', label: 'Derivation Rules', render: (campusId) => <DerivationRuleTab campusId={campusId} /> },
  { id: 'weights', label: 'Soft-Constraint Weights', render: (campusId) => <SoftWeightTab campusId={campusId} /> },
  { id: 'slots', label: 'Common Slots', render: (campusId) => <CommonSlotTab campusId={campusId} /> },
];

export default function SchedulingConfigPage() {
  const campusId = useCampusStore((s) => s.campusId);
  const setCampusId = useCampusStore((s) => s.setCampusId);
  const [active, setActive] = useState('rules');

  const campusesQuery = useCampuses();
  const campuses = campusesQuery.data?.data ?? [];
  const selectedCampus = campuses.find((c) => c.id === campusId) ?? null;

  return (
    <section>
      <h1 className="page-title">Engine Configuration</h1>
      <p className="page-subtitle">
        Manage session-derivation rules, soft-constraint weights, and institution common slots per campus.
      </p>

      <div className="campus-picker">
        <label htmlFor="campusSelect">Campus</label>
        <select
          id="campusSelect"
          value={campusId ?? ''}
          disabled={campusesQuery.isLoading || campusesQuery.isError}
          onChange={(e) => setCampusId(e.target.value ? Number(e.target.value) : null)}
        >
          <option value="">
            {campusesQuery.isLoading
              ? 'Loading campuses…'
              : campusesQuery.isError
                ? 'Failed to load campuses'
                : 'Select a campus…'}
          </option>
          {campuses.map((c) => (
            <option key={c.id} value={c.id}>
              {c.code} — {c.name}
            </option>
          ))}
        </select>
        {selectedCampus && (
          <span className="campus-picker-selected">
            Managing: {selectedCampus.code} — {selectedCampus.name}
          </span>
        )}
      </div>

      {campusId == null ? (
        <div className="empty-state">
          <p>Select a campus above to manage its scheduling-engine configuration.</p>
        </div>
      ) : (
        <>
          <div className="tabs" role="tablist" aria-label="Configuration entities">
            {TABS.map((t, i) => (
              <button
                key={t.id}
                id={`tab-${t.id}`}
                type="button"
                role="tab"
                aria-selected={active === t.id}
                aria-controls={`panel-${t.id}`}
                tabIndex={active === t.id ? 0 : -1}
                className={`tab ${active === t.id ? 'tab--active' : ''}`}
                onClick={() => setActive(t.id)}
                onKeyDown={(e) => {
                  if (e.key === 'ArrowRight') setActive(TABS[(i + 1) % TABS.length].id);
                  if (e.key === 'ArrowLeft') setActive(TABS[(i - 1 + TABS.length) % TABS.length].id);
                }}
              >
                {t.label}
              </button>
            ))}
          </div>
          <div id={`panel-${active}`} role="tabpanel" aria-labelledby={`tab-${active}`}>
            {TABS.find((t) => t.id === active).render(campusId)}
          </div>
        </>
      )}
    </section>
  );
}
