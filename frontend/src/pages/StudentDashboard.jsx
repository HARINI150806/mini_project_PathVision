import React, { useEffect, useMemo, useState } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import endpoints from '../services/api';
import StudentSidebar from '../components/layout/StudentSidebar';
import './StudentDashboard.css';

const toRadians = (degree) => (degree * Math.PI) / 180;

const calculateDistanceKm = (lat1, lng1, lat2, lng2) => {
  const earthRadius = 6371;
  const dLat = toRadians(lat2 - lat1);
  const dLng = toRadians(lng2 - lng1);

  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRadians(lat1)) *
      Math.cos(toRadians(lat2)) *
      Math.sin(dLng / 2) *
      Math.sin(dLng / 2);

  return earthRadius * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
};

const normalizeLocationToken = (value = '') =>
  value
    .toLowerCase()
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/[^a-z0-9]/g, '');

const StudentDashboard = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const showRecommendedCoursesOnly = location.hash === '#recommended-courses';
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedType, setSelectedType] = useState('All Types');
  const [selectedDistrictKey, setSelectedDistrictKey] = useState('all');
  const [sortBy, setSortBy] = useState('distance');
  const [locationMode, setLocationMode] = useState('manual');
  const [locationStatus, setLocationStatus] = useState('Select a preferred district or use your current location.');
  const [userLocation, setUserLocation] = useState(null);
  const [locationDisplay, setLocationDisplay] = useState('');
  const [locationName, setLocationName] = useState('');
  const [colleges, setColleges] = useState([]);
  const [loadingColleges, setLoadingColleges] = useState(true);
  const [loadError, setLoadError] = useState('');
  const [studentCutoffMark, setStudentCutoffMark] = useState(null);
  const [studentInterests, setStudentInterests] = useState([]);
  const [recommendedVideos, setRecommendedVideos] = useState([]);
  const [resourceFilter, setResourceFilter] = useState('all');
  const [communityFilter, setCommunityFilter] = useState('BC');
  const [recommendationInput, setRecommendationInput] = useState({
    maxAnnualFees: '',
  });
  const [recommendations, setRecommendations] = useState([]);
  const [recommendationLoading, setRecommendationLoading] = useState(false);
  const [recommendationError, setRecommendationError] = useState('');

  useEffect(() => {
    const hasSession = !!localStorage.getItem('token') || !!localStorage.getItem('user');
    if (!hasSession) {
      navigate('/login');
      return;
    }
    loadStudentCutoff();
    loadColleges();
  }, [navigate, communityFilter]);

  useEffect(() => {
    if (!location.hash) return;
    const section = document.querySelector(location.hash);
    if (section) {
      section.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
  }, [location.hash]);

  const districtOptions = useMemo(() => {
    const map = new Map();
    colleges.forEach((college) => {
      const key = `${normalizeLocationToken(college.district)}|${normalizeLocationToken(college.state)}`;
      if (!map.has(key)) {
        map.set(key, `${college.district}, ${college.state}`);
      }
    });
    return Array.from(map.entries())
      .map(([key, label]) => ({ key, label }))
      .sort((a, b) => a.label.localeCompare(b.label));
  }, [colleges]);

  const typeOptions = useMemo(
    () => [...new Set(colleges.map((college) => college.type).filter(Boolean))].sort(),
    [colleges]
  );

  const loadColleges = async () => {
    setLoadingColleges(true);
    setLoadError('');
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${endpoints.studentColleges}?community=${encodeURIComponent(communityFilter)}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });

      if (!response.ok) {
        throw new Error('Unable to load colleges');
      }

      const data = await response.json();
      const normalized = (Array.isArray(data) ? data : []).map((item) => ({
        id: item.id,
        name: item.name,
        district: item.district,
        state: item.state,
        type: item.type || 'General',
        rating: item.rating ?? 0,
        annualFees: item.annualFees,
        communityCutoff: item.communityCutoff,
        lat: item.latitude,
        lng: item.longitude,
      }));
      setColleges(normalized);
    } catch (error) {
      setLoadError('Could not load colleges from server.');
      setColleges([]);
    } finally {
      setLoadingColleges(false);
    }
  };

  const loadStudentCutoff = async () => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(endpoints.profile, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (!response.ok) {
        return;
      }
      const data = await response.json();
      const cutoff = data?.csCutoff ?? data?.aggregatePercentage ?? null;
      setStudentCutoffMark(cutoff);
      await loadRecommendedResources(resourceFilter);
    } catch {
      setStudentCutoffMark(null);
      setStudentInterests([]);
      setRecommendedVideos([]);
    }
  };

  const loadRecommendedResources = async (source = 'all') => {
    try {
      const token = localStorage.getItem('token');
      const response = await fetch(`${endpoints.studentRecommendedResources}?source=${encodeURIComponent(source)}`, {
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
      if (!response.ok) {
        throw new Error('Unable to load resources');
      }
      const data = await response.json();
      const normalized = (Array.isArray(data) ? data : []).map((item) => ({
        ...item,
        id: item.id ?? `${item.title}-${item.url}`,
      }));
      setRecommendedVideos(normalized);
      setStudentInterests([...new Set(normalized.map((item) => item.interestLabel).filter(Boolean))]);
    } catch {
      setRecommendedVideos([]);
      setStudentInterests([]);
    }
  };

  const fetchRecommendations = async () => {
    setRecommendationLoading(true);
    setRecommendationError('');
    try {
      const token = localStorage.getItem('token');
      const payload = {
        community: communityFilter,
        maxAnnualFees:
          recommendationInput.maxAnnualFees === ''
            ? null
            : Number(recommendationInput.maxAnnualFees),
        latitude: locationMode === 'gps' && userLocation ? userLocation.lat : null,
        longitude: locationMode === 'gps' && userLocation ? userLocation.lng : null,
        limit: 8,
      };

      const response = await fetch(endpoints.studentCollegeRecommendations, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          Authorization: `Bearer ${token}`,
        },
        body: JSON.stringify(payload),
      });

      if (!response.ok) {
        throw new Error('Failed to load recommendations');
      }

      const data = await response.json();
      setRecommendations(Array.isArray(data) ? data : []);
    } catch (error) {
      setRecommendationError('Could not generate recommendations now.');
      setRecommendations([]);
    } finally {
      setRecommendationLoading(false);
    }
  };

  const requestCurrentLocation = () => {
    if (!navigator.geolocation) {
      setLocationStatus('Location is not supported in this browser. Please use preferred district.');
      setLocationDisplay('');
      setLocationName('');
      return;
    }

    setLocationStatus('Fetching your location...');
    setLocationMode('gps');
    navigator.geolocation.getCurrentPosition(
      async (position) => {
        const latitude = position.coords.latitude;
        const longitude = position.coords.longitude;

        setUserLocation({
          lat: latitude,
          lng: longitude,
        });
        setLocationDisplay(`${latitude.toFixed(5)}, ${longitude.toFixed(5)}`);
        setLocationName('Resolving location name...');
        setSelectedDistrictKey('all');
        setLocationStatus('Current location applied. Colleges are sorted by nearest distance.');

        try {
          const response = await fetch(
            `https://api.bigdatacloud.net/data/reverse-geocode-client?latitude=${latitude}&longitude=${longitude}&localityLanguage=en`
          );
          if (!response.ok) {
            throw new Error('Failed to resolve location');
          }

          const data = await response.json();
          const parts = [
            data.city || data.locality || data.principalSubdivisionCode || '',
            data.principalSubdivision || '',
            data.countryName || '',
          ].filter(Boolean);

          setLocationName(parts.length ? parts.join(', ') : 'Location name unavailable');
        } catch (error) {
          setLocationName('Location name unavailable');
        }
      },
      () => {
        setLocationMode('manual');
        setLocationDisplay('');
        setLocationName('');
        setLocationStatus('Location permission denied. Please select your preferred district.');
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  };

  const processedColleges = useMemo(() => {
    const mapped = colleges
      .map((college) => {
        const distance =
          userLocation && locationMode === 'gps' && college.lat != null && college.lng != null
            ? calculateDistanceKm(userLocation.lat, userLocation.lng, college.lat, college.lng)
            : null;

        return {
          ...college,
          distance,
        };
      })
      .filter((college) => {
        const matchesSearch =
          !searchTerm ||
          college.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
          college.district.toLowerCase().includes(searchTerm.toLowerCase()) ||
          college.state.toLowerCase().includes(searchTerm.toLowerCase());

        const matchesType = selectedType === 'All Types' || college.type === selectedType;
        const collegeDistrictKey = `${normalizeLocationToken(college.district)}|${normalizeLocationToken(college.state)}`;
        const matchesDistrict = selectedDistrictKey === 'all' || collegeDistrictKey === selectedDistrictKey;

        return matchesSearch && matchesType && matchesDistrict;
      });

    if (sortBy === 'rating') {
      return mapped.sort((a, b) => b.rating - a.rating);
    }

    if (sortBy === 'distance' && locationMode === 'gps' && userLocation) {
      return mapped.sort((a, b) => (a.distance ?? Number.MAX_VALUE) - (b.distance ?? Number.MAX_VALUE));
    }

    return mapped.sort((a, b) => a.name.localeCompare(b.name));
  }, [colleges, locationMode, searchTerm, selectedDistrictKey, selectedType, sortBy, userLocation]);

  const filteredResources = useMemo(() => recommendedVideos, [recommendedVideos]);

  const recommendationStats = useMemo(() => {
    if (!filteredResources.length) {
      return { topMatch: 0, avgMatch: 0, total: 0 };
    }
    const scores = filteredResources.map((item) => item.matchPercentage || 0);
    const topMatch = Math.max(...scores);
    const avgMatch = Math.round(scores.reduce((a, b) => a + b, 0) / scores.length);
    return { topMatch, avgMatch, total: filteredResources.length };
  }, [filteredResources]);

  return (
    <div className="student-dashboard-page">
      <StudentSidebar activeLabel={showRecommendedCoursesOnly ? 'Recommended Courses' : 'Nearby Colleges'} />

      <section className="student-main-panel">
        <h1>{showRecommendedCoursesOnly ? 'Recommended Courses' : 'Nearby Colleges'}</h1>
        <p className="student-subtitle">
          {showRecommendedCoursesOnly
            ? 'Personalized resources from YouTube, NPTEL, and course platforms based on your interests'
            : 'Discover quality education opportunities near you'}
        </p>

        {!showRecommendedCoursesOnly && (
          <div className="student-location-strip">
            <button type="button" className="student-location-btn" onClick={requestCurrentLocation}>
              Use Current Location
            </button>
            <select
              value={selectedDistrictKey}
              onChange={(event) => {
                setSelectedDistrictKey(event.target.value);
                setLocationMode('manual');
                setUserLocation(null);
                setLocationDisplay('');
                setLocationName('');
              }}
              className="student-location-select"
            >
              <option value="all">All Districts</option>
              {districtOptions.map((districtOption) => (
                <option key={districtOption.key} value={districtOption.key}>
                  {districtOption.label}
                </option>
              ))}
            </select>
            <span className="student-location-status">{locationStatus}</span>
            {locationDisplay && (
              <span className="student-location-value">
                Current location: {locationName || 'Location name unavailable'}
              </span>
            )}
          </div>
        )}

        {!showRecommendedCoursesOnly && (
          <div className="student-filter-panel">
            <input
              type="text"
              placeholder="Search colleges or districts..."
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
            />
            <select value={selectedType} onChange={(event) => setSelectedType(event.target.value)}>
              <option>All Types</option>
              {typeOptions.map((type) => (
                <option key={type}>{type}</option>
              ))}
            </select>
            <select value={sortBy} onChange={(event) => setSortBy(event.target.value)}>
              <option value="distance">Sort by Distance</option>
              <option value="rating">Sort by Rating</option>
              <option value="name">Sort by Name</option>
            </select>
          </div>
        )}

        {!showRecommendedCoursesOnly && (
          <div className="student-recommend-panel">
            <h3>Recommended Colleges For You</h3>
            <p className="student-location-status">Student cutoff is auto-fetched from your profile.</p>
            <div className="student-recommend-grid">
              <select value={communityFilter} onChange={(event) => setCommunityFilter(event.target.value)}>
                <option value="OC">OC</option>
                <option value="BC">BC</option>
                <option value="MBC">MBC</option>
                <option value="SC">SC</option>
                <option value="ST">ST</option>
              </select>
              <div className="student-cutoff-field">
                <label htmlFor="studentCutoffInput">Student Cutoff</label>
                <input
                  id="studentCutoffInput"
                  type="text"
                  value={studentCutoffMark != null ? studentCutoffMark : 'Not available'}
                  disabled
                  title="Auto-fetched from student profile"
                />
              </div>
              <input
                type="number"
                placeholder="Max annual fees (optional)"
                value={recommendationInput.maxAnnualFees}
                onChange={(event) =>
                  setRecommendationInput((prev) => ({ ...prev, maxAnnualFees: event.target.value }))
                }
              />
              <button type="button" className="student-location-btn" onClick={fetchRecommendations}>
                {recommendationLoading ? 'Finding...' : 'Get Recommendations'}
              </button>
            </div>
            {recommendationError && <p className="student-location-status">{recommendationError}</p>}
            {recommendations.length > 0 && (
              <div className="student-recommend-list">
                {recommendations.map((item) => (
                  <article className="student-recommend-card" key={item.collegeId}>
                    <h4>{item.collegeName}</h4>
                    <p>{item.district}, {item.state}</p>
                    <p>Cutoff ({communityFilter}): {item.communityCutoff ?? 'N/A'} | Your score: {item.studentScore ?? 'N/A'}</p>
                    <p>Fees: {item.annualFees != null ? `Rs.${item.annualFees}/year` : 'N/A'}</p>
                    <p>{item.distanceKm != null ? `${item.distanceKm.toFixed(1)} km away` : 'Distance unavailable'}</p>
                    <p className="student-reason">{item.reason}</p>
                  </article>
                ))}
              </div>
            )}
          </div>
        )}

        {showRecommendedCoursesOnly && (
          <div className="student-course-panel recommend-layout" id="recommended-courses">
            <div className="recommend-hero">
              <h3>Smart Recommendations</h3>
              <p>AI-guided learning resources based on your selected interests.</p>
            </div>

            <div className="recommend-stats">
              <article>
                <span>Top Match</span>
                <b>{recommendationStats.topMatch}%</b>
              </article>
              <article>
                <span>Avg Score</span>
                <b>{recommendationStats.avgMatch}%</b>
              </article>
              <article>
                <span>Total Suggestions</span>
                <b>{recommendationStats.total}</b>
              </article>
              <article>
                <span>Last Updated</span>
                <b>Today</b>
              </article>
            </div>

            {studentInterests.length > 0 && (
              <>
                <p className="student-interests-title">Matched Interests</p>
                <div className="student-interest-tags">
                  {studentInterests.map((interestLabel) => (
                    <span key={interestLabel} className="student-interest-tag">
                      {interestLabel}
                    </span>
                  ))}
                </div>
              </>
            )}

            <div className="resource-filters">
              <button
                type="button"
                className={resourceFilter === 'all' ? 'active' : ''}
                onClick={() => {
                  setResourceFilter('all');
                  loadRecommendedResources('all');
                }}
              >
                All
              </button>
              <button
                type="button"
                className={resourceFilter === 'youtube' ? 'active' : ''}
                onClick={() => {
                  setResourceFilter('youtube');
                  loadRecommendedResources('youtube');
                }}
              >
                YouTube
              </button>
              <button
                type="button"
                className={resourceFilter === 'nptel' ? 'active' : ''}
                onClick={() => {
                  setResourceFilter('nptel');
                  loadRecommendedResources('nptel');
                }}
              >
                NPTEL
              </button>
              <button
                type="button"
                className={resourceFilter === 'course' ? 'active' : ''}
                onClick={() => {
                  setResourceFilter('course');
                  loadRecommendedResources('course');
                }}
              >
                Courses
              </button>
            </div>

            {filteredResources.length > 0 ? (
              <div className="student-video-grid rich-grid">
                {filteredResources.map((video) => (
                  <article key={video.id} className="student-video-card rich-card">
                    <div className="rich-top">
                      <span className="rich-type">{video.category.toUpperCase()}</span>
                      <span className="rich-demand">High Demand</span>
                    </div>
                    <h5>{video.title}</h5>
                    <p>{video.provider} | {video.level} | {video.source}</p>
                    <div className="rich-score-row">
                      <span>{video.confidenceText}</span>
                      <strong>{video.matchPercentage}%</strong>
                    </div>
                    <div className="rich-progress">
                      <div style={{ width: `${video.matchPercentage}%` }} />
                    </div>
                    <a href={video.url} target="_blank" rel="noreferrer" className="student-video-link">
                      Open Resource
                    </a>
                  </article>
                ))}
              </div>
            ) : (
              <p className="student-location-status">
                No resources available for this filter. Try another source.
              </p>
            )}
          </div>
        )}

        {!showRecommendedCoursesOnly && (
          <>
            <p className="student-results-count">{processedColleges.length} colleges found</p>
            {loadingColleges && <p className="student-results-count">Loading colleges...</p>}
            {loadError && <p className="student-location-status">{loadError}</p>}

            <div className="student-college-grid">
              {processedColleges.map((college) => (
                <article className="student-college-card" key={college.id}>
                  <div className="student-college-meta">
                    <span>{college.type}</span>
                    <span>{college.rating.toFixed(1)} rating</span>
                  </div>
                  <h3>{college.name}</h3>
                  <p>{college.district}, {college.state}</p>
                  <p>{college.annualFees != null ? `Fees: Rs.${college.annualFees}/year` : 'Fees: N/A'}</p>
                  <p>Cutoff mark ({communityFilter}): {college.communityCutoff != null ? college.communityCutoff : 'N/A'}</p>
                  <p className="student-distance-text">
                    {college.distance != null ? `${college.distance.toFixed(1)} km away` : `District: ${college.district}`}
                  </p>
                </article>
              ))}
            </div>
          </>
        )}
      </section>
    </div>
  );
};

export default StudentDashboard;
