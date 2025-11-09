import { Link } from "react-router-dom";
import "./manage_timetable.css";

function ManageTimetable(){
    return(
        <div className="manage-timetable-container">
            <header className="page-header">
                <h1>Manage Timetable</h1>
                <Link to="/admin" className="back-button">← Back to Dashboard</Link>
            </header>
            <div className="content-section">
                <div className="action-bar">
                    <button className="primary-button">Generate Timetable</button>
                    <select className="filter-select">
                        <option>All Departments</option>
                        <option>Computer Science</option>
                        <option>Mathematics</option>
                        <option>Physics</option>
                    </select>
                </div>
                <div className="timetable-grid">
                    <table className="data-table timetable-table">
                        <thead>
                            <tr>
                                <th>Time</th>
                                <th>Monday</th>
                                <th>Tuesday</th>
                                <th>Wednesday</th>
                                <th>Thursday</th>
                                <th>Friday</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td>9:00 AM</td>
                                <td colSpan="5" className="empty-state">No timetable generated yet.</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

export default ManageTimetable;
