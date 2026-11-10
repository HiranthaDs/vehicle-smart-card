import { useEffect, useRef, useState } from 'react'
import { toPng } from 'html-to-image'
import { ArrowLeft, Check, Copy, Download, ExternalLink, LoaderCircle, Pencil, Printer, Share2, ShieldCheck } from 'lucide-react'
import { Link, useParams } from 'react-router-dom'
import { VehicleCard } from '../components/VehicleCard'
import { Button, Toast } from '../components/Ui'
import { useAuth } from '../context'
import { humanError, supabase, vehiclePublicUrl } from '../lib/supabase'
import type { Vehicle } from '../types'

export function CardPage() {
  const { id } = useParams()
  const { user, profile } = useAuth()
  const [vehicle, setVehicle] = useState<Vehicle | null>(null)
  const [loading, setLoading] = useState(true)
  const [exporting, setExporting] = useState(false)
  const [notice, setNotice] = useState<{ text: string; type: 'success' | 'error' } | null>(null)
  const [loadError, setLoadError] = useState('')
  const cardRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    if (!id || !user) return
    let cancelled = false
    setLoading(true)
    setLoadError('')
    supabase.from('vehicles').select('*').eq('id', id).eq('owner_id', user.id).single().then(({ data, error }) => {
      if (cancelled) return
      setVehicle(data as Vehicle | null)
      if (error) setLoadError(humanError(error))
      setLoading(false)
    })
    return () => { cancelled = true }
  }, [id, user])

  async function prepareCard() {
    await document.fonts.ready
    const image = cardRef.current?.querySelector<HTMLImageElement>('.rp-card-qr img')
    if (!image) throw new Error('Your QR code is still preparing. Please try again in a moment.')
    await image.decode()
  }

  async function downloadCard() {
    if (!cardRef.current || !vehicle) return
    setExporting(true)
    try {
      await prepareCard()
      const dataUrl = await toPng(cardRef.current, { pixelRatio: 3, cacheBust: true })
      const link = document.createElement('a')
      link.download = `roadping-${vehicle.registration_number.replace(/\s/g, '-').toLowerCase()}.png`
      link.href = dataUrl
      link.click()
      setNotice({ type: 'success', text: 'Your high-resolution card is ready. Check your downloads.' })
    } catch (error) { setNotice({ type: 'error', text: humanError(error) }) } finally { setExporting(false) }
  }

  async function copyLink() {
    if (!vehicle) return
    try {
      if (!navigator.clipboard) throw new Error('Copy is unavailable in this browser. Open the public scan page and copy its address.')
      await navigator.clipboard.writeText(vehiclePublicUrl(vehicle.public_token))
      setNotice({ type: 'success', text: 'Public scan link copied. It is ready to share.' })
    } catch (error) { setNotice({ type: 'error', text: humanError(error) }) }
  }

  async function shareCard() {
    if (!vehicle) return
    const share = { title: `RoadPing — ${vehicle.registration_number}`, text: 'Scan this RoadPing card to contact the vehicle owner.', url: vehiclePublicUrl(vehicle.public_token) }
    try {
      if (navigator.share) await navigator.share(share)
      else await copyLink()
    } catch (error) {
      if (error instanceof Error && error.name === 'AbortError') return
      setNotice({ type: 'error', text: humanError(error) })
    }
  }

  async function printCard() {
    try { await prepareCard(); window.print() }
    catch (error) { setNotice({ type: 'error', text: humanError(error) }) }
  }

  if (loading) return <div className="app-page card-page"><div className="card-load-state" role="status"><LoaderCircle className="spin" size={28} /><p>Preparing your smart card…</p></div></div>
  if (!vehicle) return <div className="app-page"><div className="empty-state"><h2>Could not open this card</h2><p>{loadError || 'This vehicle may no longer be in your garage.'}</p><Link className="button" to="/app/vehicles">Back to vehicles</Link></div></div>

  return (
    <div className="app-page card-page">
      <Link to="/app/vehicles" className="back-link"><ArrowLeft size={17} /> Back to vehicles</Link>
      {notice && <Toast type={notice.type} onClose={() => setNotice(null)}>{notice.text}</Toast>}
      <div className="card-page-grid">
        <section className="card-stage">
          <span className="eyebrow">YOUR VEHICLE. CONNECTED.</span>
          <h1>SMALL CARD.<br />BIG PEACE OF MIND.</h1>
          <p>Your vehicle's own connection to the people around it. Save it, print it, and you're ready for the road.</p>
          <div className="card-preview-surface"><div className="export-card-wrap"><VehicleCard ref={cardRef} vehicle={vehicle} ownerName={profile?.full_name || 'Protected owner'} /></div><span className="card-preview-caption"><ShieldCheck size={14} /> A direct connection. No app needed to scan.</span></div>
          <div className="card-toolbar"><Button onClick={downloadCard} loading={exporting}><Download size={17} /> Save card</Button><Button className="secondary" onClick={printCard}><Printer size={17} /> Print</Button><Button className="ghost" onClick={shareCard}><Share2 size={17} /> Share</Button></div>
          <Link className="card-edit-link" to={`/app/vehicles/${vehicle.id}/edit`}><Pencil size={14} /> Edit details & card color</Link>
        </section>
        <aside className="card-side-panel panel">
          <div className="card-active-heading"><span className="success-seal"><Check /></span><div><h2>{vehicle.is_active ? 'Ready for the road' : 'Your card is paused'}</h2><p>{vehicle.registration_number}</p></div></div><p>{vehicle.is_active ? 'One scan opens your vehicle’s contact page, so someone can send you a helpful alert.' : 'Turn your vehicle back on in its settings to receive alerts from scans.'}</p>
          <div className="link-box"><small>PUBLIC SCAN LINK</small><span>{vehiclePublicUrl(vehicle.public_token)}</span><button onClick={copyLink} aria-label="Copy public link"><Copy /></button></div>
          <Link className="button full secondary" to={`/v/${vehicle.public_token}`} target="_blank" rel="noopener noreferrer">Preview a scan <ExternalLink size={17} /></Link>
          <div className="hich-print-callout"><small>CARD FULFILMENT</small><h3>Want Hich to make it?</h3><p>Professional print, QR quality check and careful delivery within 2-3 working business days.</p><strong>LKR 1,990 <span>ONE TIME</span></strong><Link className="button full" to={`/app/print/${vehicle.id}`}>Choose print option <Printer size={16} /></Link></div>
          <div className="print-tips"><h3>Placement tips</h3><ul><li>Print at 100% size on a matte card.</li><li>Keep the QR at least 30 mm wide.</li><li>Place inside a windscreen or on a weatherproof tag.</li><li>Test it with another phone before use.</li></ul></div>
        </aside>
      </div>
    </div>
  )
}
