import { Link } from "react-router-dom";
import "./resolve_schedule_conflict.css";

function ResolveScheduleConflict(){
    return(
        <div className="resolve-conflicts-container">
            <header className="page-header">
                <h1>Resolve Schedule Conflicts</h1>
                <Link to="/admin" className="back-button">← Back to Dashboard</Link>
            </header>
            <div className="content-section">
                <div className="action-bar">
                    <button className="primary-button">Scan for Conflicts</button>
                    <div className="conflict-summary">
                        <span className="conflict-badge">0 Conflicts Found</span>
                    </div>
                </div>
                <div className="conflicts-list">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Conflict Type</th>
                                <th>Course/Faculty</th>
                                <th>Time Slot</th>
                                <th>Room</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td colSpan="5" className="empty-state">No conflicts detected. Click "Scan for Conflicts" to check the schedule.</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

export default ResolveScheduleConflict;
