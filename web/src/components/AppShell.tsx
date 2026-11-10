import type { ReactNode } from 'react'
import { Bell, CarFront, Home, MapPinned, UserRound } from 'lucide-react'
import { NavLink } from 'react-router-dom'
import { Brand } from './Brand'
import { useAuth } from '../context'

const nav = [
  { to: '/app', label: 'Home', icon: Home, end: true },
  { to: '/app/vehicles', label: 'Vehicles', icon: CarFront },
  { to: '/app/tracking', label: 'Live', icon: MapPinned },
  { to: '/app/alerts', label: 'Alerts', icon: Bell },
  { to: '/app/profile', label: 'Profile', icon: UserRound },
]

export function AppShell({ children }: { children: ReactNode }) {
  const { profile, user } = useAuth()
  const firstName = profile?.full_name?.split(' ')[0] || user?.email?.split('@')[0] || 'Owner'

  return (
    <div className="app-shell">
      <aside className="desktop-sidebar">
        <Brand />
        <nav>
          {nav.map(({ to, label, icon: Icon, end }) => (
            <NavLink key={to} to={to} end={end}>
              <Icon size={19} /> <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <div className="sidebar-profile">
          <span className="avatar">{firstName.slice(0, 1).toUpperCase()}</span>
          <div><strong>{firstName}</strong><small>Protected account</small></div>
        </div>
      </aside>

      <main className="app-main">
        <div className="mobile-app-header">
          <Brand compact />
          <span className="header-greeting">Hi, {firstName}</span>
          <NavLink className="icon-button" to="/app/alerts" aria-label="Alerts"><Bell size={20} /></NavLink>
        </div>
        {children}
      </main>

      <nav className="mobile-bottom-nav" aria-label="App navigation">
        {nav.map(({ to, label, icon: Icon, end }) => (
          <NavLink key={to} to={to} end={end}>
            <Icon size={20} /> <span>{label}</span>
          </NavLink>
        ))}
      </nav>
    </div>
  )
}
