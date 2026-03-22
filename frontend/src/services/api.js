// Default to same-origin API calls in development so Vite can proxy `/api`
// to the Spring backend without the browser talking to localhost:8080 directly.
const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api';

const AUTH_BASE = `${API_BASE}/auth`;

const endpoints = {
  BASE: API_BASE,
  AUTH: AUTH_BASE,
  login: `${AUTH_BASE}/login`,
  googleLogin: `${AUTH_BASE}/google`,
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
