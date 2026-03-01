import React, { useState, useEffect } from 'react';
import Header from './Header';
import ManageDepartments from './admin/ManageDepartments';
import ManageFaculty from './admin/ManageFaculty';
import ManageCourses from './admin/ManageCourses';
import ManageRooms from './admin/ManageRooms';
import ManageSections from './admin/ManageSections';
import ManageDesignations from './admin/ManageDesignations';
import ManageTimeSlots from './admin/ManageTimeSlots';
import ManageOfferings from './admin/ManageOfferings';
import GenerateTimetable from './admin/GenerateTimetable';
import ViewTimetable from './admin/ViewTimetable';
import { departmentAPI } from '../utils/api';

// Pages that need a department context
const DEPT_SCOPED_PAGES = ['faculty', 'courses', 'sections', 'offerings', 'generate'];

const AdminDashboard = ({ isDarkMode, toggleDarkMode, onLogout, deptId: loginDeptId }) => {
  const [currentPage, setCurrentPage] = useState('departments');

  // If admin was created with a fixed department, use that; otherwise let them pick
  const [departments, setDepartments] = useState([]);
  const [selectedDeptId, setSelectedDeptId] = useState(loginDeptId || null);

  useEffect(() => {
    // Only need the full list if admin has no fixed dept
    if (!loginDeptId) {
      departmentAPI.getAll().then(setDepartments).catch(() => { });
    }
  }, [loginDeptId]);

  const effectiveDeptId = loginDeptId || selectedDeptId;

  const menuItems = [
    { id: 'departments', label: 'Manage Departments' },
    { id: 'faculty', label: 'Manage Faculty' },
    { id: 'courses', label: 'Manage Courses' },
    { id: 'rooms', label: 'Manage Rooms' },
    { id: 'sections', label: 'Manage Sections' },
    { id: 'designations', label: 'Manage Designations' },
    { id: 'timeslots', label: 'Manage Time Slots' },
    { id: 'offerings', label: 'Manage Course Offerings' },
    { id: 'generate', label: 'Generate Timetable' },
    { id: 'view', label: 'View Full Timetable' },
  ];

  const renderPage = () => {
    // Show a prompt if the page needs a department but none is selected
    if (DEPT_SCOPED_PAGES.includes(currentPage) && !effectiveDeptId) {
      return (
        <div className="flex flex-col items-center justify-center h-64 gap-4">
          <p className="text-gray-600 dark:text-gray-400 text-lg font-medium">
            Please select a department to continue.
          </p>
          {departments.length > 0 && (
            <select
              value={selectedDeptId || ''}
              onChange={(e) => setSelectedDeptId(Number(e.target.value))}
              className="px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
            >
              <option value="">-- Select Department --</option>
              {departments.map((d) => (
                <option key={d.id} value={d.id}>{d.name}</option>
              ))}
            </select>
          )}
        </div>
      );
    }

    switch (currentPage) {
      case 'departments':
        return <ManageDepartments />;
      case 'faculty':
        return <ManageFaculty deptId={effectiveDeptId} />;
      case 'courses':
        return <ManageCourses deptId={effectiveDeptId} />;
      case 'rooms':
        return <ManageRooms />;
      case 'sections':
        return <ManageSections />;
      case 'designations':
        return <ManageDesignations />;
      case 'timeslots':
        return <ManageTimeSlots />;
      case 'offerings':
        return <ManageOfferings />;
      case 'generate':
        return <GenerateTimetable />;
      case 'view':
        return <ViewTimetable />;
      default:
        return <ManageDepartments />;
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 dark:bg-gray-900">
      <Header isDarkMode={isDarkMode} toggleDarkMode={toggleDarkMode} onLogout={onLogout} />
      <div className="flex">
        {/* Sidebar */}
        <aside className="w-64 bg-white dark:bg-gray-800 shadow-sm min-h-[calc(100vh-4rem)] border-r border-gray-200 dark:border-gray-700 flex flex-col">
          <nav className="p-4 flex-1">
            <ul className="space-y-2">
              {menuItems.map((item) => (
                <li key={item.id}>
                  <button
                    onClick={() => setCurrentPage(item.id)}
                    className={`w-full text-left px-4 py-2 rounded-md transition-colors ${currentPage === item.id
                        ? 'bg-blue-600 text-white'
                        : 'text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700'
                      }`}
                  >
                    {item.label}
                  </button>
                </li>
              ))}
            </ul>
          </nav>

          {/* Department picker in sidebar — only shown for system admins without a fixed dept */}
          {!loginDeptId && departments.length > 0 && (
            <div className="p-4 border-t border-gray-200 dark:border-gray-700">
              <p className="text-xs text-gray-500 dark:text-gray-400 mb-1 font-medium uppercase tracking-wide">
                Active Department
              </p>
              <select
                value={selectedDeptId || ''}
                onChange={(e) => setSelectedDeptId(e.target.value ? Number(e.target.value) : null)}
                className="w-full px-2 py-1 text-sm border border-gray-300 dark:border-gray-600 rounded-md dark:bg-gray-700 dark:text-white"
              >
                <option value="">All / None</option>
                {departments.map((d) => (
                  <option key={d.id} value={d.id}>{d.name}</option>
                ))}
              </select>
            </div>
          )}
        </aside>

        {/* Main Content */}
        <main className="flex-1 p-6">
          {renderPage()}
        </main>
      </div>
    </div>
  );
};

export default AdminDashboard;
