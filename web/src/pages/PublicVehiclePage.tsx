import { useEffect, useState, type FormEvent } from 'react'
import { AlertTriangle, ArrowLeft, ArrowRight, AtSign, BriefcaseBusiness, Camera, CheckCircle2, EyeOff, LocateFixed, MapPin, MessageSquareText, Phone, ShieldCheck } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { Brand } from '../components/Brand'
import { Button } from '../components/Ui'
import { humanError, supabase } from '../lib/supabase'
import type { PublicVehicleProfile } from '../types'

const demoProfile: PublicVehicleProfile = { vehicle_id: 'demo', public_token: 'demo-roadping', registration_number: 'WP CAB-2048', make: 'Toyota', model: 'Corolla', color: 'Pearl white', vehicle_type: 'Car', owner_name: 'Nimal P.', phone_display: '077 ••• •48', phone_href: null, social_links: {} }

const alertTypes = [
  { id: 'blocking_access', icon: '🚗', label: 'Blocking access', detail: 'The vehicle is blocking a gate or driveway' },
  { id: 'parking_issue', icon: '📍', label: 'Parking issue', detail: 'The vehicle may need to be moved' },
  { id: 'lights_on', icon: '💡', label: 'Lights left on', detail: 'Lights or an indicator are still on' },
  { id: 'accident_damage', icon: '⚠️', label: 'Accident / damage', detail: 'The vehicle was involved in an incident' },
  { id: 'vehicle_attention', icon: '🔧', label: 'Needs attention', detail: 'Tyre, window or another issue noticed' },
  { id: 'other', icon: '💬', label: 'Other message', detail: 'Send a different useful message' },
]

export function PublicVehiclePage() {
  const { token } = useParams()
  const [profile, setProfile] = useState<PublicVehicleProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [notFound, setNotFound] = useState(false)
  const [step, setStep] = useState<'choose' | 'details' | 'sent'>('choose')
  const [alertType, setAlertType] = useState('')
  const [message, setMessage] = useState('')
  const [contact, setContact] = useState('')
  const [location, setLocation] = useState<{ latitude: number; longitude: number } | null>(null)
  const [locationLoading, setLocationLoading] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (token === 'demo-roadping') { setProfile(demoProfile); setLoading(false); return }
    if (!token) return
    supabase.rpc('get_public_vehicle_profile', { p_public_token: token }).then(({ data, error: queryError }) => {
      const result = Array.isArray(data) ? data[0] : data
      if (queryError || !result) setNotFound(true)
      else setProfile(result as PublicVehicleProfile)
      setLoading(false)
    })
  }, [token])

  function chooseType(id: string) { setAlertType(id); setStep('details'); window.scrollTo({ top: 0, behavior: 'smooth' }) }

  function requestLocation() {
    if (!navigator.geolocation) { setError('Location is not supported on this device.'); return }
    setLocationLoading(true)
    navigator.geolocation.getCurrentPosition(
      ({ coords }) => { setLocation({ latitude: coords.latitude, longitude: coords.longitude }); setLocationLoading(false) },
      () => { setError('Location permission was not granted. You can still send the alert.'); setLocationLoading(false) },
      { enableHighAccuracy: true, timeout: 10000 },
    )
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!token || !alertType) return
    setSubmitting(true); setError('')
    try {
      if (token !== 'demo-roadping') {
        const { error: submitError } = await supabase.rpc('submit_public_alert', {
          p_public_token: token,
          p_alert_type: alertType,
          p_message: message.trim(),
          p_reporter_contact: contact.trim() || null,
          p_latitude: location?.latitude || null,
          p_longitude: location?.longitude || null,
        })
        if (submitError) throw submitError
      } else await new Promise((resolve) => setTimeout(resolve, 650))
      setStep('sent')
    } catch (submitError) { setError(humanError(submitError)) } finally { setSubmitting(false) }
  }

  if (loading) return <div className="public-scan-page centered-screen"><div className="scan-loader"><i /><span>Opening secure vehicle card…</span></div></div>
  if (notFound || !profile) return <div className="public-scan-page centered-screen"><Brand /><div className="invalid-card"><AlertTriangle /><h1>Card unavailable</h1><p>This vehicle card is invalid, paused or no longer active.</p><Link className="button" to="/">Visit RoadPing</Link></div></div>

  const selectedMeta = alertTypes.find((type) => type.id === alertType)

  return <div className="public-scan-page"><header className="public-header"><Brand /><span><ShieldCheck size={14} /> Secure vehicle contact</span></header><main className="public-scan-main"><section className="public-vehicle-hero"><span className="scan-verified"><ShieldCheck size={14} /> ROADPING VERIFIED CARD</span><p>YOU&apos;RE CONTACTING THE OWNER OF</p><strong>{profile.registration_number}</strong><small>{[profile.color, profile.make, profile.model].filter(Boolean).join(' · ')}</small><div className="public-owner"><span>{profile.owner_name?.slice(0, 1) || 'O'}</span><div><strong>{profile.owner_name || 'Vehicle owner'}</strong><small>{profile.phone_display ? profile.phone_display : 'Phone number protected'}</small></div>{profile.phone_href ? <a href={profile.phone_href} aria-label="Call owner"><Phone /></a> : <EyeOff />}</div>{Object.values(profile.social_links || {}).some(Boolean) && <div className="public-socials">{profile.social_links.facebook && <a href={profile.social_links.facebook} target="_blank" rel="noreferrer"><AtSign /></a>}{profile.social_links.instagram && <a href={profile.social_links.instagram} target="_blank" rel="noreferrer"><Camera /></a>}{profile.social_links.linkedin && <a href={profile.social_links.linkedin} target="_blank" rel="noreferrer"><BriefcaseBusiness /></a>}</div>}</section>{step === 'choose' && <section className="scan-card"><div className="scan-step"><span>1</span><div><h1>What should the owner know?</h1><p>Choose the closest match. Your report is sent privately.</p></div></div><div className="public-alert-options">{alertTypes.map((type) => <button key={type.id} onClick={() => chooseType(type.id)}><span>{type.icon}</span><div><strong>{type.label}</strong><small>{type.detail}</small></div><ArrowRight /></button>)}</div><p className="abuse-note"><ShieldCheck size={14} /> Reports are rate-limited and stored for safety.</p></section>}{step === 'details' && <section className="scan-card"><button className="back-link" onClick={() => setStep('choose')}><ArrowLeft size={17} /> Change alert type</button><div className="selected-alert"><span>{selectedMeta?.icon}</span><div><small>YOU&apos;RE REPORTING</small><strong>{selectedMeta?.label}</strong></div></div><form onSubmit={submit}><label><span><MessageSquareText size={16} /> Add useful details</span><textarea maxLength={500} value={message} onChange={(e) => setMessage(e.target.value)} placeholder="Example: The left rear window is open and rain is starting." /><small>{message.length}/500</small></label><label><span><Phone size={16} /> Your contact number <i>optional</i></span><input type="tel" value={contact} onChange={(e) => setContact(e.target.value)} placeholder="Only shared with the owner" /></label><div className={`location-option ${location ? 'added' : ''}`}><span><MapPin /></span><div><strong>{location ? 'Location added' : 'Add current location'}</strong><small>{location ? 'The owner will receive map coordinates.' : 'Helpful for an accident or moved vehicle.'}</small></div><Button type="button" className="ghost small" loading={locationLoading} onClick={requestLocation}>{location ? 'Update' : 'Add'}</Button></div>{error && <div className="inline-error">{error}</div>}<Button type="submit" className="full send-alert" loading={submitting}>Send private alert <ArrowRight size={17} /></Button><p className="form-privacy"><ShieldCheck size={13} /> Your message goes only to this vehicle&apos;s owner.</p></form></section>}{step === 'sent' && <section className="scan-card success-card"><span className="sent-icon"><CheckCircle2 /></span><span className="eyebrow">ALERT DELIVERED</span><h1>The owner has been notified.</h1><p>Your <strong>{selectedMeta?.label.toLowerCase()}</strong> report was saved with the vehicle and is now visible in the owner&apos;s alert inbox.</p>{location && <div className="success-detail"><LocateFixed /><span><strong>Location included</strong><small>The owner can open the reported coordinates.</small></span></div>}<button className="button secondary full" onClick={() => { setStep('choose'); setAlertType(''); setMessage(''); setContact(''); setLocation(null) }}>Send another report</button>{token === 'demo-roadping' && <div className="demo-notice">This was an interactive demo. No real owner was contacted.</div>}</section>}</main><footer className="public-footer"><Brand compact /><span>Private contact powered by RoadPing</span><Link to="/">Create your own card</Link></footer></div>
}
