import PropTypes from 'prop-types';
import { useState } from 'react';

import ConfirmDeleteDialog from '@/features/scheduling-config/components/ConfirmDeleteDialog';
import { mapApiError } from '@/lib/api-error';
import {
  useAddExamWindow,
  useAddHoliday,
  useAddOrientation,
  useRemoveHoliday,
} from '../api/useAcademicCalendars';
import { EXAM_TYPES, HOLIDAY_SCOPES, labelOf } from '../constants/calendar-constants';
import ExamWindowFormModal from './ExamWindowFormModal';
import HolidayFormModal from './HolidayFormModal';
import OrientationFormModal from './OrientationFormModal';

// A4-440 §9 — detail for a selected calendar: semester header + three sub-lists.
// Holidays = add + remove (scope badge). Exam windows / orientation = add-only, no row
// actions (OQ-3). Sub-entities come from the calendar detail's children arrays — adding
// one invalidates the detail query so the list refreshes. The page remounts this panel
// via key={calendarId} so local modal/delete state resets on selection change.
export default function CalendarDetailPanel({ calendar, onToast }) {
  const calendarId = calendar.id;

  const addHolidayMut = useAddHoliday(calendarId);
  const removeHolidayMut = useRemoveHoliday(calendarId);
  const addExamMut = useAddExamWindow(calendarId);
  const addOrientationMut = useAddOrientation(calendarId);

  const [holidayOpen, setHolidayOpen] = useState(false);
  const [examOpen, setExamOpen] = useState(false);
  const [orientationOpen, setOrientationOpen] = useState(false);
  const [holidayToDelete, setHolidayToDelete] = useState(null);
  const [deleteError, setDeleteError] = useState(null);

  const holidays = calendar.holidays ?? [];
  const examWindows = calendar.examWindows ?? [];
  const orientationPeriods = calendar.orientationPeriods ?? [];

  const submitHoliday = (data, handlers) =>
    addHolidayMut.mutate(data, {
      ...handlers,
      onSuccess: () => {
        setHolidayOpen(false);
        onToast?.('success', 'Holiday added.');
      },
    });

  const submitExam = (data, handlers) =>
    addExamMut.mutate(data, {
      ...handlers,
      onSuccess: () => {
        setExamOpen(false);
        onToast?.('success', 'Exam window added.');
      },
    });

  const submitOrientation = (data, handlers) =>
    addOrientationMut.mutate(data, {
      ...handlers,
      onSuccess: () => {
        setOrientationOpen(false);
        onToast?.('success', 'Orientation period added.');
      },
    });

  const confirmDeleteHoliday = () => {
    setDeleteError(null);
    removeHolidayMut.mutate(holidayToDelete.id, {
      onSuccess: () => {
        setHolidayToDelete(null);
        onToast?.('success', 'Holiday removed.');
      },
      onError: (err) =>
        setDeleteError(mapApiError(err).message ?? 'Could not remove this holiday.'),
    });
  };
  const cancelDeleteHoliday = () => {
    setHolidayToDelete(null);
    setDeleteError(null);
  };

  return (
    <section className="card calendar-detail" aria-label="Calendar detail">
      <div className="section-head">
        <h2>
          {calendar.academicYear} — {calendar.semesterIdentifier}
        </h2>
      </div>
      <p className="page-subtitle">
        Semester: {calendar.semesterStartDate} → {calendar.semesterEndDate}
      </p>

      {/* Holidays: add + remove, with a scope badge (OQ-4) */}
      <div className="subsection">
        <div className="section-head">
          <h3>Holidays</h3>
          <button type="button" className="btn btn--primary" onClick={() => setHolidayOpen(true)}>
            Add holiday
          </button>
        </div>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Start</th>
                <th scope="col">End</th>
                <th scope="col">Description</th>
                <th scope="col">Scope</th>
                <th scope="col" className="col-actions">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody>
              {holidays.length === 0 && (
                <tr>
                  <td colSpan={5} className="table-empty">
                    No holidays yet.
                  </td>
                </tr>
              )}
              {holidays.map((h) => (
                <tr key={h.id}>
                  <td>{h.startDate}</td>
                  <td>{h.endDate}</td>
                  <td>{h.description}</td>
                  <td>
                    <span className="badge">{labelOf(HOLIDAY_SCOPES, h.scope)}</span>
                  </td>
                  <td className="row-actions">
                    <button
                      type="button"
                      className="icon-btn icon-btn--danger"
                      aria-label={`Delete holiday ${h.description}`}
                      onClick={() => {
                        setDeleteError(null);
                        setHolidayToDelete(h);
                      }}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Exam windows: add-only (OQ-3), no row actions */}
      <div className="subsection">
        <div className="section-head">
          <h3>Exam windows</h3>
          <button type="button" className="btn btn--primary" onClick={() => setExamOpen(true)}>
            Add exam window
          </button>
        </div>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Start</th>
                <th scope="col">End</th>
                <th scope="col">Type</th>
                <th scope="col">Description</th>
              </tr>
            </thead>
            <tbody>
              {examWindows.length === 0 && (
                <tr>
                  <td colSpan={4} className="table-empty">
                    No exam windows yet.
                  </td>
                </tr>
              )}
              {examWindows.map((w) => (
                <tr key={w.id}>
                  <td>{w.startDate}</td>
                  <td>{w.endDate}</td>
                  <td>{labelOf(EXAM_TYPES, w.examType)}</td>
                  <td>{w.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      {/* Orientation periods: add-only (OQ-3), no row actions */}
      <div className="subsection">
        <div className="section-head">
          <h3>Orientation periods</h3>
          <button
            type="button"
            className="btn btn--primary"
            onClick={() => setOrientationOpen(true)}
          >
            Add orientation
          </button>
        </div>
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th scope="col">Start</th>
                <th scope="col">End</th>
                <th scope="col">Description</th>
              </tr>
            </thead>
            <tbody>
              {orientationPeriods.length === 0 && (
                <tr>
                  <td colSpan={3} className="table-empty">
                    No orientation periods yet.
                  </td>
                </tr>
              )}
              {orientationPeriods.map((o) => (
                <tr key={o.id}>
                  <td>{o.startDate}</td>
                  <td>{o.endDate}</td>
                  <td>{o.description}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <HolidayFormModal
        open={holidayOpen}
        isPending={addHolidayMut.isPending}
        onSubmit={submitHoliday}
        onClose={() => setHolidayOpen(false)}
      />
      <ExamWindowFormModal
        open={examOpen}
        semesterStartDate={calendar.semesterStartDate}
        semesterEndDate={calendar.semesterEndDate}
        isPending={addExamMut.isPending}
        onSubmit={submitExam}
        onClose={() => setExamOpen(false)}
      />
      <OrientationFormModal
        open={orientationOpen}
        semesterStartDate={calendar.semesterStartDate}
        semesterEndDate={calendar.semesterEndDate}
        isPending={addOrientationMut.isPending}
        onSubmit={submitOrientation}
        onClose={() => setOrientationOpen(false)}
      />

      <ConfirmDeleteDialog
        open={Boolean(holidayToDelete)}
        label={
          deleteError
            ? `holiday. ${deleteError}`
            : holidayToDelete
              ? `holiday "${holidayToDelete.description}"`
              : 'this holiday'
        }
        isPending={removeHolidayMut.isPending}
        onConfirm={confirmDeleteHoliday}
        onCancel={cancelDeleteHoliday}
      />
    </section>
  );
}

CalendarDetailPanel.propTypes = {
  calendar: PropTypes.shape({
    id: PropTypes.oneOfType([PropTypes.string, PropTypes.number]).isRequired,
    academicYear: PropTypes.string,
    semesterIdentifier: PropTypes.string,
    semesterStartDate: PropTypes.string,
    semesterEndDate: PropTypes.string,
    holidays: PropTypes.array,
    examWindows: PropTypes.array,
    orientationPeriods: PropTypes.array,
  }).isRequired,
  onToast: PropTypes.func,
};
