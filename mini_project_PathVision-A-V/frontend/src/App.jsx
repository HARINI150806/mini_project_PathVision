import {
  createBrowserRouter,
  RouterProvider,
} from 'react-router-dom';
import Navbar from './components/layout/Navbar';
import Home from './pages/Home';
import Login from './pages/Login';
import Register from './pages/Register';
import AdminDashboard from './pages/AdminDashboard';
import StudentDashboard from './pages/StudentDashboard';
import ProfessionalDashboard from './pages/ProfessionalDashboard';
import StudentProfile from './pages/StudentProfile';
import './components/layout/Navbar.css';
import './App.css';
import { ThemeProvider } from './context/ThemeContext';

const router = createBrowserRouter([
  {
    path: '/',
    element: <NavbarWrapper><Home /></NavbarWrapper>,
  },
  {
    path: '/login',
    element: <NavbarWrapper><Login /></NavbarWrapper>,
  },
  {
    path: '/register',
    element: <NavbarWrapper><Register /></NavbarWrapper>,
  },
  {
    path: '/dashboard/admin',
    element: <NavbarWrapper><AdminDashboard /></NavbarWrapper>,
  },
  {
    path: '/dashboard/student',
    element: <NavbarWrapper><StudentDashboard /></NavbarWrapper>,
  },
  {
    path: '/dashboard/student/profile',
    element: <NavbarWrapper><StudentProfile /></NavbarWrapper>,
  },
  {
    path: '/dashboard/professional',
    element: <NavbarWrapper><ProfessionalDashboard /></NavbarWrapper>,
  },
  {
    path: '/assessment',
    element: <NavbarWrapper><div className="container"><h2>Assessment Page</h2><p>Coming Soon</p></div></NavbarWrapper>,
  },
  {
    path: '/courses',
    element: <NavbarWrapper><div className="container"><h2>Courses Page</h2><p>Coming Soon</p></div></NavbarWrapper>,
  },
  {
    path: '/colleges',
    element: <NavbarWrapper><div className="container"><h2>Colleges Page</h2><p>Coming Soon</p></div></NavbarWrapper>,
  },
], {
  future: {
    v7_startTransition: true,
    v7_relativeSplatPath: true,
  },
});

function NavbarWrapper({ children }) {
  return (
    <div className="app-container">
      <Navbar />
      <main className="main-content">{children}</main>
    </div>
  );
}

function App() {
  return (
    <ThemeProvider>
      <RouterProvider router={router} />
    </ThemeProvider>
  );
}

export default App;
