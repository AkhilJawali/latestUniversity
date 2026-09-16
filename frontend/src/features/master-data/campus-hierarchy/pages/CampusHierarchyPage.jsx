import { useState } from 'react';

import BatchTab from '../components/BatchTab';
import CampusTab from '../components/CampusTab';
import DepartmentTab from '../components/DepartmentTab';
import ProgramTab from '../components/ProgramTab';
import SectionTab from '../components/SectionTab';
import '../campus-hierarchy.css';

// A4-410 §5.1 (PD-4) — drill-down orchestrator. Each level's selection sets the
// parent id for the next level and clears deeper selections. Levels appear only
// once their parent is selected, so the admin walks Campus -> Dept -> Program ->
// Batch -> Section. This satisfies FR-1 without a bespoke tree widget.
export default function CampusHierarchyPage() {
  const [campus, setCampus] = useState(null);
  const [department, setDepartment] = useState(null);
  const [program, setProgram] = useState(null);
  const [batch, setBatch] = useState(null);

  const selectCampus = (row) => {
    setCampus(row);
    setDepartment(null);
    setProgram(null);
    setBatch(null);
  };
  const selectDepartment = (row) => {
    setDepartment(row);
    setProgram(null);
    setBatch(null);
  };
  const selectProgram = (row) => {
    setProgram(row);
    setBatch(null);
  };
  const selectBatch = (row) => {
    setBatch(row);
  };

  return (
    <section className="campus-hierarchy">
      <h1 className="page-title">Campus Hierarchy</h1>
      <p className="page-subtitle">
        Manage the institution structure: Campus &rarr; Department &rarr; Program &rarr; Batch &rarr; Section.
      </p>

      <nav className="breadcrumb" aria-label="Hierarchy selection">
        <span className="crumb crumb--active">Campuses</span>
        {campus && <span className="crumb">{campus.name}</span>}
        {department && <span className="crumb">{department.name}</span>}
        {program && <span className="crumb">{program.name}</span>}
        {batch && <span className="crumb">{batch.yearIdentifier}</span>}
      </nav>

      <div className="hierarchy-levels">
        <CampusTab selectedId={campus?.id} onSelect={selectCampus} />

        {campus && (
          <DepartmentTab
            campusId={campus.id}
            selectedId={department?.id}
            onSelect={selectDepartment}
          />
        )}

        {department && (
          <ProgramTab
            departmentId={department.id}
            selectedId={program?.id}
            onSelect={selectProgram}
          />
        )}

        {program && (
          <BatchTab programId={program.id} selectedId={batch?.id} onSelect={selectBatch} />
        )}

        {batch && <SectionTab batchId={batch.id} />}
      </div>
    </section>
  );
}
