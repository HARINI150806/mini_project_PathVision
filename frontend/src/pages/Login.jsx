import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiLogOut, FiArrowRight } from 'react-icons/fi';
import './Login.css';
import endpoints from '../services/api.js';
import learningImage from '../assets/login-learning-hero.svg';

const Login = () => {
  const navigate = useNavigate();
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID || '';
  const [formData, setFormData] = useState({ email: '', password: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [isAlreadyLoggedIn, setIsAlreadyLoggedIn] = useState(false);
  const [googleReady, setGoogleReady] = useState(false);
  const [googleLoading, setGoogleLoading] = useState(false);

  useEffect(() => {
    const hasSession = !!localStorage.getItem('token') || !!localStorage.getItem('user');
    setIsAlreadyLoggedIn(hasSession);
  }, []);

  useEffect(() => {
    if (!googleClientId) return;
    const scriptId = 'google-identity-service';
    const initializeGoogle = () => {
      if (!window.google?.accounts?.id) return;
      window.google.accounts.id.initialize({
        client_id: googleClientId,
        callback: handleGoogleCredentialResponse,
      });
      const container = document.getElementById('google-login-button');
      if (container) {
        container.innerHTML = '';
        window.google.accounts.id.renderButton(container, {
          theme: 'outline',
          size: 'large',
          shape: 'rectangular',
          text: 'continue_with',
          width: 320,
        });
      }
      setGoogleReady(true);
    };

    const existing = document.getElementById(scriptId);
    if (existing) {
      initializeGoogle();
      return;
    }

    const script = document.createElement('script');
    script.id = scriptId;
    script.src = 'https://accounts.google.com/gsi/client';
    script.async = true;
    script.defer = true;
    script.onload = initializeGoogle;
    script.onerror = () => setError('Google Sign-In script failed to load.');
    document.head.appendChild(script);
  }, [googleClientId]);

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

  const persistAndNavigate = (data) => {
    const tokenValue = data && data.token ? data.token : null;
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
  };

  const handleGoogleCredentialResponse = async (credentialResponse) => {
    const idToken = credentialResponse?.credential;
    if (!idToken) {
      setError('Google sign-in failed. Missing token.');
      return;
    }
    setError('');
    setGoogleLoading(true);
    try {
      const response = await fetch(endpoints.googleLogin, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken }),
      });
      const data = await response.json();
      if (!response.ok) {
        setError(data?.message || 'Google login failed.');
        return;
      }
      persistAndNavigate(data);
    } catch {
      setError('Could not connect to the server for Google login.');
    } finally {
      setGoogleLoading(false);
    }
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
        persistAndNavigate(data);
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

            {googleClientId ? (
              <div id="google-login-button" style={{ display: 'flex', justifyContent: 'center', minHeight: 42 }} />
            ) : (
              <button type="button" className="login-google-btn" disabled>
                Configure VITE_GOOGLE_CLIENT_ID to enable Google login
              </button>
            )}
            {googleClientId && !googleReady && <p className="login-subtext">Loading Google Sign-In...</p>}
            {googleLoading && <p className="login-subtext">Signing in with Google...</p>}

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
