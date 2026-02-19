import React from 'react';
import { Link } from 'react-router-dom';
import { FiArrowRight, FiClipboard, FiAward, FiTrendingUp } from 'react-icons/fi';
import './Home.css';

const Home = () => {
  return (
    <div className="home-container">
      <section className="hero-section">
        <div className="hero-content">
          <h1>
            Construct Your Future with <span className="highlight">PathVision</span>
          </h1>
          <p>
            Your AI-powered personalized career and education advisor. Discover the right path, tailored just for you.
          </p>
          <div className="cta-buttons">
            <Link to="/assessment" className="btn btn-primary btn-lg">
              Start Assessment <FiArrowRight className="ml-2" />
            </Link>
            <Link to="/courses" className="btn btn-outline btn-lg">
              Explore Courses
            </Link>
          </div>
        </div>
      </section>

      <section className="features-section">
        <div className="section-header">
          <h2>Why Choose PathVision?</h2>
          <p>We provide a comprehensive suite of tools to guide you every step of the way.</p>
        </div>
        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon">
              <FiClipboard />
            </div>
            <h3>AI Guidance</h3>
            <p>Get personalized course and career recommendations based on your unique interests and aptitude.</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">
              <FiAward />
            </div>
            <h3>College Finder</h3>
            <p>Locate nearby top-rated government colleges and access their detailed information with ease.</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon">
              <FiTrendingUp />
            </div>
            <h3>Career Mapping</h3>
            <p>Visualize your entire career trajectory, from initial course selection to future job opportunities.</p>
          </div>
        </div>
      </section>
    </div>
  );
};

export default Home;
