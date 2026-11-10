import { forwardRef, useEffect, useState } from 'react'
import QRCode from 'qrcode'
import { ArrowUpRight, ShieldCheck } from 'lucide-react'
import type { Vehicle } from '../types'
import { vehiclePublicUrl } from '../lib/supabase'
import '../card-design.css'

interface VehicleCardProps {
  vehicle: Pick<
    Vehicle,
    'public_token' | 'registration_number' | 'make' | 'model' | 'color' | 'card_theme' | 'nickname'
  > & Partial<Pick<Vehicle, 'is_active'>>
  ownerName?: string
  demo?: boolean
}

export const VehicleCard = forwardRef<HTMLDivElement, VehicleCardProps>(function VehicleCard(
  { vehicle, ownerName = 'Protected owner', demo = false },
  ref,
) {
  const [qr, setQr] = useState('')
  const [qrError, setQrError] = useState(false)

  useEffect(() => {
    let cancelled = false
    setQr('')
    setQrError(false)
    QRCode.toDataURL(vehiclePublicUrl(vehicle.public_token), {
      width: 720,
      margin: 4,
      color: { dark: '#080808', light: '#ffffff' },
      errorCorrectionLevel: 'H',
    }).then((value) => { if (!cancelled) setQr(value) })
      .catch(() => { if (!cancelled) setQrError(true) })
    return () => { cancelled = true }
  }, [vehicle.public_token])

  return (
    <div ref={ref} className={`vehicle-card roadping-card theme-${vehicle.card_theme || 'midnight'}`}>
      <div className="rp-card-top">
        <span className="rp-card-brand">ROADPING<span aria-hidden="true">✳</span></span>
        <span className={`rp-card-status${vehicle.is_active === false ? ' paused' : ''}`}><i />{demo ? 'SAMPLE CARD' : vehicle.is_active === false ? 'PAUSED' : 'SMART VEHICLE ID'}</span>
      </div>
      <div className="rp-card-main">
        <div className="rp-card-identity">
          <span className="rp-card-label">ONE VEHICLE. ONE CONNECTION.</span>
          <strong className="rp-card-registration">{vehicle.registration_number || 'WP CAB-1234'}</strong>
          <p>{[vehicle.make, vehicle.model].filter(Boolean).join(' ') || 'Your vehicle'}{vehicle.color && <span>{vehicle.color}</span>}</p>
        </div>
        <div className="rp-card-qr-block">
          <div className="rp-card-qr">
            {qr ? <img src={qr} alt={`Scan to contact the owner of ${vehicle.registration_number}`} /> : qrError ? <span className="rp-card-qr-error">QR unavailable</span> : <span className="qr-loader" role="status" aria-label="Generating QR code" />}
          </div>
          <span>SCAN TO CONNECT <ArrowUpRight size={10} /></span>
        </div>
      </div>
      <div className="rp-card-owner">
        <span><ShieldCheck size={14} /><span>{ownerName}</span></span>
        <span>{vehicle.nickname || 'MY VEHICLE'}</span>
      </div>
      <div className="rp-card-bottom"><span>A LITTLE PEACE OF MIND.</span><span>NO APP NEEDED <ArrowUpRight size={12} /></span></div>
    </div>
  )
})
