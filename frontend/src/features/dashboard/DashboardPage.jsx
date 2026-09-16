// Landing page for the admin panel. Confirms the app shell renders and lists
// the scheduling-engine surfaces that upcoming stories will fill in.
export default function DashboardPage() {
  return (
    <section>
      <h1 className="page-title">Scheduling Engine — Admin Panel</h1>
      <p className="page-subtitle">
        Frontend project scaffold is running. Feature screens are delivered by their own stories.
      </p>

      <div className="card-grid">
        <article className="card">
          <h2>Engine Configuration</h2>
          <p>
            Manage session-derivation rules, soft-constraint weights, and institution common slots in
            data tables with add / edit / delete actions.
          </p>
        </article>
        <article className="card">
          <h2>Timetable Generation</h2>
          <p>
            Trigger generation, watch progress, and view the produced draft — sessions, scores, and the
            soft-constraint violations list.
          </p>
        </article>
      </div>
    </section>
  );
}
