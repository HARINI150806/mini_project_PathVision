import React from 'react';
import { Link } from 'react-router-dom';
import './Home.css';

const Home = () => {
  return (
    <div className="home-container">
      <section className="hero-section">
        <div className="hero-content">
          <h1>Construct Your Future with <span className="highlight">PathVision</span></h1>
          <p>Your AI-powered personalized career and education advisor. Discover the right path for you.</p>
          <div className="cta-buttons">
            <Link to="/assessment" className="btn btn-primary">Start Assessment</Link>
            <Link to="/courses" className="btn btn-secondary">Explore Courses</Link>
          </div>
        </div>
      </section>

      <section className="features-section">
        <h2>Why Choose PathVision?</h2>
        <div className="features-grid">
          <div className="feature-card">
            <h3>🤖 AI Guidance</h3>
            <p>Get personalized recommendations based on your interests and aptitude.</p>
          </div>
          <div className="feature-card">
            <h3>🎓 College Finder</h3>
            <p>Locate nearby government colleges and view their details easily.</p>
          </div>
          <div className="feature-card">
            <h3>📈 Career Mapping</h3>
            <p>Visualize your career path from course selection to job opportunities.</p>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Home;
