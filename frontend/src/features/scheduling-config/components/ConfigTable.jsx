import PropTypes from 'prop-types';
import { useMemo, useState } from 'react';

// A4-340 section 5.2 (enhanced) — the shared config/master-data table used across the
// whole app. Renders loading / error / empty / populated states with a consistent
// structure. All cells render as text (React escapes; no dangerouslySetInnerHTML — NFR-2).
//
// Built-in filtering (additive, opt-in, backward-compatible):
// - A search box filters across every column's rendered text when `searchable` (default true)
//   and there is at least one row.
// - Any column with `filterable: true` gets a dropdown of its distinct values.
// Filtering is client-side over `rows` — these are small master-data sets.

// Resolve a column's display text for a row (used for search + distinct filter values).
function cellText(col, row) {
  if (col.render) {
    const out = col.render(row);
    // render() may return a node; only strings/numbers are searchable/filterable.
    return typeof out === 'string' || typeof out === 'number' ? String(out) : '';
  }
  return String(row[col.key] ?? '');
}

export default function ConfigTable({
  columns,
  rows,
  isLoading,
  isError,
  onRetry,
  onAdd,
  onEdit,
  onDelete,
  addLabel,
  entityLabel,
  searchable = true,
}) {
  const [search, setSearch] = useState('');
  const [colFilters, setColFilters] = useState({});
  const [page, setPage] = useState(0);
  const pageSize = 10;

  const filterableCols = columns.filter((c) => c.filterable);
  const showFilterBar =
    !isError && !isLoading && rows.length > 0 && (searchable || filterableCols.length > 0);

  // Distinct values per filterable column, for the dropdowns.
  const distinctValues = useMemo(() => {
    const map = {};
    filterableCols.forEach((col) => {
      const set = new Set();
      rows.forEach((r) => {
        const t = cellText(col, r);
        if (t !== '') set.add(t);
      });
      map[col.key] = Array.from(set).sort();
    });
    return map;
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rows, columns]);

  const visibleRows = useMemo(() => {
    const q = search.trim().toLowerCase();
    return rows.filter((row) => {
      // Per-column dropdown filters (exact match on rendered text).
      for (const col of filterableCols) {
        const selected = colFilters[col.key];
        if (selected && cellText(col, row) !== selected) return false;
      }
      // Free-text search across all columns' rendered text.
      if (q) {
        const hay = columns
          .map((col) => cellText(col, row))
          .join(' ')
          .toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rows, columns, search, colFilters]);

  const hasActiveFilters = Boolean(search) || Object.values(colFilters).some(Boolean);
  const clearFilters = () => {
    setSearch('');
    setColFilters({});
  };

  // Pagination (client-side over the filtered rows). Reset to page 0 whenever the
  // filtered set shrinks so the current page never points past the end.
  const pageCount = Math.max(1, Math.ceil(visibleRows.length / pageSize));
  const safePage = Math.min(page, pageCount - 1);
  const pagedRows = visibleRows.slice(safePage * pageSize, safePage * pageSize + pageSize);
  const showPagination = !isLoading && !isError && visibleRows.length > pageSize;

  if (isError) {
    return (
      <section className="card table-card" aria-label={entityLabel}>
        <div className="section-head">
          <h2>{entityLabel}</h2>
        </div>
        <p className="form-error" role="alert">
          Could not load {entityLabel.toLowerCase()}.{' '}
          <button type="button" className="btn" onClick={onRetry}>
            Retry
          </button>
        </p>
      </section>
    );
  }

  return (
    <section className="card table-card" aria-label={entityLabel}>
      <div className="section-head">
        <h2>{entityLabel}</h2>
        <button type="button" className="btn btn--primary" onClick={onAdd}>
          {addLabel}
        </button>
      </div>

      {showFilterBar && (
        <div className="table-toolbar">
          {searchable && (
            <div className="table-search">
              <input
                type="search"
                aria-label={`Search ${entityLabel.toLowerCase()}`}
                placeholder={`Search ${entityLabel.toLowerCase()}…`}
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
            </div>
          )}

          {filterableCols.map((col) => (
            <div className="table-filter" key={col.key}>
              <label htmlFor={`filter-${col.key}`}>{col.header}</label>
              <select
                id={`filter-${col.key}`}
                value={colFilters[col.key] ?? ''}
                onChange={(e) => setColFilters((f) => ({ ...f, [col.key]: e.target.value }))}
              >
                <option value="">All</option>
                {distinctValues[col.key].map((v) => (
                  <option key={v} value={v}>
                    {v}
                  </option>
                ))}
              </select>
            </div>
          ))}

          {hasActiveFilters && (
            <button type="button" className="link-btn" onClick={clearFilters}>
              Clear filters
            </button>
          )}

          <span className="table-count">
            {visibleRows.length} of {rows.length}
          </span>
        </div>
      )}

      <div className="table-scroll">
        <table className="data-table" aria-busy={isLoading}>
          <thead>
            <tr>
              {columns.map((col) => (
                <th key={col.key} scope="col">
                  {col.header}
                </th>
              ))}
              <th scope="col" className="col-actions">
                Actions
              </th>
            </tr>
          </thead>
          <tbody>
            {isLoading &&
              Array.from({ length: 3 }).map((_, i) => (
                <tr key={`skeleton-${i}`} className="skeleton-row">
                  {columns.map((col) => (
                    <td key={col.key}>
                      <span className="skeleton-cell" />
                    </td>
                  ))}
                  <td>
                    <span className="skeleton-cell" />
                  </td>
                </tr>
              ))}

            {!isLoading && rows.length === 0 && (
              <tr>
                <td colSpan={columns.length + 1} className="table-empty">
                  No {entityLabel.toLowerCase()} yet.{' '}
                  <button type="button" className="link-btn" onClick={onAdd}>
                    {addLabel}
                  </button>
                </td>
              </tr>
            )}

            {!isLoading && rows.length > 0 && visibleRows.length === 0 && (
              <tr>
                <td colSpan={columns.length + 1} className="table-empty">
                  No {entityLabel.toLowerCase()} match the current filters.{' '}
                  <button type="button" className="link-btn" onClick={clearFilters}>
                    Clear filters
                  </button>
                </td>
              </tr>
            )}

            {!isLoading &&
              pagedRows.map((row) => (
                <tr key={row.id}>
                  {columns.map((col) => (
                    <td key={col.key}>{col.render ? col.render(row) : String(row[col.key] ?? '')}</td>
                  ))}
                  <td className="row-actions">
                    <button
                      type="button"
                      className="icon-btn"
                      aria-label={`Edit ${entityLabel} ${row.id}`}
                      onClick={() => onEdit(row)}
                    >
                      Edit
                    </button>
                    <button
                      type="button"
                      className="icon-btn icon-btn--danger"
                      aria-label={`Delete ${entityLabel} ${row.id}`}
                      onClick={() => onDelete(row)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
          </tbody>
        </table>
      </div>

      {showPagination && (
        <div className="table-pagination">
          <span className="pagination-info">
            {safePage * pageSize + 1}–{Math.min((safePage + 1) * pageSize, visibleRows.length)} of{' '}
            {visibleRows.length}
          </span>
          <div className="pagination-controls">
            <button
              type="button"
              className="btn"
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={safePage === 0}
            >
              Previous
            </button>
            <span className="pagination-page">
              Page {safePage + 1} of {pageCount}
            </span>
            <button
              type="button"
              className="btn"
              onClick={() => setPage((p) => Math.min(pageCount - 1, p + 1))}
              disabled={safePage >= pageCount - 1}
            >
              Next
            </button>
          </div>
        </div>
      )}
    </section>
  );
}

ConfigTable.propTypes = {
  columns: PropTypes.arrayOf(
    PropTypes.shape({
      key: PropTypes.string.isRequired,
      header: PropTypes.string.isRequired,
      render: PropTypes.func,
      filterable: PropTypes.bool,
    }),
  ).isRequired,
  rows: PropTypes.array.isRequired,
  isLoading: PropTypes.bool,
  isError: PropTypes.bool,
  onRetry: PropTypes.func,
  onAdd: PropTypes.func.isRequired,
  onEdit: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  addLabel: PropTypes.string.isRequired,
  entityLabel: PropTypes.string.isRequired,
  searchable: PropTypes.bool,
};
