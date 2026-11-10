import { useEffect, useState } from 'react'
import { ArrowLeft, ArrowRight, CalendarDays, CarFront, Droplets, ExternalLink, Gauge, MapPinned, Palette, Pencil, QrCode, ShieldCheck } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { VehicleCard } from '../components/VehicleCard'
import { useAuth } from '../context'
import { humanError, supabase } from '../lib/supabase'
import type { Vehicle } from '../types'

export function VehicleDetailsPage() {
  const { id } = useParams()
  const { user, profile } = useAuth()
  const [vehicle, setVehicle] = useState<Vehicle | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id || !user) return
    let cancelled = false
    setLoading(true)
    supabase.from('vehicles').select('*').eq('id', id).eq('owner_id', user.id).single().then(({ data, error: queryError }) => {
      if (cancelled) return
      if (queryError) setError(humanError(queryError))
      setVehicle(data as Vehicle | null)
      setLoading(false)
    })
    return () => { cancelled = true }
  }, [id, user])

  if (loading) return <div className="app-page"><div className="page-loading">Opening vehicle…</div></div>
  if (!vehicle) return <div className="app-page"><Link className="back-link" to="/app/vehicles"><ArrowLeft /> Back to vehicles</Link><div className="empty-state"><CarFront /><h2>Vehicle unavailable</h2><p>{error || 'This vehicle is not in your garage.'}</p></div></div>

  const details = [
    { icon: <CarFront />, label: 'Vehicle type', value: vehicle.vehicle_type },
    { icon: <Gauge />, label: 'Make and model', value: [vehicle.make, vehicle.model].filter(Boolean).join(' ') || 'Not added' },
    { icon: <Droplets />, label: 'Colour', value: vehicle.color || 'Not added' },
    { icon: <CalendarDays />, label: 'Year', value: vehicle.year?.toString() || 'Not added' },
    { icon: <Palette />, label: 'Card style', value: `${vehicle.card_theme} · ${vehicle.card_layout.replace('_', ' ')}` },
    { icon: <ShieldCheck />, label: 'Public status', value: vehicle.is_active ? 'Active and ready' : 'Paused' },
  ]

  return <div className="app-page vehicle-details-page">
    <Link className="back-link" to="/app/vehicles"><ArrowLeft size={17} /> Back to vehicles</Link>
    <header className="vehicle-detail-hero">
      <div><span className={`vehicle-detail-status ${vehicle.is_active ? 'active' : ''}`}><i />{vehicle.is_active ? 'ACTIVE SMART ID' : 'CARD PAUSED'}</span><p>{vehicle.nickname || 'MY VEHICLE'}</p><h1>{vehicle.registration_number}</h1><span>{[vehicle.color, vehicle.make, vehicle.model].filter(Boolean).join(' · ') || vehicle.vehicle_type}</span></div>
      <Link className="button secondary" to={`/app/vehicles/${vehicle.id}/edit`}><Pencil size={16} /> Edit vehicle</Link>
    </header>
    <div className="vehicle-detail-grid">
      <section className="vehicle-detail-card-panel">
        <div className="vehicle-detail-section-head"><div><span className="eyebrow">SMART CARD</span><h2>Ready to scan</h2></div><QrCode /></div>
        <VehicleCard vehicle={vehicle} ownerName={profile?.full_name || 'Protected owner'} />
        <div className="vehicle-detail-actions"><Link className="button" to={`/app/card/${vehicle.id}`}>Open card studio <ArrowRight /></Link><Link className="button ghost" to={`/v/${vehicle.public_token}`} target="_blank">Test public scan <ExternalLink /></Link></div>
      </section>
      <aside className="vehicle-detail-info">
        <div className="vehicle-detail-section-head"><div><span className="eyebrow">VEHICLE PROFILE</span><h2>Everything at a glance</h2></div></div>
        <div className="vehicle-facts">{details.map((detail) => <div key={detail.label}><span>{detail.icon}</span><div><small>{detail.label}</small><strong>{detail.value}</strong></div></div>)}</div>
        <Link className="vehicle-live-link" to="/app/tracking"><MapPinned /><span><small>LIVE LOCATION & JOURNEYS</small><strong>Open vehicle tracking</strong></span><ArrowRight /></Link>
      </aside>
    </div>
  </div>
}
