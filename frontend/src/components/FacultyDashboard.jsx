import React, { useState } from 'react';
import Header from './Header';
import SetPreferences from './faculty/SetPreferences';
import ViewMyTimetable from './faculty/ViewMyTimetable';

const FacultyDashboard = ({ isDarkMode, toggleDarkMode, onLogout }) => {
  const [currentPage, setCurrentPage] = useState('preferences');

  const menuItems = [
    { id: 'preferences', label: 'Set Preferences' },
    { id: 'timetable', label: 'View My Timetable' },
  ];

  const renderPage = () => {
    switch (currentPage) {
      case 'preferences':
        return <SetPreferences />;
      case 'timetable':
        return <ViewMyTimetable />;
      default:
        return <SetPreferences />;
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
                    className={`w-full text-left px-4 py-2 rounded-md transition-colors ${
                      currentPage === item.id
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

export default FacultyDashboard;

