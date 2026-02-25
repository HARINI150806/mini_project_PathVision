import React from 'react';
import './AdminDashboard.css';

const AdminDashboard = () => (
  <div className="admin-dashboard-container">
    <div className="admin-dashboard-title">Admin Dashboard</div>
    <div className="admin-dashboard-welcome">
      Welcome, <b>Admin</b>! Here you can manage users, view analytics, and oversee the platform.
    </div>
    <div className="admin-dashboard-section">
      <ul className="admin-dashboard-list">
        <li><b>Real-time analytics:</b> Track usage, app suggestions, and transitions to college enrollment.</li>
        <li><b>Manage content:</b> Collaborate with education departments, teachers, NGOs, and counselors.</li>
        <li><b>Monitor feedback:</b> Review feedback from students and teachers for continuous improvement.</li>
        <li><b>System monitoring:</b> Ensure platform scalability and reliability.</li>
      </ul>
    </div>
  </div>
);

export default AdminDashboard;
