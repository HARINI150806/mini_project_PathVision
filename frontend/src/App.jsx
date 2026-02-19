
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Navbar from './components/layout/Navbar';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import AdminDashboard from './pages/AdminDashboard';
import StudentDashboard from './pages/StudentDashboard';
import ProfessionalDashboard from './pages/ProfessionalDashboard';
import './components/layout/Navbar.css';
import './App.css';
import { ThemeProvider } from './context/ThemeContext';

function App() {
  return (
    <ThemeProvider>
      <Router>
        <div className="app-container">
          <Navbar />
          <main className="main-content">
            <Routes>
              <Route path="/" element={<Home />} />
              <Route path="/login" element={<Login />} />
              <Route path="/register" element={<Register />} />
              <Route path="/dashboard/admin" element={<AdminDashboard />} />
              <Route path="/dashboard/student" element={<StudentDashboard />} />
              <Route path="/dashboard/professional" element={<ProfessionalDashboard />} />
              {/* Placeholder routes */}
              <Route path="/assessment" element={<div className="container"><h2>Assessment Page</h2><p>Coming Soon</p></div>} />
              <Route path="/courses" element={<div className="container"><h2>Courses Page</h2><p>Coming Soon</p></div>} />
              <Route path="/colleges" element={<div className="container"><h2>Colleges Page</h2><p>Coming Soon</p></div>} />
            </Routes>
          </main>
        </div>
      </Router>
    </ThemeProvider>
  );
}


export default App
