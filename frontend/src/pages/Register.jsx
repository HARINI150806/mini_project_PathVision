import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiUser, FiMail, FiLock, FiKey } from 'react-icons/fi';
import './Register.css';

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
                setError(data.message || 'Registration failed. Please try again.');
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
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-header">
                    <h2>{isVerifying ? 'Verify Your Email' : 'Create an Account'}</h2>
                    <p>
                        {isVerifying
                                ? `Enter the code sent to ${formData.email}`
                                : 'Get started with PathVision today.'}
                    </p>
                </div>

                {message && <div className="alert success">{message}</div>}
                {error && <div className="alert error">{error}</div>}

                {!isVerifying ? (
                    <form onSubmit={handleRegister} className="auth-form">
                        <div className="input-group">
                            <FiUser className="input-icon" />
                            <input
                                type="text"
                                name="fullName"
                                placeholder="Full Name"
                                value={formData.fullName}
                                onChange={handleChange}
                                required
                                className="input-field"
                            />
                        </div>
                        <div className="input-group">
                            <FiMail className="input-icon" />
                            <input
                                type="email"
                                name="email"
                                placeholder="Email Address"
                                value={formData.email}
                                onChange={handleChange}
                                required
                                className="input-field"
                            />
                        </div>
                        <div className="input-group">
                            <FiLock className="input-icon" />
                            <input
                                type="password"
                                name="password"
                                placeholder="Password"
                                value={formData.password}
                                onChange={handleChange}
                                required
                                className="input-field"
                            />
                        </div>
                        <div className="input-group">
                            <select
                                name="role"
                                value={formData.role}
                                onChange={handleChange}
                                className="input-field"
                                required
                                style={{ paddingLeft: '2.75rem' }}
                            >
                                <option value="student">Student</option>
                                <option value="professional">Professional Learner</option>
                            </select>
                        </div>
                        <button type="submit" className="btn-auth" disabled={loading}>
                            {loading ? 'Sending...' : 'Get Verification Code'}
                        </button>
                    </form>
                ) : (
                    <form onSubmit={handleVerify} className="auth-form">
                        <div className="input-group">
                            <FiKey className="input-icon" />
                            <input
                                type="text"
                                name="verificationCode"
                                placeholder="6-Digit Code"
                                value={verificationCode}
                                onChange={(e) => setVerificationCode(e.target.value)}
                                required
                                className="input-field"
                            />
                        </div>
                        <button type="submit" className="btn-auth" disabled={loading}>
                            {loading ? 'Verifying...' : 'Verify & Create Account'}
                        </button>
                    </form>
                )}

                <div className="auth-footer">
                    <span>
                        {isVerifying ? "Didn't receive a code? " : 'Already have an account? '}
                    </span>
                    {isVerifying ? (
                        <button
                            type="button"
                            className="btn-link auth-link"
                            onClick={handleRegister}
                        >
                            Resend Code
                        </button>
                    ) : (
                        <Link to="/login" className="auth-link">
                            Log In
                        </Link>
                    )}
                </div>
            </div>
        </div>
    );
};

export default Register;
