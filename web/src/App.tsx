import { Navigate, Route, Routes, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { AppShell } from './components/AppShell'
import { useAuth } from './context'
import { HomePage } from './pages/HomePage'
import { AuthPage } from './pages/AuthPage'
import { DashboardPage } from './pages/DashboardPage'
import { VehiclesPage } from './pages/VehiclesPage'
import { VehicleFormPage } from './pages/VehicleFormPage'
import { CardPage } from './pages/CardPage'
import { AlertsPage } from './pages/AlertsPage'
import { ProfilePage } from './pages/ProfilePage'
import { PublicVehiclePage } from './pages/PublicVehiclePage'
import { TrackingPage } from './pages/TrackingPage'
import { PrintOrderPage } from './pages/PrintOrderPage'
import { VehicleDetailsPage } from './pages/VehicleDetailsPage'

function Protected({ children }: { children: ReactNode }) {
  const { user, loading } = useAuth()
  const location = useLocation()
  if (loading) return <div className="global-loader"><span className="brand-mark"><i /></span><p>Opening RoadPing…</p></div>
  if (!user) return <Navigate to="/login" state={{ from: location.pathname }} replace />
  return <AppShell>{children}</AppShell>
}

export default function App() {
  return <Routes>
    <Route path="/" element={<HomePage />} />
    <Route path="/login" element={<AuthPage mode="login" />} />
    <Route path="/signup" element={<AuthPage mode="signup" />} />
    <Route path="/v/:token" element={<PublicVehiclePage />} />
    <Route path="/app" element={<Protected><DashboardPage /></Protected>} />
    <Route path="/app/vehicles" element={<Protected><VehiclesPage /></Protected>} />
    <Route path="/app/vehicles/new" element={<Protected><VehicleFormPage /></Protected>} />
    <Route path="/app/vehicles/:id" element={<Protected><VehicleDetailsPage /></Protected>} />
    <Route path="/app/vehicles/:id/edit" element={<Protected><VehicleFormPage /></Protected>} />
    <Route path="/app/card/:id" element={<Protected><CardPage /></Protected>} />
    <Route path="/app/print/:vehicleId" element={<Protected><PrintOrderPage /></Protected>} />
    <Route path="/app/tracking" element={<Protected><TrackingPage /></Protected>} />
    <Route path="/app/alerts" element={<Protected><AlertsPage /></Protected>} />
    <Route path="/app/profile" element={<Protected><ProfilePage /></Protected>} />
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes>
}
