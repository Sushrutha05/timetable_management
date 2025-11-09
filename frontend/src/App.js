import {BrowserRouter as Router, Routes, Route} from 'react-router-dom';
import './App.css';
import Loginpage from './pages/login';
import AdminDashboard from './pages/admin_dashboard';
import ManageFaculty from './pages/manage_faculty';
import ManageCourses from './pages/manage_courses';
import ManageRoomResources from './pages/manage_room_resources';
import ManageTimetable from './pages/manage_timetable';
import ResolveScheduleConflict from './pages/resolve_schedule_conflict';

function App() {
  return (
    <Router>
      <Routes>
        <Route path='/' element={<Loginpage/>}/>
        <Route path='/admin' element={<AdminDashboard/>}/>
        <Route path='/manage_faculty' element={<ManageFaculty/>}/>
        <Route path='/manage_courses' element={<ManageCourses/>}/>
        <Route path='/manage_room_resources' element={<ManageRoomResources/>}/>
        <Route path='/manage_timetable' element={<ManageTimetable/>}/>
        <Route path='/resolve_schedule_conflicts' element={<ResolveScheduleConflict/>}/>
      </Routes>
    </Router>

  );
}

export default App;
