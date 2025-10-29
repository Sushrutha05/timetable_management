import React  from 'react';
import {BrowserRouter as Router, Routes, Route} from 'react-router-dom';
import './App.css';
import Loginpage from './pages/login';
import AdminDashboard from './pages/admin_dashboard';

function App() {
  return (
    <Router>
      <Routes>
        <Route path='/' element={<Loginpage/>}/>
        <Route path='/admin' element={<AdminDashboard/>}/>
      </Routes>
    </Router>

  );
}

export default App;
