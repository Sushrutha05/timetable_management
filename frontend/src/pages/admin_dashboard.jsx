import React from 'react';
import './admin_dashboard.css';

function MenuButtons({title}){
    return(
        <button title={title}>{title}</button>
    );
}

function AdminDashboard() {
    return (
        <div className='AdminDashboard'>
            <div className='admin-title'>
                <h1>Welcome to the Admin Dashboard</h1>
            </div>
             <div className='menu-buttons'>
                <MenuButtons title={"Manage Faculty"}/>
                <MenuButtons title={"Manage Courses"}/>
                <MenuButtons title={"Manage Room & Resources"}/>
                <MenuButtons title={"Manage Timetable"}/>
                <MenuButtons title={"Resolve Schedule Conflicts"}/>
                <MenuButtons title={"Logout"}/>
             </div>

        </div>
    );    
}

export default AdminDashboard;