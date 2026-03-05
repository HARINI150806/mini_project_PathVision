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
  adminColleges: `${API_BASE}/admin/colleges`,
  adminCollegesUpload: `${API_BASE}/admin/colleges/upload`,
  adminCutoffsUpload: `${API_BASE}/admin/colleges/cutoffs/upload`,
  adminCutoffs: `${API_BASE}/admin/colleges/cutoffs`,
  adminResources: `${API_BASE}/admin/resources`,
  adminResourcesUpload: `${API_BASE}/admin/resources/upload`,
  adminUsers: `${API_BASE}/admin/users`,
  studentColleges: `${API_BASE}/student/colleges`,
  studentCollegeRecommendations: `${API_BASE}/student/college-recommendations`,
  studentRecommendedResources: `${API_BASE}/student/resources/recommended`,
};

export default endpoints;
