import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import App from './App';

describe('Admin Dashboard Routing', () => {
  test('navigates to Manage Faculty screen from admin dashboard', async () => {
    const { container } = render(
      <MemoryRouter initialEntries={['/admin']}>
        <App />
      </MemoryRouter>
    );
    
    const manageFacultyButton = screen.getByRole('button', { name: /Manage Faculty/i });
    fireEvent.click(manageFacultyButton);
    
    await waitFor(() => {
      expect(screen.getByText(/Manage Faculty/i)).toBeInTheDocument();
    });
  });

  test('navigates to Manage Courses screen from admin dashboard', async () => {
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <App />
      </MemoryRouter>
    );
    
    const manageCoursesButton = screen.getByRole('button', { name: /Manage Courses/i });
    fireEvent.click(manageCoursesButton);
    
    await waitFor(() => {
      expect(screen.getByText(/Manage Courses/i)).toBeInTheDocument();
    });
  });

  test('navigates to Manage Room & Resources screen from admin dashboard', async () => {
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <App />
      </MemoryRouter>
    );
    
    const manageRoomButton = screen.getByRole('button', { name: /Manage Room & Resources/i });
    fireEvent.click(manageRoomButton);
    
    await waitFor(() => {
      expect(screen.getByText(/Manage Room & Resources/i)).toBeInTheDocument();
    });
  });

  test('navigates to Manage Timetable screen from admin dashboard', async () => {
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <App />
      </MemoryRouter>
    );
    
    const manageTimetableButton = screen.getByRole('button', { name: /Manage Timetable/i });
    fireEvent.click(manageTimetableButton);
    
    await waitFor(() => {
      expect(screen.getByText(/Manage Timetable/i)).toBeInTheDocument();
    });
  });

  test('navigates to Resolve Schedule Conflicts screen from admin dashboard', async () => {
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <App />
      </MemoryRouter>
    );
    
    const resolveConflictsButton = screen.getByRole('button', { name: /Resolve Schedule Conflicts/i });
    fireEvent.click(resolveConflictsButton);
    
    await waitFor(() => {
      expect(screen.getByText(/Resolve Schedule Conflicts/i)).toBeInTheDocument();
    });
  });

  test('logout button navigates back to login page', async () => {
    render(
      <MemoryRouter initialEntries={['/admin']}>
        <App />
      </MemoryRouter>
    );
    
    const logoutButton = screen.getByRole('button', { name: /Logout/i });
    fireEvent.click(logoutButton);
    
    await waitFor(() => {
      // Login page should be rendered (checking for login-specific elements)
      expect(screen.queryByText(/Welcome to the Admin Dashboard/i)).not.toBeInTheDocument();
    });
  });

  test('direct URL access to /manage_faculty renders ManageFaculty component', () => {
    render(
      <MemoryRouter initialEntries={['/manage_faculty']}>
        <App />
      </MemoryRouter>
    );
    
    expect(screen.getByText(/Manage Faculty/i)).toBeInTheDocument();
  });

  test('direct URL access to /manage_courses renders ManageCourses component', () => {
    render(
      <MemoryRouter initialEntries={['/manage_courses']}>
        <App />
      </MemoryRouter>
    );
    
    expect(screen.getByText(/Manage Courses/i)).toBeInTheDocument();
  });

  test('direct URL access to /manage_room_resources renders ManageRoomResources component', () => {
    render(
      <MemoryRouter initialEntries={['/manage_room_resources']}>
        <App />
      </MemoryRouter>
    );
    
    expect(screen.getByText(/Manage Room & Resources/i)).toBeInTheDocument();
  });

  test('direct URL access to /manage_timetable renders ManageTimetable component', () => {
    render(
      <MemoryRouter initialEntries={['/manage_timetable']}>
        <App />
      </MemoryRouter>
    );
    
    expect(screen.getByText(/Manage Timetable/i)).toBeInTheDocument();
  });

  test('direct URL access to /resolve_schedule_conflicts renders ResolveScheduleConflict component', () => {
    render(
      <MemoryRouter initialEntries={['/resolve_schedule_conflicts']}>
        <App />
      </MemoryRouter>
    );
    
    expect(screen.getByText(/Resolve Schedule Conflicts/i)).toBeInTheDocument();
  });
});
