import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { FiMail, FiLock } from 'react-icons/fi';
import './Login.css';
import endpoints from '../services/api.js';


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
            const response = await fetch(endpoints.login, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(formData)
            });
            // parse response safely (could be non-JSON)
            let data = null;
            let text = null;
            try {
                data = await response.json();
            } catch (jsonErr) {
                // fallback to text if not JSON
                try { text = await response.text(); } catch (tErr) { text = null; }
            }

            console.debug('Login response status:', response.status, 'json:', data, 'text:', text);

            if (response.ok) {
                // token might be in JSON or in text
                const tokenValue = data && data.token ? data.token : (text || null);
                if (tokenValue) {
                    localStorage.setItem('token', tokenValue);
                    localStorage.setItem('user', JSON.stringify({ name: data && data.name ? data.name : '', role: data && data.role ? data.role : '' }));
                }
                // Navigate to dashboard based on role
                if (data && data.role && data.role.toLowerCase() === 'admin') {
                    navigate('/dashboard/admin');
                } else if (data && data.role && data.role.toLowerCase() === 'professional') {
                    navigate('/dashboard/professional');
                } else {
                    navigate('/dashboard/student');
                }
            } else {
                // Backend now returns: "this email is not registered" for missing emails (404)
                const message = (data && data.message) ? data.message.toString() : (text ? text.toString() : '');
                const lower = message.toLowerCase();

                if (response.status === 404 || lower.includes('not registered') || lower.includes('this email')) {
                    setError('This email is not registered');
                } else if (lower.includes('verify')) {
                    setError('Account not verified. Please check your email for the verification code.');
                } else if (lower.includes('incorrect password') || lower.includes('bad credentials') ) {
                    setError('Incorrect password. Please try again.');
                } else {
                    setError(message || 'Invalid email or password.');
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
