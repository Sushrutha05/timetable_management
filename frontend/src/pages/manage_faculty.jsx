import { Link } from "react-router-dom";
import "./manage_faculty.css";

function ManageFaculty(){
    return(
        <div className="manage-faculty-container">
            <header className="page-header">
                <h1>Manage Faculty</h1>
                <Link to="/admin" className="back-button">← Back to Dashboard</Link>
            </header>
            <div className="content-section">
                <div className="action-bar">
                    <button className="primary-button">Add New Faculty</button>
                    <input type="search" placeholder="Search faculty..." className="search-input" />
                </div>
                <div className="faculty-list">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Faculty ID</th>
                                <th>Name</th>
                                <th>Department</th>
                                <th>Email</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td colSpan="5" className="empty-state">No faculty members found. Click "Add New Faculty" to get started.</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

export default ManageFaculty; 