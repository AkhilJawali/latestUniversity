// A4-340 §5.6 — maps the backend standard error envelope to something the UI can
// render, without ever surfacing stack traces or internal detail (NFR-2 / FR-7.1).
//
// Backend envelope: { timestamp, status, error, message, path, details? }
// where details = [{ field, message, rejectedValue }]
//
// Returns either { fields: { [field]: message } } for 400 field errors,
// or { message } for everything else.
export function mapApiError(error) {
  const res = error?.response?.data;
  if (res?.details?.length) {
    return {
      fields: Object.fromEntries(
        res.details
          .filter((d) => d && d.field)
          .map((d) => [d.field, d.message || 'Invalid value']),
      ),
    };
  }
  return { message: res?.message || 'Something went wrong. Please try again.' };
}
