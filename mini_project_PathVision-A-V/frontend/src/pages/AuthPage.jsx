import React, { useState, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { FiLogOut, FiArrowRight } from 'react-icons/fi';
import './AuthPage.css';
import endpoints from '../services/api.js';
import learningImage from '../assets/login-learning-hero.svg';

const AuthPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const [isFlipped, setIsFlipped] = useState(false);
  
  // Login state
  const [loginData, setLoginData] = useState({ email: '', password: '' });
  const [loginError, setLoginError] = useState('');
  const [loginLoading, setLoginLoading] = useState(false);
  const [isAlreadyLoggedIn, setIsAlreadyLoggedIn] = useState(false);
  
  // Register state
  const [registerData, setRegisterData] = useState({
    fullName: '',
    email: '',
    password: '',
    role: 'student'
  });
  const [verificationCode, setVerificationCode] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [message, setMessage] = useState('');
  const [registerError, setRegisterError] = useState('');
  const [registerLoading, setRegisterLoading] = useState(false);

  useEffect(() => {
    const hasSession = !!localStorage.getItem('token') || !!localStorage.getItem('user');
    setIsAlreadyLoggedIn(hasSession);
  }, []);

  useEffect(() => {
    if (location.pathname === '/register') {
      setIsFlipped(true);
    } else {
      setIsFlipped(false);
    }
  }, [location.pathname]);

  const flipToRegister = () => {
    navigate('/register');
  };

  const flipToLogin = () => {
    navigate('/login');
  };

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setIsAlreadyLoggedIn(false);
    setLoginError('Logged out successfully.');
  };

  const navigateByRole = (role) => {
    if (role && role.toLowerCase() === 'admin') navigate('/dashboard/admin');
    else if (role && role.toLowerCase() === 'professional') navigate('/dashboard/professional');
    else navigate('/dashboard/student');
  };

  const handleLoginChange = (e) => {
    setLoginData({ ...loginData, [e.target.name]: e.target.value });
  };

  const handleLogin = async (e) => {
    e.preventDefault();
    setLoginError('');
    setLoginLoading(true);
    try {
      const response = await fetch(endpoints.login, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(loginData),
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
          setLoginError('This email is not registered');
        } else if (lower.includes('verify')) {
          setLoginError('Account not verified. Please check your email for the verification code.');
        } else if (lower.includes('incorrect password') || lower.includes('bad credentials')) {
          setLoginError('Incorrect password. Please try again.');
        } else {
          setLoginError(message || 'Invalid email or password.');
        }
      }
    } catch {
      setLoginError('Could not connect to the server. Is it running?');
    } finally {
      setLoginLoading(false);
    }
  };

  const handleRegisterChange = (e) => {
    setRegisterData({ ...registerData, [e.target.name]: e.target.value });
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setRegisterError('');
    setMessage('');
    setRegisterLoading(true);

    try {
      const response = await fetch('http://localhost:8080/api/auth/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(registerData)
      });

      const data = await response.json();

      if (response.ok) {
        setMessage(data.message || 'Verification code sent to your email.');
        setIsVerifying(true);
      } else {
        if (data.message && data.message.toLowerCase().includes('already in use')) {
          setRegisterError('This email is already registered. Please log in or use a different email.');
        } else {
          setRegisterError(data.message || 'Registration failed. Please try again.');
        }
      }
    } catch (err) {
      setRegisterError('Could not connect to the server. Is it running?');
    } finally {
      setRegisterLoading(false);
    }
  };

  const handleVerify = async (e) => {
    e.preventDefault();
    setRegisterError('');
    setMessage('');
    setRegisterLoading(true);

    try {
      const response = await fetch('http://localhost:8080/api/auth/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: registerData.email,
          verificationCode: verificationCode
        })
      });

      if (response.ok) {
        setMessage('Verification successful! Redirecting to login...');
        setTimeout(() => {
          setIsVerifying(false);
          flipToLogin();
        }, 2000);
      } else {
        const data = await response.text();
        try {
          const jsonData = JSON.parse(data);
          setRegisterError(jsonData.message || 'Invalid or expired verification code.');
        } catch {
          setRegisterError(data || 'Invalid or expired verification code.');
        }
      }
    } catch (err) {
      setRegisterError('Verification failed. Please try again.');
    } finally {
      setRegisterLoading(false);
    }
  };

  const handleGoogleSignIn = () => {
    alert('Google Sign-In would be implemented here');
  };

  return (
    <div className="flip-auth-page">
      <div className="flip-auth-card">
        {/* Left Side - Illustration */}
        <section className="flip-auth-illustration">
          <div className="flip-auth-illustration-overlay" />
          <img className="flip-auth-illustration-image" src={learningImage} alt="Learning illustration" />
        </section>

        {/* Right Side - Sliding Forms */}
        <div className="flip-auth-slider">
          <div className={`flip-auth-slider-track ${isFlipped ? 'flipped' : ''}`}>
            {/* Login Panel */}
            <div className="flip-auth-panel">
              <div className="flip-auth-form-container">
                <h1>LOGIN</h1>
                <p className="flip-auth-subtext">Welcome back! Please enter your details</p>
                {loginError && <div className="flip-auth-alert error">{loginError}</div>}

                {isAlreadyLoggedIn && (
                  <div className="flip-auth-session-actions">
                    <button type="button" className="flip-session-btn flip-session-logout" onClick={handleLogout}>
                      <FiLogOut /> Logout
                    </button>
                    <button
                      type="button"
                      className="flip-session-btn flip-session-dashboard"
                      onClick={() => navigate('/dashboard/student')}
                    >
                      Dashboard <FiArrowRight />
                    </button>
                  </div>
                )}

                <button type="button" className="flip-google-btn" onClick={handleGoogleSignIn}>
                  <svg className="flip-google-icon" viewBox="0 0 24 24" width="20" height="20">
                    <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                    <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                    <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                    <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                  </svg>
                  Sign in with Google
                </button>

                <div className="flip-auth-divider">
                  <span>or</span>
                </div>

                <form onSubmit={handleLogin} className="flip-auth-form">
                  <label htmlFor="login-email">Email address</label>
                  <input
                    id="login-email"
                    type="email"
                    name="email"
                    value={loginData.email}
                    onChange={handleLoginChange}
                    placeholder="Enter your email"
                    required
                  />

                  <label htmlFor="login-password">Password</label>
                  <input
                    id="login-password"
                    type="password"
                    name="password"
                    value={loginData.password}
                    onChange={handleLoginChange}
                    placeholder="Enter your password"
                    required
                  />

                  <button type="submit" className="flip-auth-submit-btn" disabled={loginLoading}>
                    {loginLoading ? 'Signing in...' : 'Login'}
                  </button>

                  <div className="flip-auth-remember-row">
                    <label className="flip-remember">
                      <input type="checkbox" /> Remember for 30 days
                    </label>
                    <a href="#" className="flip-forgot">Forgot password</a>
                  </div>
                </form>

                <p className="flip-auth-new-user">
                  Don't have an account? 
                  <button type="button" className="flip-auth-switch-btn" onClick={flipToRegister}>Sign up</button>
                </p>
              </div>
            </div>

            {/* Register Panel */}
            <div className="flip-auth-panel">
              <div className="flip-auth-form-container">
                <h1>{isVerifying ? 'VERIFY EMAIL' : 'SIGN UP'}</h1>
                <p className="flip-auth-subtext">
                  {isVerifying
                    ? `Enter the code sent to ${registerData.email}`
                    : 'Create your account to get started'}
                </p>
                
                {message && <div className="flip-auth-alert success">{message}</div>}
                {registerError && <div className="flip-auth-alert error">{registerError}</div>}

                {!isVerifying && (
                  <>
                    <button type="button" className="flip-google-btn" onClick={handleGoogleSignIn}>
                      <svg className="flip-google-icon" viewBox="0 0 24 24" width="20" height="20">
                        <path fill="#4285F4" d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z"/>
                        <path fill="#34A853" d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z"/>
                        <path fill="#FBBC05" d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z"/>
                        <path fill="#EA4335" d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z"/>
                      </svg>
                      Sign up with Google
                    </button>

                    <div className="flip-auth-divider">
                      <span>or</span>
                    </div>
                  </>
                )}

                {!isVerifying ? (
                  <form onSubmit={handleRegister} className="flip-auth-form">
                    <label htmlFor="register-name">Full Name</label>
                    <input
                      id="register-name"
                      type="text"
                      name="fullName"
                      value={registerData.fullName}
                      onChange={handleRegisterChange}
                      placeholder="Enter your full name"
                      required
                    />

                    <label htmlFor="register-email">Email Address</label>
                    <input
                      id="register-email"
                      type="email"
                      name="email"
                      value={registerData.email}
                      onChange={handleRegisterChange}
                      placeholder="Enter your email"
                      required
                    />

                    <label htmlFor="register-password">Password</label>
                    <input
                      id="register-password"
                      type="password"
                      name="password"
                      value={registerData.password}
                      onChange={handleRegisterChange}
                      placeholder="Create a password"
                      required
                    />

                    <label htmlFor="register-role">Role</label>
                    <select
                      id="register-role"
                      name="role"
                      value={registerData.role}
                      onChange={handleRegisterChange}
                      required
                    >
                      <option value="student">Student</option>
                      <option value="professional">Professional Learner</option>
                      <option value="admin">Admin</option>
                    </select>

                    <button type="submit" className="flip-auth-submit-btn" disabled={registerLoading}>
                      {registerLoading ? 'Sending...' : 'Get Verification Code'}
                    </button>
                  </form>
                ) : (
                  <form onSubmit={handleVerify} className="flip-auth-form">
                    <label htmlFor="verify-code">Verification Code</label>
                    <input
                      id="verify-code"
                      type="text"
                      name="verificationCode"
                      value={verificationCode}
                      onChange={(e) => setVerificationCode(e.target.value)}
                      placeholder="6-Digit Code"
                      required
                    />

                    <button type="submit" className="flip-auth-submit-btn" disabled={registerLoading}>
                      {registerLoading ? 'Verifying...' : 'Verify & Create Account'}
                    </button>
                  </form>
                )}

                <p className="flip-auth-new-user">
                  Already have an account?
                  <button type="button" className="flip-auth-switch-btn" onClick={flipToLogin}>Log in</button>
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default AuthPage;
