import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';

import { apiClient } from '@/lib/api-client';

import GenerateForm from './GenerateForm';

// The department is chosen by code (campus first) — no department ID is typed or shown,
// but the generate request still carries the department id.
const page = (rows) => ({ data: { data: rows, meta: { page: 0, totalPages: 1 } } });

function mockMasterData(campuses) {
  return vi.spyOn(apiClient, 'get').mockImplementation((url, { params } = {}) => {
    if (url === '/campuses') return Promise.resolve(page(campuses));
    if (url === '/departments' && params?.campusId === 1) {
      return Promise.resolve(page([{ id: 12, code: 'CSE', name: 'Computer Science' }]));
    }
    if (url === '/academic-calendars/campus/1') {
      return Promise.resolve({ data: { data: [{ academicYear: '2019', semesterIdentifier: 'ODD' }] } });
    }
    return Promise.resolve(page([]));
  });
}

function renderForm(onStarted = vi.fn()) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  render(
    <QueryClientProvider client={client}>
      <GenerateForm onStarted={onStarted} />
    </QueryClientProvider>,
  );
  return onStarted;
}

describe('GenerateForm', () => {
  const twoCampuses = [
    { id: 1, code: 'MAIN', name: 'Main Campus' },
    { id: 2, code: 'NORTH', name: 'North Campus' },
  ];

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('picks the department by code and submits its id', async () => {
    mockMasterData(twoCampuses);
    const post = vi.spyOn(apiClient, 'post').mockResolvedValue({ data: { requestId: 99 } });
    const onStarted = renderForm();

    const department = screen.getByRole('combobox', { name: 'Department' });
    expect(department).toBeDisabled();

    await screen.findByRole('option', { name: 'MAIN — Main Campus' });
    fireEvent.change(screen.getByRole('combobox', { name: 'Campus' }), { target: { value: '1' } });
    await screen.findByRole('option', { name: 'CSE — Computer Science' });

    fireEvent.change(department, { target: { value: '12' } });
    fireEvent.change(screen.getByLabelText(/^Semester/), { target: { value: 'ODD' } });
    fireEvent.change(screen.getByLabelText(/^Academic Year/), { target: { value: '2025-26' } });
    fireEvent.click(screen.getByRole('button', { name: 'Generate' }));

    await waitFor(() => expect(onStarted).toHaveBeenCalledWith(99));
    expect(post).toHaveBeenCalledWith('/timetables/generate', {
      departmentId: 12,
      semester: 'ODD',
      academicYear: '2025-26',
    });
    expect(screen.queryByText('12')).not.toBeInTheDocument();
  });

  it('uses the only campus automatically', async () => {
    mockMasterData([twoCampuses[0]]);
    renderForm();

    await screen.findByRole('option', { name: 'CSE — Computer Science' });
    expect(screen.getByRole('combobox', { name: 'Department' })).toBeEnabled();
  });

  it('explains which setup checks failed when generation cannot start', async () => {
    mockMasterData([twoCampuses[0]]);
    vi.spyOn(apiClient, 'post').mockRejectedValue({
      response: {
        status: 422,
        data: {
          message: 'Generation preconditions not met',
          details: [
            { check: 'ROOMS', message: 'No active rooms available for campus 1' },
            { check: 'ACADEMIC_CALENDAR', message: 'No academic calendar for campus 1, year 2019, semester Odd' },
          ],
        },
      },
    });
    renderForm();

    await screen.findByRole('option', { name: 'CSE — Computer Science' });
    fireEvent.change(screen.getByRole('combobox', { name: 'Department' }), { target: { value: '12' } });
    fireEvent.change(screen.getByLabelText(/^Semester/), { target: { value: 'Odd' } });
    fireEvent.change(screen.getByLabelText(/^Academic Year/), { target: { value: '2019' } });
    fireEvent.click(screen.getByRole('button', { name: 'Generate' }));

    expect(await screen.findByText('Generation cannot start until these are fixed:')).toBeInTheDocument();
    expect(screen.getByText('This campus has no rooms.')).toBeInTheDocument();
    expect(await screen.findByText(/Calendars on this campus \(academic year \/ semester\): 2019 \/ ODD/)).toBeInTheDocument();
    expect(screen.queryByText(/campus 1/)).not.toBeInTheDocument();
  });

  it('asks for a department instead of submitting when none is selected', async () => {
    mockMasterData(twoCampuses);
    const post = vi.spyOn(apiClient, 'post');
    renderForm();

    fireEvent.change(screen.getByLabelText(/^Semester/), { target: { value: 'ODD' } });
    fireEvent.change(screen.getByLabelText(/^Academic Year/), { target: { value: '2025-26' } });
    fireEvent.click(screen.getByRole('button', { name: 'Generate' }));

    expect(await screen.findByText('Please select a department')).toBeInTheDocument();
    expect(post).not.toHaveBeenCalled();
  });
});
