import React from 'react';
import { Link } from 'react-router-dom';
import { useTheme } from '../../context/ThemeContext';
import Logo from '../common/Logo';
import './Navbar.css';

const Navbar = () => {
  const { theme, toggleTheme } = useTheme();

  return (
    <nav className="navbar">
      <div className="navbar-content">
        <div className="navbar-brand">
          <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
            <Logo width={32} height={32} />
            <span>PathVision</span>
          </Link>
        </div>
        <ul className="navbar-links">
          <li><Link to="/">Home</Link></li>
          <li><Link to="/assessment">Assessment</Link></li>
          <li><Link to="/colleges">Colleges</Link></li>
          <li><button onClick={toggleTheme} className="theme-toggle">
            {theme === 'light' ? '🌙' : '☀️'}
          </button></li>
          <li><Link to="/login" className="nav-btn">Login</Link></li>
        </ul>
      </div>
    </nav>
  );
};

export default Navbar;
