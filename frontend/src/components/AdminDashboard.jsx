import React, { useState } from 'react';
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

const AdminDashboard = ({ isDarkMode, toggleDarkMode, onLogout, deptId }) => {
  const [currentPage, setCurrentPage] = useState('departments');

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
    switch (currentPage) {
      case 'departments':
        return <ManageDepartments />;
      case 'faculty':
        return <ManageFaculty deptId={deptId} />;
      case 'courses':
        return <ManageCourses deptId={deptId} />;
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
        <aside className="w-64 bg-white dark:bg-gray-800 shadow-sm min-h-[calc(100vh-4rem)] border-r border-gray-200 dark:border-gray-700">
          <nav className="p-4">
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

