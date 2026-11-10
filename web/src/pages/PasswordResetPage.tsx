import { useState, type FormEvent } from 'react'
import { ArrowLeft, ArrowRight, Check, Eye, EyeOff, KeyRound, LockKeyhole, Mail, ShieldCheck } from 'lucide-react'
import { Link, useNavigate } from 'react-router-dom'
import { Brand } from '../components/Brand'
import { Button, Toast } from '../components/Ui'
import { humanError, supabase } from '../lib/supabase'

type Step = 'email' | 'otp' | 'password' | 'done'

export function PasswordResetPage() {
  const [step, setStep] = useState<Step>('email')
  const [email, setEmail] = useState('')
  const [otp, setOtp] = useState('')
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const navigate = useNavigate()

  async function sendOtp(event?: FormEvent) {
    event?.preventDefault()
    setLoading(true); setMessage(null)
    try {
      const { error } = await supabase.auth.resetPasswordForEmail(email.trim())
      if (error) throw error
      setStep('otp')
      setMessage({ type: 'success', text: 'A six-digit recovery code was sent to your email.' })
    } catch (error) { setMessage({ type: 'error', text: humanError(error) }) } finally { setLoading(false) }
  }

  async function verifyOtp(event: FormEvent) {
    event.preventDefault()
    setLoading(true); setMessage(null)
    try {
      const { error } = await supabase.auth.verifyOtp({ email: email.trim(), token: otp.trim(), type: 'recovery' })
      if (error) throw error
      setStep('password')
    } catch (error) { setMessage({ type: 'error', text: humanError(error) }) } finally { setLoading(false) }
  }

  async function updatePassword(event: FormEvent) {
    event.preventDefault()
    if (password.length < 8) return setMessage({ type: 'error', text: 'Use at least 8 characters.' })
    if (password !== confirmPassword) return setMessage({ type: 'error', text: 'The passwords do not match.' })
    setLoading(true); setMessage(null)
    try {
      const { error } = await supabase.auth.updateUser({ password })
      if (error) throw error
      await supabase.auth.signOut()
      setStep('done')
    } catch (error) { setMessage({ type: 'error', text: humanError(error) }) } finally { setLoading(false) }
  }

  return <div className="auth-page reset-page">
    <div className="auth-brand-panel"><div className="auth-panel-top"><Brand /><Link to="/"><ArrowLeft size={16} /> Back to home</Link></div><div className="auth-panel-content"><div className="auth-shield"><ShieldCheck /></div><span className="section-kicker">ACCOUNT RECOVERY</span><h2>ONE CODE.<br />BACK IN CONTROL.</h2><p>Recovery codes are used only when you forget your password. Creating a RoadPing account never asks for an OTP.</p><ul><li><Check /> Code expires automatically</li><li><Check /> Password stays encrypted</li><li><Check /> Existing vehicle data remains safe</li></ul></div><small>RoadPing / Secure recovery by Hich</small></div>
    <div className="auth-form-panel"><div className="mobile-auth-brand"><Brand /><Link to="/login"><ArrowLeft size={18} /></Link></div><div className="auth-form-wrap"><span className="auth-icon">{step === 'email' ? <Mail /> : step === 'otp' ? <KeyRound /> : <LockKeyhole />}</span><h1>{step === 'email' ? 'Reset your password' : step === 'otp' ? 'Enter recovery code' : step === 'password' ? 'Choose a new password' : 'Password changed'}</h1><p>{step === 'email' ? 'We will email a six-digit OTP to your registered address.' : step === 'otp' ? `Enter the code sent to ${email}.` : step === 'password' ? 'Use a strong password you have not used before.' : 'You can now sign in with your new password.'}</p><div className="otp-steps"><i className="active" /><i className={step !== 'email' ? 'active' : ''} /><i className={step === 'password' || step === 'done' ? 'active' : ''} /></div>{message && <Toast type={message.type} onClose={() => setMessage(null)}>{message.text}</Toast>}
      {step === 'email' && <form onSubmit={sendOtp}><label><span>Email address</span><div className="input-wrap"><Mail size={18} /><input required type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} placeholder="you@example.com" /></div></label><Button type="submit" className="full auth-submit" loading={loading}>Send recovery OTP <ArrowRight size={17} /></Button></form>}
      {step === 'otp' && <form onSubmit={verifyOtp}><label><span>Six-digit recovery code</span><div className="input-wrap"><KeyRound size={18} /><input className="otp-code-input" required inputMode="numeric" autoComplete="one-time-code" minLength={6} maxLength={6} pattern="[0-9]{6}" value={otp} onChange={(event) => setOtp(event.target.value.replace(/\D/g, '').slice(0, 6))} placeholder="000000" /></div></label><Button type="submit" className="full auth-submit" loading={loading}>Verify code <ArrowRight size={17} /></Button><button className="forgot-link reset-resend" type="button" disabled={loading} onClick={() => void sendOtp()}>Send a new code</button></form>}
      {step === 'password' && <form onSubmit={updatePassword}><label><span>New password</span><div className="input-wrap"><LockKeyhole size={18} /><input required minLength={8} type={showPassword ? 'text' : 'password'} autoComplete="new-password" value={password} onChange={(event) => setPassword(event.target.value)} /><button type="button" className="input-action" onClick={() => setShowPassword(!showPassword)}>{showPassword ? <EyeOff size={18} /> : <Eye size={18} />}</button></div></label><label><span>Confirm new password</span><div className="input-wrap"><LockKeyhole size={18} /><input required minLength={8} type={showPassword ? 'text' : 'password'} autoComplete="new-password" value={confirmPassword} onChange={(event) => setConfirmPassword(event.target.value)} /></div></label><Button type="submit" className="full auth-submit" loading={loading}>Save new password <ArrowRight size={17} /></Button></form>}
      {step === 'done' && <Button className="full auth-submit" onClick={() => navigate('/login', { replace: true })}>Continue to sign in <ArrowRight size={17} /></Button>}
      {step !== 'done' && <p className="auth-switch"><Link to="/login"><ArrowLeft size={14} /> Return to login</Link></p>}
    </div></div>
  </div>
}
