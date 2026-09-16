import { useState } from 'react';
import { Link } from 'react-router-dom';

import {
  useDraft,
  useGenerationStatus,
  isTerminal,
} from '@/features/scheduling/generation/api/useGeneration';
import DraftSummary from '@/features/scheduling/generation/components/DraftSummary';
import GenerateForm from '@/features/scheduling/generation/components/GenerateForm';
import InfeasibilityReport from '@/features/scheduling/generation/components/InfeasibilityReport';
import OutcomeBanner from '@/features/scheduling/generation/components/OutcomeBanner';
import ProgressPanel from '@/features/scheduling/generation/components/ProgressPanel';
import TimetableGrid from '@/features/scheduling/generation/components/TimetableGrid';
import UnplacedList from '@/features/scheduling/generation/components/UnplacedList';
import ViolationsList from '@/features/scheduling/generation/components/ViolationsList';

import './generation.css';

// A4-345 orchestrator — trigger -> progress -> terminal outcome. Holds the
// current requestId in page-local state; TanStack Query owns all server state.
export default function GenerationPage() {
  const [requestId, setRequestId] = useState(null);

  const statusQuery = useGenerationStatus(requestId);
  const status = statusQuery.data?.status;
  const draftId = statusQuery.data?.draftId;

  // Draft summary is loaded only for terminal states that produced a draft
  // (COMPLETED or TIMED_OUT). INFEASIBLE / FAILED / CANCELLED carry no draft.
  const hasDraft = draftId != null && (status === 'COMPLETED' || status === 'TIMED_OUT');
  const draftQuery = useDraft(hasDraft ? draftId : null);

  const running = requestId != null && !isTerminal(status);

  return (
    <div className="generation-page">
      <h1 className="page-title">Timetable Generation</h1>
      <p className="page-subtitle">
        Trigger generation, watch progress, and review the produced draft.
      </p>

      <GenerateForm onStarted={setRequestId} />

      {statusQuery.isError && (
        <p className="form-error" role="alert">
          Could not read generation status. Please try again.
        </p>
      )}

      {running && <ProgressPanel status={statusQuery.data} />}

      {isTerminal(status) && (
        <div className="generation-result">
          <OutcomeBanner status={status} />

          {(status === 'COMPLETED' || status === 'TIMED_OUT') && (
            <>
              <DraftSummary draft={draftQuery.data} />
              {draftId != null && (
                <p className="editor-entry">
                  <Link className="btn btn--primary" to={`/scheduling/editor/${draftId}`}>
                    Open in editor
                  </Link>{' '}
                  {draftQuery.data?.departmentId != null && (
                    <Link
                      className="btn"
                      to={`/scheduling/approvals?departmentId=${draftQuery.data.departmentId}&draftId=${draftId}`}
                    >
                      Send for approval
                    </Link>
                  )}
                </p>
              )}
              {draftId != null && (
                <TimetableGrid draftId={draftId} departmentId={draftQuery.data?.departmentId} />
              )}
              {draftId != null && <ViolationsList draftId={draftId} />}
            </>
          )}

          {status === 'TIMED_OUT' && draftId != null && <UnplacedList draftId={draftId} />}

          {status === 'INFEASIBLE' && requestId != null && (
            <InfeasibilityReport requestId={requestId} />
          )}
        </div>
      )}
    </div>
  );
}
