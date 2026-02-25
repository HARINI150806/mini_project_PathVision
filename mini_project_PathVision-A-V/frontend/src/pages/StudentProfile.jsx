import React, { useState, useEffect } from 'react';
import endpoints from '../services/api.js';
import styles from './StudentProfile.module.css';

const initial = {
  interests: '',
  addressLine: '',
  city: '',
  state: '',
  pincode: '',
  gender: 'PREFER_NOT_TO_SAY',
  phone: '',
  stream: '',
};

export default function StudentProfile() {
  const [form, setForm] = useState(initial);
  const [file, setFile] = useState(null);
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');
  const [digilockerPrompt, setDigilockerPrompt] = useState(false);

  useEffect(() => {
    // Optionally, fetch existing profile to prefill form
    async function fetchProfile() {
      const token = localStorage.getItem('token');
      if (!token) return;
      try {
        const res = await fetch(endpoints.profile, { headers: { Authorization: `Bearer ${token}` } });
        if (res.ok) {
          const data = await res.json();
          setProfile(data);
          if (data) {
            setForm({
              interests: data.interestsJson ? JSON.parse(data.interestsJson).join(', ') : '',
              addressLine: data.addressLine || '',
              city: data.city || '',
              state: data.state || '',
              pincode: data.pincode || '',
              gender: data.gender || 'PREFER_NOT_TO_SAY',
              phone: data.phone || '',
              stream: data.stream || '',
            });
          }
        }
      } catch (e) {
        // ignore
      }
    }
    fetchProfile();
  }, []);

  const onChange = (e) => setForm({ ...form, [e.target.name]: e.target.value });

  // Disable manual file upload
  const onFile = (e) => {
    const f = e.target.files && e.target.files[0];
    if (!f) {
      setFile(null);
      setMessage('No file selected.');
      setDigilockerPrompt(false);
      return;
    }
    if (f.name.toLowerCase().endsWith('.pdf')) {
      setFile(f);
      setMessage('');
      setDigilockerPrompt(false);
    } else {
      setFile(null);
      setMessage('Only DigiLocker-verified PDF files are accepted. Please use DigiLocker to upload your marksheet.');
      setDigilockerPrompt(true);
    }
  };

  const validate = () => {
    if (!form.interests || form.interests.trim().length === 0) return 'Please enter at least one interest';
    if (!form.phone || !/^\d{10}$/.test(form.phone)) return 'Enter a valid 10-digit phone number';
    if (!form.city || !form.state) return 'City and state are required';
    return null;
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setMessage('');
    const err = validate();
    if (err) return setMessage(err);

    const token = localStorage.getItem('token');
    if (!token) return setMessage('Not authenticated');

    const fd = new FormData();
    fd.append('interests', JSON.stringify(form.interests.split(',').map(s => s.trim()).filter(Boolean)));
    fd.append('addressLine', form.addressLine);
    fd.append('city', form.city);
    fd.append('state', form.state);
    fd.append('pincode', form.pincode);
    fd.append('gender', form.gender);
    fd.append('phone', form.phone);
    if (form.stream) fd.append('stream', form.stream);
    // Only allow DigiLocker PDF
    if (file) {
      if (file.name.toLowerCase().endsWith('.pdf') && file.size > 0) {
        fd.append('marksheet', file);
      } else {
        setMessage('Only DigiLocker PDF files are accepted. Please download your marksheet from DigiLocker and upload the original PDF.');
        setLoading(false);
        return;
      }
    }

    setLoading(true);
    try {
      let url = endpoints.profile;
      const res = await fetch(url, {
        method: 'POST',
        headers: { Authorization: `Bearer ${token}` },
        body: fd
      });
      const text = await res.text();
      let data = null;
      try { data = JSON.parse(text); } catch (_) { data = null; }
      if (res.ok) {
        setMessage('Profile saved successfully');
      } else {
        // Show backend error message if available
        if (data && data.message) {
          setMessage(data.message);
        } else if (text && text.length < 200) {
          setMessage(text);
        } else {
          setMessage('Failed to save profile');
        }
      }
    } catch (err) {
      setMessage('Network error: ' + (err && err.message ? err.message : '')); 
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className={styles['profile-container']}>
      <div className={styles['profile-card']}>
        <h2>Student Profile</h2>
        {message && <div className={styles['profile-alert']}>{message}</div>}
        <form onSubmit={handleSubmit} className={styles['profile-form']}>
          <label>Interests (comma separated)</label>
          <input name="interests" value={form.interests} onChange={onChange} />

          <label>Marks (12) - upload marksheet</label>
          <div style={{ marginBottom: 8 }}>
            <button type="button" onClick={() => window.open('https://www.digilocker.gov.in/', '_blank')} style={{ padding: '8px 16px', background: '#1976d2', color: 'white', border: 'none', borderRadius: 4 }}>Go to DigiLocker</button>
          </div>
          <div style={{ marginBottom: 8 }}>
            <input type="file" accept="application/pdf" onChange={onFile} />
            <div style={{ fontSize: 13, color: '#555' }}>After downloading your marksheet PDF from DigiLocker, upload it here.</div>
          </div>
          {digilockerPrompt && (
            <div style={{ color: 'red', marginBottom: 8 }}>
              Only DigiLocker-verified certificates are accepted. Please use DigiLocker to upload your marksheet.
            </div>
          )}
          <div>
            {profile && profile.marksheetUrl && (
              <div>
                <a href={profile.marksheetUrl} target="_blank" rel="noreferrer" className={styles['profile-marksheet-link']}>View existing marksheet</a>
                {profile.aggregatePercentage !== null && (
                  <div style={{ marginTop: 6 }}>Extracted aggregate: {profile.aggregatePercentage}%</div>
                )}
                {profile.marksheetText && (
                  <details style={{ marginTop: 6 }}>
                    <summary>OCR Text (preview)</summary>
                    <pre className={styles['profile-ocr-preview']}>{profile.marksheetText.substring(0, Math.min(2000, profile.marksheetText.length))}</pre>
                  </details>
                )}
              </div>
            )}
          </div>


          <label>Stream</label>
          <select name="stream" value={form.stream} onChange={onChange}>
            <option value="">Select stream</option>
            <option value="CSE">CSE</option>
            <option value="Arts">Arts</option>
            <option value="Biology">Biology</option>
            <option value="Commerce">Commerce</option>
            <option value="Other">Other</option>
          </select>

          <label>Address Line</label>
          <input name="addressLine" value={form.addressLine} onChange={onChange} />

          <div className={styles['row']}>
            <div>
              <label>City</label>
              <input name="city" value={form.city} onChange={onChange} />
            </div>
            <div>
              <label>State</label>
              <input name="state" value={form.state} onChange={onChange} />
            </div>
            <div>
              <label>Pincode</label>
              <input name="pincode" value={form.pincode} onChange={onChange} />
            </div>
          </div>

          <label>Gender</label>
          <select name="gender" value={form.gender} onChange={onChange}>
            <option value="MALE">Male</option>
            <option value="FEMALE">Female</option>
            <option value="OTHER">Other</option>
            <option value="PREFER_NOT_TO_SAY">Prefer not to say</option>
          </select>

          <label>Phone</label>
          <input name="phone" value={form.phone} onChange={onChange} />

          <button type="submit" disabled={loading}>{loading ? 'Saving...' : 'Save Profile'}</button>
        </form>
      </div>
    </div>
  );
}
