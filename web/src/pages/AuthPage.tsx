import { useEffect, useState, type FormEvent } from 'react'
import { ArrowLeft, ArrowRight, Check, Eye, EyeOff, LockKeyhole, Mail, Phone, ShieldCheck, UserRound } from 'lucide-react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { Brand } from '../components/Brand'
import { Button, Toast } from '../components/Ui'
import { useAuth } from '../context'
import { humanError, supabase } from '../lib/supabase'

export function AuthPage({ mode }: { mode: 'login' | 'signup' }) {
  const isSignup = mode === 'signup'
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [form, setForm] = useState({ fullName: '', email: '', phone: '', emergencyPhone: '', password: '' })
  const { user, loading: authLoading } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    if (!authLoading && user) navigate('/app', { replace: true })
  }, [user, authLoading, navigate])

  function update(key: keyof typeof form, value: string) {
    setForm((current) => ({ ...current, [key]: value }))
  }

  async function submit(event: FormEvent) {
    event.preventDefault()
    setLoading(true)
    setMessage(null)
    try {
      if (isSignup) {
        const { data, error } = await supabase.auth.signUp({
          email: form.email.trim(),
          password: form.password,
          options: {
            data: {
              full_name: form.fullName.trim(),
              phone: form.phone.trim(),
              emergency_contact_phone: form.emergencyPhone.trim(),
            },
          },
        })
        if (error) throw error
        if (!data.session || !data.user) {
          throw new Error('Your account could not be started immediately. RoadPing registration does not require email confirmation.')
        }
        const { error: profileError } = await supabase.from('profiles').upsert({
          id: data.user.id,
          full_name: form.fullName.trim(),
          phone: form.phone.trim(),
          emergency_contact_phone: form.emergencyPhone.trim(),
          phone_visibility: 'hidden',
        })
        if (profileError) throw profileError
        navigate('/app', { replace: true })
      } else {
        const { error } = await supabase.auth.signInWithPassword({ email: form.email.trim(), password: form.password })
        if (error) throw error
        const destination = (location.state as { from?: string } | null)?.from || '/app'
        navigate(destination, { replace: true })
      }
    } catch (error) {
      setMessage({ type: 'error', text: humanError(error) })
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="auth-page">
      <div className="auth-brand-panel">
        <div className="auth-panel-top"><Brand /><Link to="/"><ArrowLeft size={16} /> Back to home</Link></div>
        <div className="auth-panel-content">
          <div className="auth-shield"><ShieldCheck /></div>
          <span className="section-kicker">YOUR PRIVATE VEHICLE ID</span>
          <h2>Help can reach you.<br />Your number doesn&apos;t have to.</h2>
          <p>Every RoadPing account is designed around owner privacy and fast, useful alerts.</p>
          <ul><li><Check /> One account for all your vehicles</li><li><Check /> Unique QR identity per vehicle</li><li><Check /> Privacy mode on by default</li></ul>
        </div>
        <small>RoadPing · Secure vehicle contact</small>
      </div>

      <div className="auth-form-panel">
        <div className="mobile-auth-brand"><Brand /><Link to="/"><ArrowLeft size={18} /></Link></div>
        <div className="auth-form-wrap">
          <span className="auth-icon"><LockKeyhole /></span>
          <h1>{isSignup ? 'Create your owner account' : 'Welcome back'}</h1>
          <p>{isSignup ? 'Your first smart vehicle card is only a few details away.' : 'Sign in to manage vehicles, alerts and privacy.'}</p>
          {message && <Toast type={message.type} onClose={() => setMessage(null)}>{message.text}</Toast>}
          <form onSubmit={submit}>
            {isSignup && <label><span>Full name</span><div className="input-wrap"><UserRound size={18} /><input required autoComplete="name" value={form.fullName} onChange={(e) => update('fullName', e.target.value)} placeholder="e.g. Nimal Perera" /></div></label>}
            <label><span>Email address</span><div className="input-wrap"><Mail size={18} /><input required type="email" autoComplete="email" value={form.email} onChange={(e) => update('email', e.target.value)} placeholder="you@example.com" /></div></label>
            {isSignup && <div className="two-fields"><label><span>Phone number</span><div className="input-wrap"><Phone size={18} /><input required type="tel" autoComplete="tel" value={form.phone} onChange={(e) => update('phone', e.target.value)} placeholder="+94 77 123 4567" /></div></label><label><span>Emergency contact</span><div className="input-wrap"><Phone size={18} /><input required type="tel" value={form.emergencyPhone} onChange={(e) => update('emergencyPhone', e.target.value)} placeholder="+94 71 987 6543" /></div></label></div>}
            <label><span>Password</span><div className="input-wrap"><LockKeyhole size={18} /><input required minLength={8} type={showPassword ? 'text' : 'password'} autoComplete={isSignup ? 'new-password' : 'current-password'} value={form.password} onChange={(e) => update('password', e.target.value)} placeholder="At least 8 characters" /><button type="button" className="input-action" onClick={() => setShowPassword(!showPassword)} aria-label="Toggle password visibility">{showPassword ? <EyeOff size={18} /> : <Eye size={18} />}</button></div></label>
            <Button type="submit" className="full auth-submit" loading={loading}>{isSignup ? 'Create secure account' : 'Sign in to RoadPing'} <ArrowRight size={17} /></Button>
          </form>
          <p className="auth-switch">{isSignup ? 'Already have an account?' : 'New to RoadPing?'} <Link to={isSignup ? '/login' : '/signup'}>{isSignup ? 'Log in' : 'Create an account'}</Link></p>
          {isSignup && <small className="terms">By creating an account, you agree to use RoadPing responsibly and not include sensitive personal details in public card fields.</small>}
        </div>
      </div>
    </div>
  )
}
