import { Link } from 'react-router-dom';
import './admin_dashboard.css';

function MenuCard({ title, routing_page, icon, color }) {
    return (
        <Link to={routing_page} className="menu-card" style={{ '--card-color': color }}>
            <div className="card-icon">{icon}</div>
            <h3 className="card-title">{title}</h3>
            <div className="card-arrow">→</div>
        </Link>
    );
}

function AdminDashboard() {
    return (
        <div className="admin-dashboard">
            <header className="dashboard-header">
                <div className="header-content">
                    <h1>Admin Dashboard</h1>
                    <p className="header-subtitle">Manage your institution's resources and schedules</p>
                </div>
                <Link to="/" className="logout-button">
                    <span>Logout</span>
                    <span className="logout-icon">⎋</span>
                </Link>
            </header>

            <div className="dashboard-grid">
                <MenuCard 
                    title="Manage Faculty" 
                    routing_page="/manage_faculty"
                    icon="👨‍🏫"
                    color="#667eea"
                />
                <MenuCard 
                    title="Manage Courses" 
                    routing_page="/manage_courses"
                    icon="📚"
                    color="#f5576c"
                />
                <MenuCard 
                    title="Manage Room & Resources" 
                    routing_page="/manage_room_resources"
                    icon="🏢"
                    color="#4facfe"
                />
                <MenuCard 
                    title="Manage Timetable" 
                    routing_page="/manage_timetable"
                    icon="📅"
                    color="#43e97b"
                />
                <MenuCard 
                    title="Resolve Schedule Conflicts" 
                    routing_page="/resolve_schedule_conflicts"
                    icon="⚠️"
                    color="#fa709a"
                />
            </div>

            <footer className="dashboard-footer">
                <p>© 2024 Admin Dashboard System</p>
            </footer>
        </div>
    );
}

export default AdminDashboard;