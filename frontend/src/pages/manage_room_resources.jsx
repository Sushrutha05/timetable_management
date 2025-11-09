import { Link } from "react-router-dom";
import "./manage_room_resources.css";

function ManageRoomResources(){
    return(
        <div className="manage-room-resources-container">
            <header className="page-header">
                <h1>Manage Room & Resources</h1>
                <Link to="/admin" className="back-button">← Back to Dashboard</Link>
            </header>
            <div className="content-section">
                <div className="action-bar">
                    <button className="primary-button">Add New Room</button>
                    <input type="search" placeholder="Search rooms..." className="search-input" />
                </div>
                <div className="rooms-list">
                    <table className="data-table">
                        <thead>
                            <tr>
                                <th>Room Number</th>
                                <th>Building</th>
                                <th>Capacity</th>
                                <th>Resources</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr>
                                <td colSpan="5" className="empty-state">No rooms found. Click "Add New Room" to get started.</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    );
}

export default ManageRoomResources;
