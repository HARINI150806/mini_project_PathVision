import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiUser, FiMail, FiLock, FiKey } from 'react-icons/fi';
import './Register.css';
import learningImage from '../assets/login-learning-hero.svg';
import endpoints from '../services/api.js';

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
      const response = await fetch(endpoints.register, {
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
      const response = await fetch(endpoints.verify, {
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
