import { useEffect, useState, type FormEvent } from 'react'
import { ArrowLeft, CarFront, Check, Palette, Save } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Button, PageTitle, Toast } from '../components/Ui'
import { VehicleCard } from '../components/VehicleCard'
import { useAuth } from '../context'
import { formatRegistration, humanError, supabase } from '../lib/supabase'
import type { Vehicle } from '../types'

const themes = [
  { id: 'midnight', label: 'Midnight' },
  { id: 'ocean', label: 'Ocean' },
  { id: 'ember', label: 'Ember' },
  { id: 'forest', label: 'Forest' },
  { id: 'sand', label: 'Sand' },
  { id: 'violet', label: 'Violet' },
]

const layouts = [
  { id: 'balanced', label: 'Balanced', description: 'Vehicle and QR share the card.' },
  { id: 'qr_focus', label: 'Large QR', description: 'Best for quick roadside scanning.' },
  { id: 'minimal', label: 'Minimal', description: 'Quiet, clean and print friendly.' },
]

const emptyForm = { registration_number: '', nickname: '', vehicle_type: 'Car', make: '', model: '', color: '', year: '', card_theme: 'midnight', card_layout: 'balanced', is_active: true }

export function VehicleFormPage() {
  const { id } = useParams()
  const editing = Boolean(id)
  const { user, profile } = useAuth()
  const navigate = useNavigate()
  const [form, setForm] = useState(emptyForm)
  const [loading, setLoading] = useState(editing)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')

  useEffect(() => {
    if (!id || !user) return
    supabase.from('vehicles').select('*').eq('id', id).eq('owner_id', user.id).single().then(({ data, error: queryError }) => {
      if (queryError) setError(queryError.message)
      if (data) setForm({
        registration_number: data.registration_number,
        nickname: data.nickname || '',
        vehicle_type: data.vehicle_type || 'Car',
        make: data.make || '',
        model: data.model || '',
        color: data.color || '',
        year: data.year?.toString() || '',
        card_theme: data.card_theme || 'midnight',
        card_layout: data.card_layout || 'balanced',
        is_active: data.is_active,
      })
      setLoading(false)
    })
  }, [id, user])

  function update(key: keyof typeof form, value: string | boolean) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    if (!user) return
    setSaving(true)
    setError('')
    try {
      const registration = formatRegistration(form.registration_number).trim()
      if (registration.length < 2) throw new Error('Enter the registration shown on the number plate.')
      const payload = { ...form, registration_number: registration, year: form.year ? Number(form.year) : null, owner_id: user.id }
      const query = editing ? supabase.from('vehicles').update(payload).eq('id', id!).eq('owner_id', user.id).select().single() : supabase.from('vehicles').insert(payload).select().single()
      const { data, error: mutationError } = await query
      if (mutationError) throw mutationError
      navigate(`/app/vehicles/${data.id}`, { replace: true })
    } catch (mutationError) {
      setError(humanError(mutationError))
    } finally { setSaving(false) }
  }

  const previewVehicle = { ...form, public_token: 'preview', year: form.year ? Number(form.year) : null } as unknown as Vehicle

  if (loading) return <div className="app-page"><div className="page-loading">Loading vehicle…</div></div>

  return (
    <div className="app-page vehicle-form-page">
      <Link to="/app/vehicles" className="back-link"><ArrowLeft size={17} /> Back to vehicles</Link>
      <PageTitle eyebrow={editing ? 'UPDATE VEHICLE' : 'NEW SMART CARD'} title={editing ? 'Edit vehicle' : 'Add a vehicle'}>The registration number is the only detail shown prominently on the printed card.</PageTitle>
      {error && <Toast type="error" onClose={() => setError('')}>{error}</Toast>}
      <div className="vehicle-form-grid">
        <form className="panel form-panel" onSubmit={submit}>
          <div className="form-section-title"><span><CarFront /></span><div><h3>Vehicle details</h3><p>Add enough detail to help a scanner identify the right vehicle.</p></div></div>
          <div className="form-grid two">
            <label className="wide registration-field"><span>Vehicle registration number *</span><input required maxLength={18} autoCapitalize="characters" value={form.registration_number} onChange={(e) => update('registration_number', formatRegistration(e.target.value))} placeholder="CAB-2048 or 64-1234" /><small>Use the main plate number only. Letters and older numeric formats are accepted.</small></label>
            <label><span>Nickname</span><input value={form.nickname} onChange={(e) => update('nickname', e.target.value)} placeholder="Daily ride" /></label>
            <label><span>Vehicle type</span><select value={form.vehicle_type} onChange={(e) => update('vehicle_type', e.target.value)}><option>Car</option><option>Motorbike</option><option>Van</option><option>Three-wheeler</option><option>Truck</option><option>Bus</option><option>Other</option></select></label>
            <label><span>Make</span><input value={form.make} onChange={(e) => update('make', e.target.value)} placeholder="Toyota" /></label>
            <label><span>Model</span><input value={form.model} onChange={(e) => update('model', e.target.value)} placeholder="Corolla" /></label>
            <label><span>Colour</span><input value={form.color} onChange={(e) => update('color', e.target.value)} placeholder="Pearl white" /></label>
            <label><span>Year</span><input type="number" min="1950" max="2100" value={form.year} onChange={(e) => update('year', e.target.value)} placeholder="2022" /></label>
          </div>
          <div className="form-section-title theme-title"><span><Palette /></span><div><h3>Card style</h3><p>Pick a high-contrast look for your printed smart card.</p></div></div>
          <div className="theme-picker">{themes.map((theme) => <button type="button" className={`${theme.id} ${form.card_theme === theme.id ? 'selected' : ''}`} key={theme.id} onClick={() => update('card_theme', theme.id)}><i />{theme.label}{form.card_theme === theme.id && <Check />}</button>)}</div>
          <div className="layout-picker" aria-label="Card layout">{layouts.map((layout) => <button type="button" className={form.card_layout === layout.id ? 'selected' : ''} key={layout.id} onClick={() => update('card_layout', layout.id)}><span>{layout.label}</span><small>{layout.description}</small>{form.card_layout === layout.id && <Check />}</button>)}</div>
          <label className="toggle-row"><span><strong>Card active</strong><small>When paused, public reports are disabled.</small></span><input type="checkbox" checked={form.is_active} onChange={(e) => update('is_active', e.target.checked)} /><i /></label>
          <div className="form-actions"><Link className="button ghost" to="/app/vehicles">Cancel</Link><Button type="submit" loading={saving}><Save size={17} /> {editing ? 'Save changes' : 'Create smart card'}</Button></div>
        </form>
        <aside className="sticky-preview"><span className="eyebrow">LIVE CARD PREVIEW</span><VehicleCard vehicle={previewVehicle} ownerName={profile?.full_name || 'Protected owner'} /><p>Your actual QR code is created after the vehicle is saved.</p></aside>
      </div>
    </div>
  )
}
