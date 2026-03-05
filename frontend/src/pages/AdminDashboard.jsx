import React, { useEffect, useMemo, useState } from 'react';
import { FiBarChart2, FiCalendar, FiDatabase, FiGrid, FiMapPin, FiSettings, FiUpload, FiUser, FiUsers } from 'react-icons/fi';
import { useNavigate } from 'react-router-dom';
import endpoints from '../services/api';
import './AdminDashboard.css';

const initialForm = {
  name: '',
  type: 'Engineering',
  district: '',
  state: '',
  address: '',
  rating: '',
  annualFees: '',
};

const initialCutoffForm = {
  collegeId: '',
  community: '',
  cutoffScore: '',
};

const initialResourceForm = {
  title: '',
  provider: '',
  source: 'YouTube',
  level: 'Beginner',
  url: '',
  interestKey: 'engineering_cse_it',
};

const menus = [
  { id: 'dashboard', label: 'Dashboard', icon: FiGrid },
  { id: 'courses', label: 'Manage Courses', icon: FiDatabase },
  { id: 'careers', label: 'Manage Careers', icon: FiBarChart2 },
  { id: 'colleges', label: 'Manage Colleges', icon: FiMapPin },
  { id: 'timeline', label: 'Timeline Events', icon: FiCalendar },
  { id: 'analytics', label: 'Analytics', icon: FiBarChart2 },
  { id: 'users', label: 'Users', icon: FiUsers },
  { id: 'settings', label: 'Settings', icon: FiSettings },
  { id: 'cutoffs', label: 'Map Cutoffs', icon: FiBarChart2 },
  { id: 'uploads', label: 'Dataset Uploads', icon: FiUpload },
];

const AdminDashboard = () => {
  const navigate = useNavigate();
  const [activeMenu, setActiveMenu] = useState('dashboard');
  const [formData, setFormData] = useState(initialForm);
  const [cutoffForm, setCutoffForm] = useState(initialCutoffForm);
  const [colleges, setColleges] = useState([]);
  const [cutoffs, setCutoffs] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [cutoffUploading, setCutoffUploading] = useState(false);
  const [status, setStatus] = useState('');
  const [error, setError] = useState('');
  const [cutoffWarning, setCutoffWarning] = useState('');
  const [cutoffApiAvailable, setCutoffApiAvailable] = useState(true);
  const [datasetFile, setDatasetFile] = useState(null);
  const [cutoffDatasetFile, setCutoffDatasetFile] = useState(null);
  const [resources, setResources] = useState([]);
  const [resourceForm, setResourceForm] = useState(initialResourceForm);
  const [resourceFile, setResourceFile] = useState(null);
  const [resourceUploading, setResourceUploading] = useState(false);
  const [users, setUsers] = useState([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [userSearch, setUserSearch] = useState('');
  const [userActionId, setUserActionId] = useState(null);

  const handleLogout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    navigate('/login');
  };

  useEffect(() => {
    const token = localStorage.getItem('token');
    const storedUser = (() => {
      try {
        return JSON.parse(localStorage.getItem('user') || 'null');
      } catch {
        return null;
      }
    })();
    const hasSession = !!token && !!storedUser;
    const isAdmin = (storedUser?.role || '').toLowerCase() === 'admin';
    if (!hasSession || !isAdmin) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      navigate('/login');
      return;
    }
    loadColleges();
    loadResources();
    loadUsers();
  }, [navigate]);

  useEffect(() => {
    if (cutoffApiAvailable && (activeMenu === 'dashboard' || activeMenu === 'cutoffs' || activeMenu === 'uploads')) {
      loadCutoffs(true);
    }
  }, [activeMenu, cutoffApiAvailable]);

  const getAuthHeaders = () => {
    const token = localStorage.getItem('token');
    return {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    };
  };

  const loadColleges = async () => {
    try {
      const response = await fetch(endpoints.adminColleges, { headers: getAuthHeaders() });
      if (!response.ok) throw new Error('Unable to load colleges');
      const data = await response.json();
      setColleges(Array.isArray(data) ? data : []);
    } catch {
      setError('Failed to load colleges. Check backend/admin login.');
    }
  };

  const loadResources = async () => {
    try {
      const response = await fetch(endpoints.adminResources, { headers: getAuthHeaders() });
      if (!response.ok) throw new Error('Unable to load resources');
      const data = await response.json();
      setResources(Array.isArray(data) ? data : []);
    } catch {
      setResources([]);
    }
  };

  const loadUsers = async () => {
    setUsersLoading(true);
    setError('');
    try {
      const response = await fetch(endpoints.adminUsers, { headers: getAuthHeaders() });
      if (response.status === 401) {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        navigate('/login');
        throw new Error('Session expired. Please login again.');
      }
      if (response.status === 403) {
        throw new Error('Access denied. Login with an admin account.');
      }
      if (response.status === 404) {
        throw new Error('Users API not found. Restart backend after pulling latest changes.');
      }
      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || 'Unable to load users');
      }
      const data = await response.json();
      setUsers(Array.isArray(data) ? data : []);
    } catch (err) {
      setUsers([]);
      setError(err?.message || 'Failed to load users. Check admin token/role or restart backend for /api/admin/users.');
    } finally {
      setUsersLoading(false);
    }
  };

  const loadCutoffs = async (silent = false) => {
    try {
      const response = await fetch(endpoints.adminCutoffs, { headers: getAuthHeaders() });
      if (response.status === 404) {
        setCutoffApiAvailable(false);
        setCutoffWarning('Cutoff list endpoint is not available in current backend build.');
        return;
      }
      if (!response.ok) throw new Error('Unable to load cutoffs');
      const data = await response.json();
      setCutoffs(Array.isArray(data) ? data : []);
      setCutoffWarning('');
    } catch {
      setCutoffs([]);
      if (silent) {
        setCutoffWarning('Cutoff mappings are temporarily unavailable.');
      } else {
        setError('Failed to load cutoff mappings.');
      }
    }
  };

  const cutoffByCommunity = useMemo(() => {
    const map = { OC: 0, BC: 0, MBC: 0, SC: 0, ST: 0 };
    cutoffs.forEach((item) => {
      const key = (item.community || '').toUpperCase();
      if (map[key] != null) map[key] += 1;
    });
    return map;
  }, [cutoffs]);

  const filteredUsers = useMemo(() => {
    const query = userSearch.trim().toLowerCase();
    if (!query) return users;
    return users.filter((user) => {
      const name = (user.fullName || '').toLowerCase();
      const email = (user.email || '').toLowerCase();
      const role = (user.role || '').toLowerCase();
      return name.includes(query) || email.includes(query) || role.includes(query);
    });
  }, [users, userSearch]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const handleAddCollege = async (event) => {
    event.preventDefault();
    setLoading(true);
    setError('');
    setStatus('');
    try {
      const payload = {
        ...formData,
        rating: formData.rating === '' ? 0 : Number(formData.rating),
        annualFees: formData.annualFees === '' ? null : Number(formData.annualFees),
      };
      const response = await fetch(endpoints.adminColleges, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(payload),
      });
      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || 'Failed to add college.');
      }
      setFormData(initialForm);
      setStatus('College added successfully.');
      await loadColleges();
    } catch (err) {
      setError(err.message || 'Failed to add college.');
    } finally {
      setLoading(false);
    }
  };

  const handleAddCutoff = async (event) => {
    event.preventDefault();
    if (!cutoffForm.collegeId || !cutoffForm.community || cutoffForm.cutoffScore === '') {
      setError('Select college, community and cutoff score.');
      return;
    }
    setError('');
    setStatus('');
    try {
      const response = await fetch(`${endpoints.adminColleges}/${cutoffForm.collegeId}/cutoffs`, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify({
          community: cutoffForm.community,
          cutoffScore: Number(cutoffForm.cutoffScore),
        }),
      });
      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || 'Failed to save cutoff.');
      }
      const selectedCollege = colleges.find((c) => String(c.id) === String(cutoffForm.collegeId));
      if (!cutoffApiAvailable) {
        setCutoffs((prev) => [
          ...prev,
          {
            id: `local-${Date.now()}`,
            collegeId: cutoffForm.collegeId,
            collegeName: selectedCollege?.name || 'College',
            community: cutoffForm.community,
            cutoffScore: Number(cutoffForm.cutoffScore),
          },
        ]);
      }
      setCutoffForm(initialCutoffForm);
      setStatus('Community cutoff saved successfully.');
      if (cutoffApiAvailable) {
        await loadCutoffs();
      }
    } catch (err) {
      setError(err.message || 'Failed to save cutoff.');
    }
  };

  const handleCollegeDatasetUpload = async (event) => {
    event.preventDefault();
    if (!datasetFile) {
      setError('Choose a college CSV file first.');
      return;
    }
    setUploading(true);
    setError('');
    setStatus('');
    try {
      const token = localStorage.getItem('token');
      const formDataUpload = new FormData();
      formDataUpload.append('file', datasetFile);
      const response = await fetch(endpoints.adminCollegesUpload, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: formDataUpload,
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'College dataset upload failed.');
      }
      const result = await response.json();
      setStatus(`College upload: ${result.successCount}/${result.totalRows} rows imported.`);
      setDatasetFile(null);
      await loadColleges();
    } catch (err) {
      setError(err.message || 'College dataset upload failed.');
    } finally {
      setUploading(false);
    }
  };

  const handleCutoffDatasetUpload = async (event) => {
    event.preventDefault();
    if (!cutoffDatasetFile) {
      setError('Choose a cutoff CSV file first.');
      return;
    }
    setCutoffUploading(true);
    setError('');
    setStatus('');
    try {
      const token = localStorage.getItem('token');
      const formDataUpload = new FormData();
      formDataUpload.append('file', cutoffDatasetFile);
      const response = await fetch(endpoints.adminCutoffsUpload, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: formDataUpload,
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'Cutoff dataset upload failed.');
      }
      const result = await response.json();
      setStatus(`Cutoff upload: ${result.successCount}/${result.totalRows} rows imported.`);
      setCutoffDatasetFile(null);
      if (cutoffApiAvailable) {
        await loadCutoffs();
      }
    } catch (err) {
      setError(err.message || 'Cutoff dataset upload failed.');
    } finally {
      setCutoffUploading(false);
    }
  };

  const handleAddResource = async (event) => {
    event.preventDefault();
    setError('');
    setStatus('');
    try {
      const response = await fetch(endpoints.adminResources, {
        method: 'POST',
        headers: getAuthHeaders(),
        body: JSON.stringify(resourceForm),
      });
      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || 'Failed to add resource.');
      }
      setResourceForm(initialResourceForm);
      setStatus('Resource added successfully.');
      await loadResources();
    } catch (err) {
      setError(err.message || 'Failed to add resource.');
    }
  };

  const handleResourceDatasetUpload = async (event) => {
    event.preventDefault();
    if (!resourceFile) {
      setError('Choose a resources CSV file first.');
      return;
    }
    setResourceUploading(true);
    setError('');
    setStatus('');
    try {
      const token = localStorage.getItem('token');
      const formDataUpload = new FormData();
      formDataUpload.append('file', resourceFile);
      const response = await fetch(endpoints.adminResourcesUpload, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: formDataUpload,
      });
      if (!response.ok) {
        const text = await response.text();
        throw new Error(text || 'Resources dataset upload failed.');
      }
      const result = await response.json();
      setStatus(`Resource upload: ${result.successCount}/${result.totalRows} rows imported.`);
      setResourceFile(null);
      await loadResources();
    } catch (err) {
      setError(err.message || 'Resources dataset upload failed.');
    } finally {
      setResourceUploading(false);
    }
  };

  const handleUserEnabledToggle = async (userId, enabled) => {
    setUserActionId(userId);
    setError('');
    setStatus('');
    try {
      const response = await fetch(`${endpoints.adminUsers}/${userId}`, {
        method: 'PUT',
        headers: getAuthHeaders(),
        body: JSON.stringify({ enabled }),
      });
      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || 'Failed to update user status.');
      }
      setStatus('User status updated successfully.');
      await loadUsers();
    } catch (err) {
      setError(err.message || 'Failed to update user status.');
    } finally {
      setUserActionId(null);
    }
  };

  const handleDeleteUser = async (userId) => {
    const ok = window.confirm('Delete this user account? This action cannot be undone.');
    if (!ok) return;
    setUserActionId(userId);
    setError('');
    setStatus('');
    try {
      const response = await fetch(`${endpoints.adminUsers}/${userId}`, {
        method: 'DELETE',
        headers: getAuthHeaders(),
      });
      if (!response.ok) {
        const message = await response.text();
        throw new Error(message || 'Failed to delete user.');
      }
      setStatus('User deleted successfully.');
      await loadUsers();
    } catch (err) {
      setError(err.message || 'Failed to delete user.');
    } finally {
      setUserActionId(null);
    }
  };

  return (
    <div className="admin-page">
      <aside className="admin-sidebar">
        <div>
        <h2>Admin Panel</h2>
        <nav>
          {menus.map((item) => {
            const Icon = item.icon;
            return (
              <button
                type="button"
                key={item.id}
                className={`admin-menu-btn ${activeMenu === item.id ? 'active' : ''}`}
                onClick={() => setActiveMenu(item.id)}
              >
                <Icon /> {item.label}
              </button>
            );
          })}
        </nav>
        </div>
        <button type="button" className="admin-logout-btn" onClick={handleLogout}>
          Logout
        </button>
      </aside>

      <main className="admin-main">
        <header className="admin-header">
          <h1>Admin Dashboard</h1>
          <p>Manage colleges, community cutoffs, and dataset uploads.</p>
        </header>

        <section className="admin-metric-grid">
          <article className="metric-card c1"><span>Total Colleges</span><b>{colleges.length}</b></article>
          <article className="metric-card c2"><span>Total Cutoff Mappings</span><b>{cutoffs.length}</b></article>
          <article className="metric-card c3"><span>BC Cutoffs</span><b>{cutoffByCommunity.BC}</b></article>
          <article className="metric-card c4"><span>SC/ST Cutoffs</span><b>{cutoffByCommunity.SC + cutoffByCommunity.ST}</b></article>
        </section>

        {status && <p className="admin-success">{status}</p>}
        {error && <p className="admin-error">{error}</p>}

        {(activeMenu === 'dashboard' || activeMenu === 'colleges') && (
          <section className="admin-panel">
            <h3>Manage Colleges</h3>
            <form className="admin-college-form" onSubmit={handleAddCollege}>
              <input name="name" value={formData.name} onChange={handleChange} placeholder="College Name" required />
              <select name="type" value={formData.type} onChange={handleChange}>
                <option>Engineering</option>
                <option>Arts & Science</option>
                <option>Medical</option>
                <option>Polytechnic</option>
              </select>
              <input name="district" value={formData.district} onChange={handleChange} placeholder="District" required />
              <input name="state" value={formData.state} onChange={handleChange} placeholder="State" required />
              <input name="address" value={formData.address} onChange={handleChange} placeholder="Address / Landmark" required />
              <input name="rating" value={formData.rating} onChange={handleChange} placeholder="Rating (optional)" type="number" step="0.1" min="0" max="5" />
              <input name="annualFees" value={formData.annualFees} onChange={handleChange} placeholder="Annual Fees (optional)" type="number" min="0" />
              <button type="submit" disabled={loading}>{loading ? 'Adding...' : 'Add College'}</button>
            </form>
          </section>
        )}

        {(activeMenu === 'dashboard' || activeMenu === 'cutoffs') && (
          <section className="admin-panel">
            <h3>Map Colleges to Community Cutoffs</h3>
            <form className="admin-cutoff-form" onSubmit={handleAddCutoff}>
              <select
                value={cutoffForm.collegeId}
                onChange={(e) => setCutoffForm((p) => ({ ...p, collegeId: e.target.value }))}
              >
                <option value="">Select College</option>
                {colleges.map((college) => (
                  <option key={college.id} value={college.id}>{college.name}</option>
                ))}
              </select>
              <select
                value={cutoffForm.community}
                onChange={(e) => setCutoffForm((p) => ({ ...p, community: e.target.value }))}
              >
                <option value="">Community</option>
                <option value="OC">OC</option>
                <option value="BC">BC</option>
                <option value="MBC">MBC</option>
                <option value="SC">SC</option>
                <option value="ST">ST</option>
              </select>
              <input
                type="number"
                placeholder="Cutoff Score"
                value={cutoffForm.cutoffScore}
                onChange={(e) => setCutoffForm((p) => ({ ...p, cutoffScore: e.target.value }))}
              />
              <button type="submit">Save Cutoff</button>
            </form>

            <div className="admin-cutoff-table-wrap">
              {cutoffWarning && <p className="admin-error">{cutoffWarning}</p>}
              <table className="admin-cutoff-table">
                <thead>
                  <tr>
                    <th>College</th>
                    <th>Community</th>
                    <th>Cutoff Score</th>
                  </tr>
                </thead>
                <tbody>
                  {cutoffs.map((item) => (
                    <tr key={item.id}>
                      <td>{item.collegeName}</td>
                      <td>{item.community}</td>
                      <td>{item.cutoffScore}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        )}

        {(activeMenu === 'dashboard' || activeMenu === 'uploads') && (
          <section className="admin-panel">
            <h3>Dataset Uploads</h3>
            <form className="admin-upload-form" onSubmit={handleCollegeDatasetUpload}>
              <label htmlFor="collegeCsvUpload">College CSV: name,type,district,state,address (+ optional rating,annualFees,latitude,longitude)</label>
              <input id="collegeCsvUpload" type="file" accept=".csv" onChange={(e) => setDatasetFile(e.target.files?.[0] || null)} />
              <button type="submit" disabled={uploading}>{uploading ? 'Uploading...' : 'Upload Colleges CSV'}</button>
            </form>
            <form className="admin-upload-form" onSubmit={handleCutoffDatasetUpload}>
              <label htmlFor="cutoffCsvUpload">Cutoff CSV: college_name,community,cutoffScore</label>
              <input id="cutoffCsvUpload" type="file" accept=".csv" onChange={(e) => setCutoffDatasetFile(e.target.files?.[0] || null)} />
              <button type="submit" disabled={cutoffUploading}>{cutoffUploading ? 'Uploading...' : 'Upload Cutoff CSV'}</button>
            </form>
            <form className="admin-upload-form" onSubmit={handleResourceDatasetUpload}>
              <label htmlFor="resourceCsvUpload">Resources CSV: title,provider,source,level,url,interest_key</label>
              <input id="resourceCsvUpload" type="file" accept=".csv" onChange={(e) => setResourceFile(e.target.files?.[0] || null)} />
              <button type="submit" disabled={resourceUploading}>{resourceUploading ? 'Uploading...' : 'Upload Resource CSV'}</button>
            </form>
          </section>
        )}

        {(activeMenu === 'dashboard' || activeMenu === 'courses') && (
          <section className="admin-panel">
            <h3>Manage Learning Resources</h3>
            <form className="admin-college-form" onSubmit={handleAddResource}>
              <input
                placeholder="Resource Title"
                value={resourceForm.title}
                onChange={(e) => setResourceForm((p) => ({ ...p, title: e.target.value }))}
                required
              />
              <input
                placeholder="Provider"
                value={resourceForm.provider}
                onChange={(e) => setResourceForm((p) => ({ ...p, provider: e.target.value }))}
                required
              />
              <select value={resourceForm.source} onChange={(e) => setResourceForm((p) => ({ ...p, source: e.target.value }))}>
                <option>YouTube</option>
                <option>NPTEL</option>
                <option>Course</option>
              </select>
              <select value={resourceForm.level} onChange={(e) => setResourceForm((p) => ({ ...p, level: e.target.value }))}>
                <option>Beginner</option>
                <option>Intermediate</option>
                <option>Advanced</option>
              </select>
              <input
                placeholder="URL"
                value={resourceForm.url}
                onChange={(e) => setResourceForm((p) => ({ ...p, url: e.target.value }))}
                required
              />
              <input
                placeholder="Interest Key (e.g. engineering_cse_it)"
                value={resourceForm.interestKey}
                onChange={(e) => setResourceForm((p) => ({ ...p, interestKey: e.target.value }))}
                required
              />
              <button type="submit">Add Resource</button>
            </form>

            <div className="admin-cutoff-table-wrap">
              <table className="admin-cutoff-table">
                <thead>
                  <tr>
                    <th>Title</th>
                    <th>Source</th>
                    <th>Level</th>
                    <th>Interest Key</th>
                  </tr>
                </thead>
                <tbody>
                  {resources.map((item) => (
                    <tr key={item.id}>
                      <td>{item.title}</td>
                      <td>{item.source}</td>
                      <td>{item.level}</td>
                      <td>{item.interestKey}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        )}

        {activeMenu === 'users' && (
          <section className="admin-panel">
            <h3>Manage Users</h3>
            <div className="admin-users-toolbar">
              <input
                type="text"
                placeholder="Search by name, email, role"
                value={userSearch}
                onChange={(e) => setUserSearch(e.target.value)}
              />
              <button type="button" onClick={loadUsers} disabled={usersLoading}>
                {usersLoading ? 'Refreshing...' : 'Refresh'}
              </button>
            </div>

            <div className="admin-cutoff-table-wrap">
              <table className="admin-cutoff-table admin-users-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th>Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {filteredUsers.map((user) => (
                    <tr key={user.id}>
                      <td>{user.fullName || '-'}</td>
                      <td>{user.email}</td>
                      <td>
                        <span className="admin-user-role">{user.role || 'STUDENT'}</span>
                      </td>
                      <td>
                        <span className={`admin-user-status ${user.enabled ? 'on' : 'off'}`}>
                          {user.enabled ? 'Enabled' : 'Disabled'}
                        </span>
                      </td>
                      <td>
                        <div className="admin-user-actions">
                          <button
                            type="button"
                            onClick={() => handleUserEnabledToggle(user.id, !user.enabled)}
                            disabled={userActionId === user.id}
                          >
                            {user.enabled ? 'Disable' : 'Enable'}
                          </button>
                          <button
                            type="button"
                            className="danger"
                            onClick={() => handleDeleteUser(user.id)}
                            disabled={userActionId === user.id}
                          >
                            Delete
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {!filteredUsers.length && (
                    <tr>
                      <td colSpan={5}>{usersLoading ? 'Loading users...' : 'No users found.'}</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </section>
        )}

        {!['dashboard', 'colleges', 'cutoffs', 'uploads', 'courses', 'users'].includes(activeMenu) && (
          <section className="admin-panel">
            <h3>{menus.find((m) => m.id === activeMenu)?.label}</h3>
            <p className="admin-placeholder">
              This section is ready in sidebar navigation. You can connect this module next based on your data model.
            </p>
            <div className="admin-placeholder-cards">
              <article><FiUser /> Role-based actions</article>
              <article><FiCalendar /> Scheduling and events</article>
              <article><FiBarChart2 /> Reporting and KPIs</article>
            </div>
          </section>
        )}
      </main>
    </div>
  );
};

export default AdminDashboard;
