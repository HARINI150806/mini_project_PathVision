import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiLogOut, FiArrowRight } from 'react-icons/fi';
import './Login.css';
import endpoints from '../services/api.js';
import learningImage from '../assets/login-learning-hero.svg';

const Login = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [isAlreadyLoggedIn, setIsAlreadyLoggedIn] = useState(false);

  useEffect(() => {
    const hasSession = !!localStorage.getItem('token') || !!localStorage.getItem('user');
    setIsAlreadyLoggedIn(hasSession);
  }, []);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setIsAlreadyLoggedIn(false);
    setError('Logged out successfully.');
  };

  const navigateByRole = (role) => {
    if (role && role.toLowerCase() === 'admin') navigate('/dashboard/admin');
    else if (role && role.toLowerCase() === 'professional') navigate('/dashboard/professional');
    else navigate('/dashboard/student');
  };

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const response = await fetch(endpoints.login, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });

      let data = null;
      let text = null;
      try {
        data = await response.json();
      } catch {
        try {
          text = await response.text();
        } catch {
          text = null;
        }
      }

      if (response.ok) {
        const tokenValue = data && data.token ? data.token : text || null;
        if (tokenValue) {
          localStorage.setItem('token', tokenValue);
          localStorage.setItem(
            'user',
            JSON.stringify({
              name: data && data.name ? data.name : '',
              role: data && data.role ? data.role : '',
            }),
          );
        }
        navigateByRole(data && data.role ? data.role : '');
      } else {
        const message = data && data.message ? data.message.toString() : text ? text.toString() : '';
        const lower = message.toLowerCase();
        if (response.status === 404 || lower.includes('not registered') || lower.includes('this email')) {
          setError('This email is not registered');
        } else if (lower.includes('verify')) {
          setError('Account not verified. Please check your email for the verification code.');
        } else if (lower.includes('incorrect password') || lower.includes('bad credentials')) {
          setError('Incorrect password. Please try again.');
        } else {
          setError(message || 'Invalid email or password.');
        }
      }
    } catch {
      setError('Could not connect to the server. Is it running?');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <section className="login-illustration">
          <div className="login-illustration-overlay" />
          <img className="login-illustration-image" src={learningImage} alt="Learning illustration" />
        </section>

        <section className="login-panel">
          <div className="login-panel-inner">
            <h1>LOGIN</h1>

            <p className="login-subtext">Please enter your details</p>
            {error && <div className="login-alert">{error}</div>}

            {isAlreadyLoggedIn && (
              <div className="login-session-actions">
                <button type="button" className="session-btn session-logout" onClick={handleLogout}>
                  <FiLogOut /> Logout
                </button>
                <button
                  type="button"
                  className="session-btn session-dashboard"
                  onClick={() => navigate('/dashboard/student')}
                >
                  Dashboard <FiArrowRight />
                </button>
              </div>
            )}

            <button type="button" className="login-google-btn">
              <svg viewBox="0 0 24 24" width="20" height="20" xmlns="http://www.w3.org/2000/svg">
                <g transform="matrix(1, 0, 0, 1, 27.009001, -39.238998)">
                  <path fill="#4285F4" d="M -3.264 51.509 C -3.264 50.719 -3.334 49.969 -3.454 49.239 L -14.754 49.239 L -14.754 53.749 L -8.284 53.749 C -8.574 55.229 -9.424 56.479 -10.684 57.329 L -10.684 60.329 L -6.824 60.329 C -4.564 58.239 -3.264 55.159 -3.264 51.509 Z"/>
                  <path fill="#34A853" d="M -14.754 63.239 C -11.514 63.239 -8.804 62.159 -6.824 60.329 L -10.684 57.329 C -11.764 58.049 -13.134 58.489 -14.754 58.489 C -17.884 58.489 -20.534 56.379 -21.484 53.529 L -25.464 53.529 L -25.464 56.619 C -23.494 60.539 -19.444 63.239 -14.754 63.239 Z"/>
                  <path fill="#FBBC05" d="M -21.484 53.529 C -21.734 52.809 -21.864 52.039 -21.864 51.239 C -21.864 50.439 -21.734 49.669 -21.484 48.949 L -21.484 45.859 L -25.464 45.859 C -26.284 47.479 -26.754 49.299 -26.754 51.239 C -26.754 53.179 -26.284 54.999 -25.464 56.619 L -21.484 53.529 Z"/>
                  <path fill="#EA4335" d="M -14.754 43.989 C -12.984 43.989 -11.404 44.599 -10.154 45.789 L -6.734 42.369 C -8.804 40.429 -11.514 39.239 -14.754 39.239 C -19.444 39.239 -23.494 41.939 -25.464 45.859 L -21.484 48.949 C -20.534 46.099 -17.884 43.989 -14.754 43.989 Z"/>
                </g>
              </svg>
              Continue with Google
            </button>

            <div className="login-divider">
              <span>or</span>
            </div>

            <form onSubmit={handleLogin} className="login-form-fields">
              <label htmlFor="email">Email address</label>
              <input
                id="email"
                type="email"
                name="email"
                value={formData.email}
                onChange={handleChange}
                required
              />

              <label htmlFor="password">Password</label>
              <input
                id="password"
                type="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                required
              />

              <button type="submit" className="login-submit-btn" disabled={loading}>
                {loading ? 'Signing in...' : 'Login'}
              </button>

              <div className="login-remember-row">
                <label className="remember">
                  <input type="checkbox" /> Remember for 30 days
                </label>
                <a href="#" className="forgot">Forgot password</a>
              </div>
            </form>

            <p className="login-new-user">
              Don't have an account? <Link to="/register">Sign up</Link>
            </p>
          </div>
        </section>
      </div>
    </div>
  );
};

export default Login;
