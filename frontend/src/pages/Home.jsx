import React from 'react';
import './Home.css';



const Home = () => (
  <div className="home-hero-bg">
    <div className="home-hero">
      <div className="home-hero-content">
        <h1 className="home-hero-title">Your Personalized<br /><span>Path to Success</span></h1>
        <p className="home-hero-desc">
    

          Discover your strengths, explore career options, and find the perfect college with our AI-powered guidance platform designed for Indian students.
        </p>
        <div className="home-hero-cta">
          <a href="/register" className="home-btn home-btn-primary">Get Started Free</a>
          <a href="/login" className="home-btn home-btn-outline">Login</a>
        </div>
      </div>
      <div className="home-hero-img">
        <img src="https://images.unsplash.com/photo-1513258496099-48168024aec0?auto=format&fit=crop&w=600&q=80" alt="Classroom" />
      </div>
    </div>

    <section className="home-features">
      <h2 className="home-features-title">Everything You Need to Succeed</h2>
      <p className="home-features-desc">Comprehensive tools and resources to guide your educational journey</p>
      <div className="home-features-grid">
        <div className="home-feature-card">
          <div className="home-feature-icon">🎯</div>
          <div className="home-feature-title">Aptitude Quiz</div>
          <div className="home-feature-desc">Discover your strengths and interests through our comprehensive assessment</div>
        </div>
        <div className="home-feature-card">
          <div className="home-feature-icon">🎓</div>
          <div className="home-feature-title">College Directory</div>
          <div className="home-feature-desc">Explore government colleges with detailed information and admission criteria</div>
        </div>
        <div className="home-feature-card">
          <div className="home-feature-icon">🗺️</div>
          <div className="home-feature-title">Career Mapping</div>
          <div className="home-feature-desc">Get personalized career recommendations based on your aptitude and interests</div>
        </div>
        <div className="home-feature-card">
          <div className="home-feature-icon">📅</div>
          <div className="home-feature-title">Timeline Tracker</div>
          <div className="home-feature-desc">Never miss important admission deadlines and scholarship opportunities</div>
        </div>
      </div>
    </section>

    <section className="home-success">
      <div className="home-success-metrics">
        <div><span>500+</span><br />Government Colleges</div>
        <div><span>10,000+</span><br />Students Guided</div>
        <div><span>95%</span><br />Success Rate</div>
      </div>
      <h2 className="home-success-title">Success Stories</h2>
      <div className="home-success-cards">
        <div className="home-success-card">
          <div className="home-success-user">Priya Sharma<br /><span>B.Tech Computer Science</span></div>
          <div className="home-success-quote">"EduGuide helped me identify my passion for technology and guided me to the perfect college!"</div>
        </div>
        <div className="home-success-card">
          <div className="home-success-user">Rahul Verma<br /><span>B.Com Honors</span></div>
          <div className="home-success-quote">"The aptitude quiz revealed my business acumen. Now I'm pursuing my dream career in finance."</div>
        </div>
        <div className="home-success-card">
          <div className="home-success-user">Ananya Patel<br /><span>B.A. Psychology</span></div>
          <div className="home-success-quote">"The personalized recommendations were spot-on. I found the perfect course that matches my interests."</div>
        </div>
      </div>
    </section>

    <footer className="home-footer">
      <div className="home-footer-content">
        <div className="home-footer-brand">
          <span className="home-footer-logo">🎓</span> PathVision
        </div>
        <div className="home-footer-links">
          <div><b>Quick Links</b><br />
            <a href="/login">Login</a><br />
            <a href="/register">Sign Up</a>
          </div>
          <div><b>Resources</b><br />
            <a href="/colleges">Colleges</a><br />
            <a href="/courses">Courses</a><br />
            <a href="/assessment">Careers</a>
          </div>
          <div><b>Contact</b><br />
            Email: info@pathvision.com<br />
            Phone: +91 1234567890
          </div>
        </div>
      </div>
      <div className="home-footer-bottom">© 2026 PathVision. All rights reserved.</div>
    </footer>
  </div>
);

export default Home;
