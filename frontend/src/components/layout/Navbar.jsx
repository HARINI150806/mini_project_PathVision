
import React from 'react';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useTheme } from '../../context/ThemeContext';
import Logo from '../common/Logo';
import BrightnessIcon from "../../assets/brightness.png";


import './Navbar.css';

const getDashboardRoute = (role) => {
  if (!role) return '/';
  if (role.toLowerCase() === 'admin') return '/dashboard/admin';
  if (role.toLowerCase() === 'professional') return '/dashboard/professional';
  return '/dashboard/student';
};

const getDashboardName = (role) => {
  if (!role) return '';
  if (role.toLowerCase() === 'admin') return 'Admin Dashboard';
  if (role.toLowerCase() === 'professional') return 'Professional Dashboard';
  return 'Student Dashboard';
};

const Navbar = () => {
  const { theme, toggleTheme } = useTheme();
  const navigate = useNavigate();
  const location = useLocation();
  const user = JSON.parse(localStorage.getItem('user'));

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

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
            {theme === 'light' ? <div className="home-feature-icon">
  <img src={BrightnessIcon} alt="Brightness Icon" />
</div>
 : '☀️'}
          </button></li>
          {user ? (
            <>
              <li>
                <Link to={getDashboardRoute(user.role)} className="nav-btn dashboard-btn">
                  {getDashboardName(user.role)}
                </Link>
              </li>
              <li style={{fontWeight:'bold',color:'#2a4d8f'}}>{user.name}</li>
              <li><button onClick={handleLogout} className="nav-btn logout-btn">Logout</button></li>
            </>
          ) : (
            <li><Link to="/login" className="nav-btn">Login</Link></li>
          )}
        </ul>
      </div>
    </nav>
  );
};

export default Navbar;
