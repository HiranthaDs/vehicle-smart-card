import { useMemo, useState } from 'react'
import { AlertTriangle, BellRing, Check, CheckCircle2, Clock3, ExternalLink, LocateFixed, MailCheck, MessageSquareText, Phone } from 'lucide-react'
import { Button, EmptyState, PageTitle, Skeleton } from '../components/Ui'
import { useAuth } from '../context'
import { useAlerts } from '../lib/data'
import { supabase } from '../lib/supabase'
import type { IncidentAlert } from '../types'

const typeMeta: Record<string, { label: string; icon: string; tone: string }> = {
  blocking_access: { label: 'Blocking access', icon: '🚗', tone: 'blue' },
  parking_issue: { label: 'Parking issue', icon: '📍', tone: 'orange' },
  lights_on: { label: 'Lights left on', icon: '💡', tone: 'yellow' },
  accident_damage: { label: 'Accident or damage', icon: '⚠️', tone: 'red' },
  vehicle_attention: { label: 'Vehicle needs attention', icon: '🔧', tone: 'purple' },
  emergency: { label: 'Emergency', icon: '🆘', tone: 'red' },
  other: { label: 'Other message', icon: '💬', tone: 'gray' },
}

export function AlertsPage() {
  const { user } = useAuth()
  const { alerts, loading, refresh } = useAlerts(user?.id)
  const [filter, setFilter] = useState<'all' | 'new' | 'resolved'>('all')
  const [selected, setSelected] = useState<IncidentAlert | null>(null)
  const filtered = useMemo(() => alerts.filter((alert) => filter === 'all' || alert.status === filter), [alerts, filter])

  async function setStatus(alert: IncidentAlert, status: IncidentAlert['status']) {
    await supabase.from('incident_alerts').update({ status }).eq('id', alert.id).eq('owner_id', user!.id)
    setSelected((current) => current?.id === alert.id ? { ...current, status } : current)
    await refresh()
  }

  return (
    <div className="app-page alerts-page">
      <PageTitle eyebrow="OWNER INBOX" title="Road alerts">Every report is attached to the correct vehicle and kept here until you resolve it.</PageTitle>
      <div className="filter-tabs">{(['all', 'new', 'resolved'] as const).map((value) => <button key={value} className={filter === value ? 'active' : ''} onClick={() => setFilter(value)}>{value === 'all' ? `All (${alerts.length})` : value === 'new' ? `New (${alerts.filter((a) => a.status === 'new').length})` : `Resolved (${alerts.filter((a) => a.status === 'resolved').length})`}</button>)}</div>
      {loading ? <Skeleton className="alerts-skeleton" /> : filtered.length ? <div className="alerts-layout"><div className="alert-list">{filtered.map((alert) => { const meta = typeMeta[alert.alert_type] || typeMeta.other; return <button key={alert.id} className={`${selected?.id === alert.id ? 'selected' : ''} ${alert.status === 'new' ? 'unread' : ''}`} onClick={() => { setSelected(alert); if (alert.status === 'new') void setStatus(alert, 'seen') }}><span className={`alert-emoji ${meta.tone}`}>{meta.icon}</span><div><div className="alert-row-title"><strong>{meta.label}</strong>{alert.status === 'new' && <i>NEW</i>}</div><p>{alert.vehicles?.registration_number || 'Vehicle'} · {alert.message || 'No additional message'}</p><time><Clock3 size={13} /> {new Intl.DateTimeFormat(undefined, { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(alert.created_at))}</time></div></button>})}</div>{selected ? <AlertDetail alert={selected} onStatus={setStatus} /> : <div className="detail-placeholder"><MailCheck /><h3>Select an alert</h3><p>Open a report to see its message, location and status.</p></div>}</div> : <EmptyState icon={<BellRing />} title="No alerts in this view">When someone scans a vehicle card and sends a report, it will appear here.</EmptyState>}
    </div>
  )
}

function AlertDetail({ alert, onStatus }: { alert: IncidentAlert; onStatus: (alert: IncidentAlert, status: IncidentAlert['status']) => Promise<void> }) {
  const meta = typeMeta[alert.alert_type] || typeMeta.other
  return <aside className="alert-detail panel"><div className="detail-head"><span className={`alert-emoji ${meta.tone}`}>{meta.icon}</span><div><span className="eyebrow">{alert.vehicles?.registration_number}</span><h2>{meta.label}</h2></div></div><div className="detail-message"><MessageSquareText /><div><small>REPORTER MESSAGE</small><p>{alert.message || 'No additional details were included.'}</p></div></div>{alert.reporter_contact && <div className="detail-line"><Phone /><div><small>CALLBACK CONTACT</small><p>{alert.reporter_contact}</p></div></div>}{alert.latitude && alert.longitude && <a className="detail-line map-link" href={`https://www.google.com/maps?q=${alert.latitude},${alert.longitude}`} target="_blank" rel="noreferrer"><LocateFixed /><div><small>REPORTED LOCATION</small><p>Open map coordinates</p></div><ExternalLink /></a>}<div className="detail-line"><Clock3 /><div><small>RECEIVED</small><p>{new Intl.DateTimeFormat(undefined, { dateStyle: 'full', timeStyle: 'short' }).format(new Date(alert.created_at))}</p></div></div><div className="detail-actions">{alert.status !== 'resolved' ? <Button className="full" onClick={() => onStatus(alert, 'resolved')}><CheckCircle2 size={17} /> Mark resolved</Button> : <Button className="full secondary" onClick={() => onStatus(alert, 'seen')}><AlertTriangle size={17} /> Reopen alert</Button>}<span className={`status-badge ${alert.status}`}><Check size={13} /> {alert.status}</span></div></aside>
}
