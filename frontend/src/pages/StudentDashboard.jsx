import React from 'react';
import { Link } from 'react-router-dom';

const StudentDashboard = () => (
  <div className="dashboard student-dashboard">
    <h1>Student Dashboard</h1>
    <p>Welcome, Student! Your personalized career and education advisor is here.</p>
    <ul>
      <li><Link to="/dashboard/student/profile">Edit/View Profile</Link></li>
      <li>Take aptitude & interest quizzes for course suggestions.</li>
      <li>View course-to-career path mapping with visual charts.</li>
      <li>Find nearby government colleges with program details and facilities.</li>
      <li>Track important dates: admissions, scholarships, entrance tests.</li>
      <li>Get AI-driven recommendations for courses, colleges, and study materials.</li>
      <li>See your decision confidence score and skill gap analysis.</li>
      <li>Access voice-based career guidance in your language.</li>
    </ul>
  </div>
);

export default StudentDashboard;
