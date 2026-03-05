import React from 'react';
import { useNavigate } from 'react-router-dom';
import './ProfessionalDashboard.css';

const professionalFeatures = [
  'Skill Gap to Job Mapping',
  'Personalized Course Recommendations',
  'Certification Path Planner',
  'Progress Tracker',
  'AI Explanation Assistant',
  'Voice-Based Guidance',
];

const ProfessionalDashboard = () => {
  const navigate = useNavigate();

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  return (
    <div className="professional-page">
      <aside className="professional-sidebar">
        <div>
          <h2>Professional Features</h2>
          <ul className="professional-feature-list">
            {professionalFeatures.map((feature) => (
              <li key={feature}>
                <span>{feature}</span>
              </li>
            ))}
          </ul>
        </div>
        <button type="button" className="professional-logout-btn" onClick={handleLogout}>
          Logout
        </button>
      </aside>

      <main className="professional-main">
        <h1>Professional Learner Dashboard</h1>
        <p>
          Continue your lifelong learning journey with AI-assisted upskilling, role-based recommendations,
          and measurable career progress.
        </p>
      </main>
    </div>
  );
};

export default ProfessionalDashboard;
