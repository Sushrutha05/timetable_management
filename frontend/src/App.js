import React, { useState } from 'react';
import LoginPage from './components/LoginPage';
import AdminDashboard from './components/AdminDashboard';
import FacultyDashboard from './components/FacultyDashboard';

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userRole, setUserRole] = useState(null); // 1 = Admin, 2 = Faculty
  const [isDarkMode, setIsDarkMode] = useState(false);

  const [facultyId, setFacultyId] = useState(null);
  const [deptId, setDeptId] = useState(null);

  const handleLogin = (data) => {
    setIsLoggedIn(true);
    setUserRole(data.role);
    if (data.role === 2) {
      setFacultyId(data.facultyId);
      setDeptId(data.deptId);
    }
  };

  const handleLogout = () => {
    setIsLoggedIn(false);
    setUserRole(null);
  };

  const toggleDarkMode = () => {
    setIsDarkMode(!isDarkMode);
  };

  // If not logged in, show login page
  if (!isLoggedIn) {
    return <LoginPage onLogin={handleLogin} />;
  }

  // Render appropriate dashboard based on user role
  if (userRole === 1) {
    return (
      <AdminDashboard
        isDarkMode={isDarkMode}
        toggleDarkMode={toggleDarkMode}
        onLogout={handleLogout}
      />
    );
  }

  if (userRole === 2) {
    return (
      <FacultyDashboard
        isDarkMode={isDarkMode}
        toggleDarkMode={toggleDarkMode}
        onLogout={handleLogout}
        facultyId={facultyId}
        deptId={deptId}
      />
    );
  }

  // Fallback (shouldn't reach here)
  return <LoginPage onLogin={handleLogin} />;
}

export default App;
