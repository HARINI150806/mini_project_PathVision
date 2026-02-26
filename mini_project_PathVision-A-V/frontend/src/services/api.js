// Centralized API endpoints for the frontend
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

const AUTH_BASE = `${API_BASE}/auth`;

const endpoints = {
  BASE: API_BASE,
  AUTH: AUTH_BASE,
  login: `${AUTH_BASE}/login`,
  register: `${AUTH_BASE}/register`,
  verify: `${AUTH_BASE}/verify`,
  profile: `${API_BASE}/student/profile`,
};

export default endpoints;
