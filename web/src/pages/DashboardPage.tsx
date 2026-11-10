import { useState } from 'react'
import { ArrowRight, BellRing, CarFront, EyeOff, MapPinned, Plus, QrCode, Radio, ShieldCheck, Smartphone, Sparkles } from 'lucide-react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context'
import { useAlerts, useTracking, useVehicles } from '../lib/data'
import { enablePush } from '../lib/notifications'
import { Button, EmptyState, Skeleton, Toast } from '../components/Ui'
import { VehicleCard } from '../components/VehicleCard'

export function DashboardPage() {
  const { user, profile } = useAuth()
  const { vehicles, loading: vehiclesLoading } = useVehicles(user?.id)
  const { alerts, loading: alertsLoading } = useAlerts(user?.id)
  const { devices } = useTracking(user?.id)
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [pushLoading, setPushLoading] = useState(false)
  const unread = alerts.filter((alert) => alert.status === 'new').length
  const firstName = profile?.full_name?.split(' ')[0] || 'there'

  async function activateNotifications() {
    if (!user) return
    setPushLoading(true)
    try {
      const result = await enablePush(user.id)
      setNotice({ type: 'success', text: result.permissionOnly ? 'Device notifications enabled. Add the VAPID key to activate background delivery.' : 'Background alerts are now enabled on this device.' })
    } catch (error) {
      setNotice({ type: 'error', text: error instanceof Error ? error.message : 'Could not enable notifications.' })
    } finally { setPushLoading(false) }
  }

  return (
    <div className="app-page dashboard-page">
      <header className="dashboard-welcome"><div><span className="eyebrow">OWNER DASHBOARD</span><h1>Good day, {firstName}.</h1><p>Your garage is protected and ready to receive pings.</p></div><Link className="button desktop-only" to="/app/vehicles/new"><Plus size={17} /> Add vehicle</Link></header>
      {notice && <Toast type={notice.type} onClose={() => setNotice(null)}>{notice.text}</Toast>}
      <Link className="tracker-dashboard-banner" to="/app/tracking">
        <span className="tracker-dashboard-icon"><MapPinned /></span>
        <div><small>ANDROID VEHICLE PLAYER</small><strong>{devices.length ? `${devices.filter((device) => device.is_tracking).length} PLAYER${devices.length === 1 ? '' : 'S'} TRACKING NOW` : 'SET UP THIS VEHICLE'}</strong><p>{devices.length ? 'Open the live map, player status and complete trip history.' : 'Install the same RoadPing app in a compatible Android car player and connect it to one vehicle.'}</p></div>
        <span className="tracker-dashboard-action">{devices.length ? <Radio /> : <Smartphone />} OPEN LIVE <ArrowRight /></span>
      </Link>
      <div className="stat-grid">
        <article><span className="stat-icon blue"><CarFront /></span><div><small>Active vehicles</small><strong>{vehiclesLoading ? '—' : vehicles.length}</strong><Link to="/app/vehicles">Manage garage <ArrowRight /></Link></div></article>
        <article><span className={`stat-icon ${unread ? 'orange' : 'green'}`}><BellRing /></span><div><small>New alerts</small><strong>{alertsLoading ? '—' : unread}</strong><Link to="/app/alerts">Open inbox <ArrowRight /></Link></div></article>
        <article><span className="stat-icon purple"><EyeOff /></span><div><small>Phone privacy</small><strong className="word-stat">{profile?.phone_visibility || 'Hidden'}</strong><Link to="/app/profile">Privacy settings <ArrowRight /></Link></div></article>
      </div>

      {!profile?.push_enabled && Notification.permission !== 'granted' && <div className="notification-banner"><span className="banner-icon"><Smartphone /></span><div><strong>Never miss a roadside alert</strong><p>Allow notifications so RoadPing can reach you even when the app is in the background.</p></div><Button className="secondary small" loading={pushLoading} onClick={activateNotifications}>Enable alerts</Button></div>}

      <section className="dashboard-section">
        <div className="section-row"><div><span className="eyebrow">YOUR GARAGE</span><h2>Smart vehicle cards</h2></div><Link to="/app/vehicles">View all <ArrowRight size={16} /></Link></div>
        {vehiclesLoading ? <div className="vehicle-preview-grid"><Skeleton className="card-skeleton" /><Skeleton className="card-skeleton" /></div> : vehicles.length ? <div className="vehicle-preview-grid">{vehicles.slice(0, 2).map((vehicle) => <div className="dashboard-card" key={vehicle.id}><VehicleCard vehicle={vehicle} ownerName={profile?.full_name || 'Protected owner'} /><div className="card-actions"><Link to={`/app/card/${vehicle.id}`}><QrCode size={17} /> Open card</Link><Link to={`/v/${vehicle.public_token}`}><Sparkles size={17} /> Test scan</Link></div></div>)}</div> : <EmptyState icon={<CarFront />} title="Your garage is waiting">Add your first vehicle to generate a unique smart card and QR code.</EmptyState>}
      </section>

      <section className="dashboard-section">
        <div className="section-row"><div><span className="eyebrow">RECENT ACTIVITY</span><h2>Latest pings</h2></div><Link to="/app/alerts">View inbox <ArrowRight size={16} /></Link></div>
        {alertsLoading ? <Skeleton className="alert-list-skeleton" /> : alerts.length ? <div className="compact-alert-list">{alerts.slice(0, 4).map((alert) => <Link to="/app/alerts" key={alert.id} className={alert.status === 'new' ? 'unread' : ''}><span className="alert-type-icon"><BellRing /></span><div><strong>{alert.alert_type.replaceAll('_', ' ')}</strong><p>{alert.vehicles?.registration_number} · {alert.message || 'A road user sent an alert.'}</p></div><time>{new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit', day: 'numeric', month: 'short' }).format(new Date(alert.created_at))}</time></Link>)}</div> : <div className="all-clear"><ShieldCheck /><div><strong>All clear</strong><p>No alerts have been reported for your vehicles yet.</p></div></div>}
      </section>

      <Link className="floating-add" to="/app/vehicles/new" aria-label="Add vehicle"><Plus /></Link>
    </div>
  )
}
