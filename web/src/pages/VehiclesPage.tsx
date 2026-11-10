import { ArrowRight, CarFront, Plus, QrCode, Radio, Settings2 } from 'lucide-react'
import { Link } from 'react-router-dom'
import { EmptyState, PageTitle, Skeleton } from '../components/Ui'
import { useAuth } from '../context'
import { useVehicles } from '../lib/data'

export function VehiclesPage() {
  const { user } = useAuth()
  const { vehicles, loading, error } = useVehicles(user?.id)

  return (
    <div className="app-page vehicles-page">
      <PageTitle eyebrow="YOUR GARAGE" title="Vehicles" action={<Link className="button" to="/app/vehicles/new"><Plus size={17} /> Add vehicle</Link>}>Every vehicle has one clear home for its details, smart card, QR link and live status.</PageTitle>
      {error && <div className="inline-error">{error}</div>}
      {loading ? (
        <div className="vehicle-list-grid"><Skeleton className="vehicle-tile-skeleton" /><Skeleton className="vehicle-tile-skeleton" /></div>
      ) : vehicles.length ? (
        <div className="vehicle-list-grid">{vehicles.map((vehicle) => (
          <article className="vehicle-tile" key={vehicle.id}>
            <Link className={`vehicle-tile-art theme-${vehicle.card_theme}`} to={`/app/vehicles/${vehicle.id}`}>
              <span className="car-icon"><CarFront /></span>
              <span className="status-dot"><Radio size={11} /> {vehicle.is_active ? 'ACTIVE' : 'PAUSED'}</span>
              <strong>{vehicle.registration_number}</strong>
              <small>{[vehicle.color, vehicle.make, vehicle.model].filter(Boolean).join(' · ') || vehicle.vehicle_type}</small>
              <span className="open-vehicle">View vehicle <ArrowRight /></span>
            </Link>
            <div className="vehicle-tile-info"><div><span className="eyebrow">{vehicle.nickname || 'MY VEHICLE'}</span><h3>{vehicle.make || vehicle.vehicle_type} {vehicle.model}</h3></div><Link to={`/app/vehicles/${vehicle.id}`} aria-label={`Open ${vehicle.registration_number}`}><ArrowRight /></Link></div>
            <div className="vehicle-tile-actions"><Link className="button secondary" to={`/app/card/${vehicle.id}`}><QrCode size={17} /> Smart card</Link><Link className="button ghost" to={`/app/vehicles/${vehicle.id}/edit`}><Settings2 size={17} /> Edit</Link></div>
          </article>
        ))}</div>
      ) : (
        <EmptyState icon={<CarFront />} title="No vehicles added yet">Build your first smart vehicle card in under a minute.<Link className="button" to="/app/vehicles/new"><Plus size={17} /> Add first vehicle</Link></EmptyState>
      )}
    </div>
  )
}
