import React, { useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import './StudentDashboard.css';

const features = [
  { label: 'Edit/View Profile', path: '/dashboard/student/profile' },
  { label: 'Aptitude & Interest Quizzes' },
  { label: 'Course-to-Career Mapping' },
  { label: 'Nearby Government Colleges' },
  { label: 'Admissions & Scholarship Timeline' },
  { label: 'AI Recommendations' },
  { label: 'Decision Confidence Score' },
  { label: 'Skill Gap Analysis' },
  { label: 'Voice-Based Career Guidance' },
];

const StudentDashboard = () => {
  const navigate = useNavigate();

  useEffect(() => {
    const hasSession = !!localStorage.getItem('token') || !!localStorage.getItem('user');
    if (!hasSession) {
      navigate('/login');
    }
  }, [navigate]);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <div className="student-dashboard-page">
      <aside className="student-sidebar">
        <div>
          <h2 className="student-sidebar-title">Student Features</h2>
          <ul className="student-feature-list">
            {features.map((item) => (
              <li key={item.label}>
                {item.path ? (
                  <Link to={item.path} className="student-feature-link">
                    {item.label}
                  </Link>
                ) : (
                  <span className="student-feature-text">{item.label}</span>
                )}
              </li>
            ))}
          </ul>
        </div>

        <button type="button" className="student-logout-btn" onClick={handleLogout}>
          Logout
        </button>
      </aside>

      <section className="student-main-panel">
        <h1>Student Dashboard</h1>
        <p>Welcome, Student! Your personalized career and education advisor is here.</p>
        <p>Select a feature from the sidebar to continue.</p>
      </section>
    </div>
  );
};

export default StudentDashboard;
