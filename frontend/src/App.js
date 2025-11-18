import React, { useState } from 'react';
import LoginPage from './components/LoginPage';
import AdminDashboard from './components/AdminDashboard';
import FacultyDashboard from './components/FacultyDashboard';

function App() {
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userRole, setUserRole] = useState(null); // 1 = Admin, 2 = Faculty
  const [isDarkMode, setIsDarkMode] = useState(false);

  const handleLogin = (role) => {
    setIsLoggedIn(true);
    setUserRole(role);
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
      />
    );
  }

  // Fallback (shouldn't reach here)
  return <LoginPage onLogin={handleLogin} />;
}

export default App;
