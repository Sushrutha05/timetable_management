import React from 'react';
import './admin_dashboard.css';

function MenuButtons({title, routing_page}){
    return(
        <a href={routing_page}>
            <button title={title}>{title}</button>
        </a>
        
    );
}

function AdminDashboard() {
    return (
        <div className='AdminDashboard'>
            <div className='admin-title'>
                <h1>Welcome to the Admin Dashboard</h1>
            </div>
             <div className='menu-buttons'>
                <MenuButtons title={"Manage Faculty"} routing_page={"./manage_faculty"}/>
                <MenuButtons title={"Manage Courses"} routing_page={"./manage_courses"}/>
                <MenuButtons title={"Manage Room & Resources"} routing_page={"./manage_room_resources"}/>
                <MenuButtons title={"Manage Timetable"} routing_page={"./manage_timetable"}/>
                <MenuButtons title={"Resolve Schedule Conflicts"} routing_page={"./resolve_schedule_conflicts"}/>
                <MenuButtons title={"Logout"} routing_page={"./"}/>
             </div>

        </div>
    );    
}

export default AdminDashboard;