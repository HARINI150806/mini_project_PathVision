import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiUser, FiMail, FiLock, FiKey } from 'react-icons/fi';
import './Register.css';
import learningImage from '../assets/login-learning-hero.svg';

const API_BASE_URL = 'http://localhost:8080/api/auth';

const Register = () => {
  const navigate = useNavigate();
  const [formData, setFormData] = useState({
    fullName: '',
    email: '',
    password: '',
    role: 'student'
  });
  const [verificationCode, setVerificationCode] = useState('');
  const [isVerifying, setIsVerifying] = useState(false);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const user = JSON.parse(localStorage.getItem('user'));
    if (user && user.role) {
      if (user.role.toLowerCase() === 'admin') navigate('/dashboard/admin');
      else if (user.role.toLowerCase() === 'professional') navigate('/dashboard/professional');
      else navigate('/dashboard/student');
    }
  }, [navigate]);

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleRegister = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/register`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData)
      });

      const data = await response.json();

      if (response.ok) {
        setMessage(data.message || 'Verification code sent to your email.');
        setIsVerifying(true);
      } else {
        if (data.message && data.message.toLowerCase().includes('already in use')) {
          setError('This email is already registered. Please log in or use a different email.');
        } else {
          setError(data.message || 'Registration failed. Please try again.');
        }
      }
    } catch (err) {
      setError('Could not connect to the server. Is it running?');
    } finally {
      setLoading(false);
    }
  };

  const handleVerify = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setLoading(true);

    try {
      const response = await fetch(`${API_BASE_URL}/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          email: formData.email,
          verificationCode: verificationCode
        })
      });

      if (response.ok) {
        setMessage('Verification successful! Redirecting to login...');
        setTimeout(() => navigate('/login'), 2000);
      } else {
        const data = await response.text();
        try {
          const jsonData = JSON.parse(data);
          setError(jsonData.message || 'Invalid or expired verification code.');
        } catch {
          setError(data || 'Invalid or expired verification code.');
        }
      }
    } catch (err) {
      setError('Verification failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="register-page">
      <div className="register-card">
        <section className="register-illustration">
          <div className="register-illustration-overlay" />
          <img className="register-illustration-image" src={learningImage} alt="Learning illustration" />
        </section>

        <section className="register-panel">
          <div className="register-panel-inner">
            <h1>{isVerifying ? 'VERIFY EMAIL' : 'SIGN UP'}</h1>
            <p className="register-subtext">
              {isVerifying
                ? `Enter the code sent to ${formData.email}`
                : 'Get started with PathVision today.'}
            </p>

            {message && <div className="register-alert success">{message}</div>}
            {error && <div className="register-alert error">{error}</div>}

            {!isVerifying && (
              <>
                <button type="button" className="register-google-btn">
                  <svg viewBox="0 0 24 24" width="20" height="20" xmlns="http://www.w3.org/2000/svg">
                    <g transform="matrix(1, 0, 0, 1, 27.009001, -39.238998)">
                      <path fill="#4285F4" d="M -3.264 51.509 C -3.264 50.719 -3.334 49.969 -3.454 49.239 L -14.754 49.239 L -14.754 53.749 L -8.284 53.749 C -8.574 55.229 -9.424 56.479 -10.684 57.329 L -10.684 60.329 L -6.824 60.329 C -4.564 58.239 -3.264 55.159 -3.264 51.509 Z"/>
                      <path fill="#34A853" d="M -14.754 63.239 C -11.514 63.239 -8.804 62.159 -6.824 60.329 L -10.684 57.329 C -11.764 58.049 -13.134 58.489 -14.754 58.489 C -17.884 58.489 -20.534 56.379 -21.484 53.529 L -25.464 53.529 L -25.464 56.619 C -23.494 60.539 -19.444 63.239 -14.754 63.239 Z"/>
                      <path fill="#FBBC05" d="M -21.484 53.529 C -21.734 52.809 -21.864 52.039 -21.864 51.239 C -21.864 50.439 -21.734 49.669 -21.484 48.949 L -21.484 45.859 L -25.464 45.859 C -26.284 47.479 -26.754 49.299 -26.754 51.239 C -26.754 53.179 -26.284 54.999 -25.464 56.619 L -21.484 53.529 Z"/>
                      <path fill="#EA4335" d="M -14.754 43.989 C -12.984 43.989 -11.404 44.599 -10.154 45.789 L -6.734 42.369 C -8.804 40.429 -11.514 39.239 -14.754 39.239 C -19.444 39.239 -23.494 41.939 -25.464 45.859 L -21.484 48.949 C -20.534 46.099 -17.884 43.989 -14.754 43.989 Z"/>
                    </g>
                  </svg>
                  Sign up with Google
                </button>

                <div className="register-divider">
                  <span>or</span>
                </div>
              </>
            )}

            {!isVerifying ? (
              <form onSubmit={handleRegister} className="register-form-fields">
                <label htmlFor="register-name">Full Name</label>
                <input
                  id="register-name"
                  type="text"
                  name="fullName"
                  value={formData.fullName}
                  onChange={handleChange}
                  required
                />

                <label htmlFor="register-email">Email Address</label>
                <input
                  id="register-email"
                  type="email"
                  name="email"
                  value={formData.email}
                  onChange={handleChange}
                  required
                />

                <label htmlFor="register-password">Password</label>
                <input
                  id="register-password"
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  required
                />

                <label htmlFor="register-role">Role</label>
                <select
                  id="register-role"
                  name="role"
                  value={formData.role}
                  onChange={handleChange}
                  required
                >
                  <option value="student">Student</option>
                  <option value="professional">Professional Learner</option>
                  <option value="admin">Admin</option>
                </select>

                <button type="submit" className="register-submit-btn" disabled={loading}>
                  {loading ? 'Sending...' : 'Get Verification Code'}
                </button>
              </form>
            ) : (
              <form onSubmit={handleVerify} className="register-form-fields">
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

                <button type="submit" className="register-submit-btn" disabled={loading}>
                  {loading ? 'Verifying...' : 'Verify & Create Account'}
                </button>
              </form>
            )}

            <p className="register-new-user">
              Already have an account?
              <Link to="/login">Log in</Link>
            </p>
          </div>
        </section>
      </div>
    </div>
  );
};

export default Register;
