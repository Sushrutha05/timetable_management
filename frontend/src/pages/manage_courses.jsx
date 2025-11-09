import { Link } from "react-router-dom";
import "./manage_courses.css";

function ManageCourses(){
    return(
        <div className="manage-courses-container">
            <header className="page-header">
                <h1>Manage Courses</h1>
                <Link to="/admin" className="back-button">← Back to Dashboard</Link>
            </header>
            <div className="content-section">
                <div className="action-bar">
                    <button className="primary-button">Add New Course</button>
                    <input type="search" placeholder="Search courses..." className="search-input" />
                </div>
                <div className="courses-list">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Course Code</th>
                                <th>Course Name</th>
                                <th>Credits</th>
                                <th>Department</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td colSpan="5" className="empty-state">No courses found. Click "Add New Course" to get started.</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

export default ManageCourses;
