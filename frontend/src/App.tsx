import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import LoginPage from './pages/LoginPage';
import EmployeeDashboard from './pages/EmployeeDashboard';
import TechnicianDashboard from './pages/TechnicianDashboard';
import AdminDashboard from './pages/AdminDashboard';
import IncidentDetail from './pages/IncidentDetail';
import Navbar from './components/Navbar';
import { motion } from 'framer-motion';

function App() {
  const { user } = useAuth();

  if (!user) return <LoginPage />;

  return (
    <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 transition-colors duration-200 relative overflow-x-hidden bg-grid-pattern">
      {/* Subtle ambient glows */}
      <div className="fixed top-[-15%] left-[-10%] w-[500px] h-[500px] rounded-full bg-indigo-500/5 dark:bg-indigo-600/10 blur-[130px] pointer-events-none -z-10" />
      <div className="fixed bottom-[-15%] right-[-10%] w-[500px] h-[500px] rounded-full bg-purple-500/5 dark:bg-purple-600/10 blur-[130px] pointer-events-none -z-10" />

      <Navbar />

      <motion.main
        initial={{ opacity: 0, y: 10 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, ease: 'easeOut' }}
        className="max-w-7xl mx-auto px-4 sm:px-6 py-6"
      >
        <Routes>
          <Route path="/login" element={<Navigate to="/" />} />
          <Route path="/incidents/:id" element={<IncidentDetail />} />
          <Route
            path="/*"
            element={
              user.role === 'ADMIN' ? (
                <AdminDashboard />
              ) : user.role === 'TECHNICIAN' ? (
                <TechnicianDashboard />
              ) : (
                <EmployeeDashboard />
              )
            }
          />
        </Routes>
      </motion.main>
    </div>
  );
}

export default App;
