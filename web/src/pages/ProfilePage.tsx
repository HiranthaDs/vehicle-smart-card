import { useEffect, useState, type FormEvent } from 'react'
import { AtSign, BellRing, BriefcaseBusiness, Camera, Eye, EyeOff, LockKeyhole, LogOut, Save, ShieldCheck, UserRound } from 'lucide-react'
import { Button, PageTitle, Toast } from '../components/Ui'
import { useAuth } from '../context'
import { enablePush } from '../lib/notifications'
import { humanError, supabase } from '../lib/supabase'
import type { PhoneVisibility, Profile } from '../types'

export function ProfilePage() {
  const { user, profile, refreshProfile, signOut } = useAuth()
  const [form, setForm] = useState({ full_name: '', phone: '', emergency_contact_name: '', emergency_contact_phone: '', phone_visibility: 'hidden' as PhoneVisibility, facebook: '', instagram: '', linkedin: '' })
  const [saving, setSaving] = useState(false)
  const [pushLoading, setPushLoading] = useState(false)
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null)

  useEffect(() => {
    if (!profile) return
    setForm({ full_name: profile.full_name || '', phone: profile.phone || '', emergency_contact_name: profile.emergency_contact_name || '', emergency_contact_phone: profile.emergency_contact_phone || '', phone_visibility: profile.phone_visibility || 'hidden', facebook: profile.social_links?.facebook || '', instagram: profile.social_links?.instagram || '', linkedin: profile.social_links?.linkedin || '' })
  }, [profile])

  function update(key: keyof typeof form, value: string) { setForm((current) => ({ ...current, [key]: value })) }

  async function save(event: FormEvent) {
    event.preventDefault()
    if (!user) return
    setSaving(true)
    try {
      const payload: Partial<Profile> = { full_name: form.full_name.trim(), phone: form.phone.trim(), emergency_contact_name: form.emergency_contact_name.trim(), emergency_contact_phone: form.emergency_contact_phone.trim(), phone_visibility: form.phone_visibility, social_links: { facebook: form.facebook.trim(), instagram: form.instagram.trim(), linkedin: form.linkedin.trim() } }
      const { error } = await supabase.from('profiles').update(payload).eq('id', user.id)
      if (error) throw error
      await refreshProfile()
      setNotice({ type: 'success', text: 'Profile and privacy settings saved.' })
    } catch (error) { setNotice({ type: 'error', text: humanError(error) }) } finally { setSaving(false) }
  }

  async function activatePush() {
    if (!user) return
    setPushLoading(true)
    try {
      const result = await enablePush(user.id)
      await supabase.from('profiles').update({ push_enabled: true }).eq('id', user.id)
      await refreshProfile()
      setNotice({ type: 'success', text: result.permissionOnly ? 'Notification permission enabled. Configure VAPID to turn on background web push.' : 'Background notifications enabled.' })
    } catch (error) { setNotice({ type: 'error', text: humanError(error) }) } finally { setPushLoading(false) }
  }

  return <div className="app-page profile-page"><PageTitle eyebrow="ACCOUNT SETTINGS" title="Profile & privacy">Control the information used across all of your vehicle cards.</PageTitle>{notice && <Toast type={notice.type} onClose={() => setNotice(null)}>{notice.text}</Toast>}<form onSubmit={save}><section className="settings-panel panel"><div className="settings-heading"><span><UserRound /></span><div><h2>Owner details</h2><p>Private contact details used to deliver important alerts.</p></div></div><div className="form-grid two"><label><span>Full name</span><input required value={form.full_name} onChange={(e) => update('full_name', e.target.value)} /></label><label><span>Email</span><input disabled value={user?.email || ''} /></label><label><span>Phone number</span><input required type="tel" value={form.phone} onChange={(e) => update('phone', e.target.value)} /></label><label><span>Emergency contact name</span><input value={form.emergency_contact_name} onChange={(e) => update('emergency_contact_name', e.target.value)} placeholder="Contact name" /></label><label><span>Emergency contact number</span><input required type="tel" value={form.emergency_contact_phone} onChange={(e) => update('emergency_contact_phone', e.target.value)} /></label></div></section><section className="settings-panel panel"><div className="settings-heading"><span><ShieldCheck /></span><div><h2>Phone privacy</h2><p>Choose what a person sees after scanning any of your cards.</p></div></div><div className="privacy-choice-grid">{([{ id: 'hidden', icon: EyeOff, title: 'Protected', text: 'No number shown. Scanner sends an alert.' }, { id: 'masked', icon: LockKeyhole, title: 'Masked', text: 'Only a few digits are shown; alerts still preferred.' }, { id: 'visible', icon: Eye, title: 'Visible', text: 'Display a direct call button on public pages.' }] as const).map(({ id, icon: Icon, title, text }) => <button type="button" key={id} className={form.phone_visibility === id ? 'selected' : ''} onClick={() => update('phone_visibility', id)}><Icon /><span><strong>{title}</strong><small>{text}</small></span><i /></button>)}</div></section><section className="settings-panel panel"><div className="settings-heading"><span><AtSign /></span><div><h2>Social links</h2><p>Optional links appear on the public vehicle page.</p></div></div><div className="social-inputs"><label><AtSign /><input type="url" value={form.facebook} onChange={(e) => update('facebook', e.target.value)} placeholder="https://facebook.com/yourprofile" /></label><label><Camera /><input type="url" value={form.instagram} onChange={(e) => update('instagram', e.target.value)} placeholder="https://instagram.com/yourprofile" /></label><label><BriefcaseBusiness /><input type="url" value={form.linkedin} onChange={(e) => update('linkedin', e.target.value)} placeholder="https://linkedin.com/in/yourprofile" /></label></div></section><section className="settings-panel panel notification-settings"><div className="settings-heading"><span><BellRing /></span><div><h2>Device notifications</h2><p>Allow RoadPing to show incoming vehicle alerts on this device.</p></div></div><Button type="button" className="secondary" loading={pushLoading} onClick={activatePush}>{profile?.push_enabled ? 'Re-enable on this device' : 'Enable notifications'}</Button></section><div className="settings-footer"><Button type="submit" loading={saving}><Save size={17} /> Save all changes</Button><button type="button" className="danger-link" onClick={() => void signOut()}><LogOut size={17} /> Sign out</button></div></form></div>
}
