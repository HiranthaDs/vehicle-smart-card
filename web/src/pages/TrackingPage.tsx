import { useEffect, useMemo, useState } from 'react'
import { ArrowRight, BatteryCharging, CarFront, Check, Clock3, Gauge, LocateFixed, MapPin, Radio, Route, Signal, Smartphone } from 'lucide-react'
import { PageTitle, Skeleton } from '../components/Ui'
import { TrackingMap } from '../components/TrackingMap'
import { useAuth } from '../context'
import { useTracking } from '../lib/data'
import { supabase } from '../lib/supabase'
import type { LocationPoint, TrackingSession } from '../types'

function kmh(mps: number) { return Math.round((mps || 0) * 3.6) }
function duration(seconds: number) {
  const hours = Math.floor(seconds / 3600)
  const minutes = Math.floor((seconds % 3600) / 60)
  return hours ? `${hours}h ${minutes}m` : `${minutes} min`
}

export function TrackingPage() {
  const { user } = useAuth()
  const { devices, sessions, loading, refresh } = useTracking(user?.id)
  const [selectedDeviceId, setSelectedDeviceId] = useState('')
  const [selectedSession, setSelectedSession] = useState<TrackingSession | null>(null)
  const [points, setPoints] = useState<LocationPoint[]>([])
  const [pointsLoading, setPointsLoading] = useState(false)
  const [displaySaving, setDisplaySaving] = useState(false)
  const [displayMessage, setDisplayMessage] = useState('')
  const selectedDevice = devices.find((device) => device.id === selectedDeviceId) || devices[0]

  useEffect(() => {
    if (!selectedDeviceId && devices[0]) setSelectedDeviceId(devices[0].id)
  }, [devices, selectedDeviceId])

  const deviceSessions = useMemo(() => sessions.filter((session) => !selectedDevice || session.tracker_device_id === selectedDevice.id), [sessions, selectedDevice])
  const activeSession = deviceSessions.find((session) => session.status === 'active')
  const isLive = Boolean(selectedDevice?.is_tracking && selectedDevice.last_seen_at && Date.now() - new Date(selectedDevice.last_seen_at).getTime() < 90_000)

  async function openSession(session: TrackingSession) {
    setSelectedSession(session)
    setPointsLoading(true)
    const { data } = await supabase.from('location_points').select('id,session_id,sequence_number,recorded_at,latitude,longitude,speed_mps,bearing,accuracy_m').eq('session_id', session.id).order('sequence_number')
    setPoints((data as LocationPoint[]) || [])
    setPointsLoading(false)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  async function setPlayerDisplay(mode: 'journey' | 'speed' | 'clock' | 'map') {
    if (!selectedDevice || !user) return
    setDisplaySaving(true)
    setDisplayMessage('')
    const { error } = await supabase.from('tracker_devices').update({ display_mode: mode }).eq('id', selectedDevice.id).eq('owner_id', user.id)
    if (error) setDisplayMessage(error.message)
    else {
      setDisplayMessage('Player screen updated. The car display will use it on the next refresh.')
      await refresh()
    }
    setDisplaySaving(false)
  }

  if (loading) return <div className="app-page tracking-page"><Skeleton className="alerts-skeleton" /></div>

  if (!devices.length) return <div className="app-page tracking-page"><PageTitle eyebrow="VEHICLE INTELLIGENCE" title="Live tracking">Connect the Android player inside a vehicle to see its position and journey history here.</PageTitle><section className="no-tracker"><div><p className="eyebrow">NO PLAYER CONNECTED</p><h2>TURN THE CAR PLAYER INTO A TRACKER.</h2><p>Install the RoadPing Android APK on a compatible aftermarket Android head unit, sign in, and choose Vehicle Player Mode. Setup remains visible and permission-led.</p><a className="button secondary" href="/downloads/roadping.apk" download>Download Android APK <ArrowRight size={16} /></a></div><div className="no-tracker-steps"><div><span>01</span><section><strong>Install and sign in</strong><p>Use the same RoadPing owner account on the player.</p></section></div><div><span>02</span><section><strong>Tap Setup this vehicle</strong><p>Select a vehicle and review every requested permission.</p></section></div><div><span>03</span><section><strong>Start tracking</strong><p>A persistent Android notification shows whenever location is being shared.</p></section></div><div><span>04</span><section><strong>Watch from anywhere</strong><p>Open this dashboard from your phone or website.</p></section></div></div></section></div>

  const displaySession = selectedSession || activeSession
  const displayPoints = selectedSession ? points : []
  const lastSeenText = selectedDevice?.last_seen_at ? new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' }).format(-Math.max(0, Math.round((Date.now() - new Date(selectedDevice.last_seen_at).getTime()) / 60000)), 'minute') : 'Never'

  return <div className="app-page tracking-page">
    <PageTitle eyebrow="VEHICLE INTELLIGENCE" title="Live tracking">Realtime car-player status, precise route history and useful journey totals.</PageTitle>
    <div className="tracking-toolbar"><select value={selectedDevice?.id || ''} onChange={(event) => { setSelectedDeviceId(event.target.value); setSelectedSession(null); setPoints([]) }}>{devices.map((device) => <option value={device.id} key={device.id}>{device.vehicles?.registration_number || 'Vehicle'} · {device.name}</option>)}</select><span className={`tracking-status ${isLive ? 'live' : 'stale'}`}><i /> {isLive ? 'LIVE PLAYER ONLINE' : 'PLAYER OFFLINE'}</span></div>
    <section className="live-tracking-grid">
      <div className="tracking-map-card">
        {pointsLoading ? <Skeleton className="tracking-map" /> : <TrackingMap latitude={selectedSession ? undefined : selectedDevice?.last_latitude} longitude={selectedSession ? undefined : selectedDevice?.last_longitude} bearing={selectedDevice?.last_bearing} points={displayPoints} />}
        <div className="tracking-map-foot"><div><small>CURRENT SPEED</small><strong>{selectedSession ? kmh(selectedSession.max_speed_mps) : kmh(selectedDevice?.last_speed_mps || 0)}</strong><span>KM/H {selectedSession && 'MAX'}</span></div><div><small>LAST UPDATE</small><strong>{isLive ? 'NOW' : lastSeenText}</strong><span>{selectedDevice?.last_accuracy_m ? `±${Math.round(selectedDevice.last_accuracy_m)}m accuracy` : 'Waiting for GPS'}</span></div><div><small>{displaySession ? 'TRIP DISTANCE' : 'LOCATION'}</small><strong>{displaySession ? (displaySession.distance_m / 1000).toFixed(1) : selectedDevice?.last_latitude?.toFixed(3) || '—'}</strong><span>{displaySession ? 'KM' : selectedDevice?.last_longitude?.toFixed(3) || 'NO FIX'}</span></div><div><small>{displaySession ? 'TRIP DURATION' : 'PLAYER MODE'}</small><strong>{displaySession ? duration(displaySession.duration_seconds) : 'CAR'}</strong><span>{displaySession?.status || selectedDevice?.mode.replaceAll('_', ' ')}</span></div></div>
      </div>
      <aside className="tracker-side"><div className="tracker-device-panel"><span className={`tracking-status ${isLive ? 'live' : 'stale'}`}><i /> {isLive ? 'TRACKING' : 'NOT TRACKING'}</span><h2>{selectedDevice?.vehicles?.registration_number || 'VEHICLE PLAYER'}</h2><p>{selectedDevice?.name} · {selectedDevice?.platform} {selectedDevice?.app_version}</p><div className="tracker-device-meta"><div><small>NETWORK</small><strong><Signal size={13} /> {selectedDevice?.network_connected ? 'Connected' : 'Offline'}</strong></div><div><small>BATTERY</small><strong><BatteryCharging size={13} /> {selectedDevice?.last_battery_percent ?? '—'}%</strong></div><div><small>GPS ACCESS</small><strong><LocateFixed size={13} /> {selectedDevice?.location_permission}</strong></div><div><small>BACKGROUND</small><strong><Radio size={13} /> {selectedDevice?.background_permission ? 'Allowed' : 'Limited'}</strong></div></div></div><div className="player-display-panel"><small>CAR PLAYER SCREEN</small><h3>Choose the calm display</h3><p>Change what the head unit shows while driving. These controls stay on the owner screen.</p><div>{([{ id: 'journey', label: 'Journey', icon: <Route /> }, { id: 'speed', label: 'Speed', icon: <Gauge /> }, { id: 'clock', label: 'Clock', icon: <Clock3 /> }, { id: 'map', label: 'Map side', icon: <MapPin /> }] as const).map((mode) => <button type="button" disabled={displaySaving} className={selectedDevice?.display_mode === mode.id ? 'active' : ''} key={mode.id} onClick={() => void setPlayerDisplay(mode.id)}>{mode.icon}<span>{mode.label}</span>{selectedDevice?.display_mode === mode.id && <Check />}</button>)}</div>{displayMessage && <span className="display-message">{displayMessage}</span>}</div><div className="tracker-setup-panel"><h3>Consent stays visible.</h3><p>The vehicle player uses an Android location foreground service. Its persistent notification lets anyone using the player see and stop tracking.</p><ol><li>Location is connected only to the selected vehicle.</li><li>Offline points wait securely on the device.</li><li>Mock-location flags are retained for review.</li></ol></div></aside>
    </section>
    <div className="tracking-tabs"><button className="active">TRIP HISTORY ({deviceSessions.length})</button></div>
    <div className="trip-history-list">{deviceSessions.length ? deviceSessions.map((session, index) => <button className="trip-row" key={session.id} onClick={() => void openSession(session)}><span>{String(index + 1).padStart(2, '0')}</span><span className="trip-main"><strong>{session.vehicles?.registration_number || 'Vehicle journey'}</strong><small>{new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(session.started_at))} · {session.status}</small></span><span className="trip-metric"><small>DISTANCE</small><strong>{(session.distance_m / 1000).toFixed(1)} km</strong></span><span className="trip-metric"><small>DURATION</small><strong>{duration(session.duration_seconds)}</strong></span><span className="trip-metric"><small>MAX SPEED</small><strong>{kmh(session.max_speed_mps)} km/h</strong></span><span className="trip-metric"><small>POINTS</small><strong>{session.point_count}</strong></span><ArrowRight size={16} /></button>) : <div className="empty-state"><Route size={35} /><h3>No journeys recorded</h3><p>A completed vehicle-player session will appear here with its route and speed history.</p></div>}</div>
  </div>
}
