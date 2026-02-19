import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiMail, FiLock } from 'react-icons/fi';
import './Login.css';

const API_BASE_URL = 'http://localhost:8080/api/auth';


const Login = () => {
        const navigate = useNavigate();
        const [formData, setFormData] = useState({
                email: '',
                password: ''
        });
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

    const handleLogin = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const response = await fetch(`${API_BASE_URL}/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });

            const data = await response.json();

            if (response.ok) {
                localStorage.setItem('token', data.token);
                localStorage.setItem('user', JSON.stringify({ name: data.name, role: data.role }));
                // Navigate to dashboard based on role
                if (data.role && data.role.toLowerCase() === 'admin') {
                    navigate('/dashboard/admin');
                } else if (data.role && data.role.toLowerCase() === 'professional') {
                    navigate('/dashboard/professional');
                } else {
                    navigate('/dashboard/student');
                }
            } else {
                if (data.message && data.message.toLowerCase().includes('not found')) {
                  setError('Email does not exist. Please register first.');
                } else if (data.message && data.message.toLowerCase().includes('verify')) {
                  setError('Account not verified. Please check your email for the verification code.');
                } else {
                  setError(data.message || 'Invalid email or password.');
                }
            }
        } catch (err) {
            setError('Could not connect to the server. Is it running?');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-container">
            <div className="auth-card">
                <div className="auth-title">Welcome Back!</div>
                <p style={{ color: 'var(--text-secondary)', textAlign: 'center', marginBottom: '1rem' }}>
                    Log in to continue your journey with PathVision.
                </p>

                {error && <div className="alert error">{error}</div>}

                <form onSubmit={handleLogin} className="auth-form">
                    <div className="input-group">
                        <FiMail className="input-icon" />
                        <input
                            type="email"
                            id="email"
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
                            id="password"
                            name="password"
                            placeholder="Password"
                            value={formData.password}
                            onChange={handleChange}
                            required
                            className="input-field"
                        />
                    </div>
                    <button type="submit" className="auth-btn" disabled={loading}>
                        {loading ? 'Logging In...' : 'Log In'}
                    </button>
                </form>

                <div className="auth-footer">
                    <span>Don't have an account?</span>
                    <Link to="/register" className="auth-link">
                        Sign Up
                    </Link>
                </div>
            </div>
        </div>
    );
};

export default Login;
