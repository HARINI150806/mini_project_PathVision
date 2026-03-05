import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './StudentSidebar.css';

const features = [
  { label: 'Edit/View Profile', path: '/dashboard/student/profile' },
  { label: 'Aptitude & Interest Quizzes' },
  { label: 'Course-to-Career Mapping' },
  { label: 'Nearby Colleges', path: '/dashboard/student' },
  { label: 'Recommended Courses', path: '/dashboard/student#recommended-courses' },
  { label: 'Admissions & Scholarship Timeline' },
  { label: 'AI Recommendations' },
  { label: 'Decision Confidence Score' },
  { label: 'Skill Gap Analysis' },
  { label: 'Voice-Based Career Guidance' },
];

const StudentSidebar = ({ activeLabel }) => {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <aside className="student-sidebar">
      <div>
        <h2 className="student-sidebar-title">Student Features</h2>
        <ul className="student-feature-list">
          {features.map((item) => (
            <li key={item.label}>
              {item.path ? (
                <Link
                  to={item.path}
                  className={`student-feature-link ${activeLabel === item.label ? 'student-feature-active' : ''}`}
                >
                  {item.label}
                </Link>
              ) : (
                <span className={`student-feature-text ${activeLabel === item.label ? 'student-feature-active' : ''}`}>
                  {item.label}
                </span>
              )}
            </li>
          ))}
        </ul>
      </div>

      <button type="button" className="student-logout-btn" onClick={handleLogout}>
        Logout
      </button>
    </aside>
  );
};

export default StudentSidebar;
