import { useEffect, useState, type FormEvent } from 'react'
import { ArrowLeft, ArrowRight, Check, Clock3, CreditCard, PackageCheck, Printer, ShieldCheck, Truck } from 'lucide-react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { Button, PageTitle, Skeleton, Toast } from '../components/Ui'
import { useAuth } from '../context'
import { humanError, supabase } from '../lib/supabase'
import type { PrintOrder, Vehicle } from '../types'

export function PrintOrderPage() {
  const { vehicleId } = useParams()
  const { user, profile } = useAuth()
  const navigate = useNavigate()
  const [vehicle, setVehicle] = useState<Vehicle | null>(null)
  const [order, setOrder] = useState<PrintOrder | null>(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [notice, setNotice] = useState<{ type: 'success' | 'error'; text: string } | null>(null)
  const [form, setForm] = useState({ recipientName: '', phone: '', address1: '', address2: '', city: '', district: '', postalCode: '', notes: '', paymentMethod: 'cash_on_delivery' })

  useEffect(() => {
    if (!vehicleId || !user) return
    Promise.all([
      supabase.from('vehicles').select('*').eq('id', vehicleId).eq('owner_id', user.id).single(),
      supabase.from('print_orders').select('*, vehicles(registration_number,nickname)').eq('vehicle_id', vehicleId).eq('owner_id', user.id).not('status', 'eq', 'cancelled').order('created_at', { ascending: false }).limit(1).maybeSingle(),
    ]).then(([vehicleResult, orderResult]) => {
      setVehicle(vehicleResult.data as Vehicle | null)
      setOrder(orderResult.data as PrintOrder | null)
      setForm((current) => ({ ...current, recipientName: profile?.full_name || '', phone: profile?.phone || '' }))
      setLoading(false)
    })
  }, [vehicleId, user, profile])

  function update(key: keyof typeof form, value: string) { setForm((current) => ({ ...current, [key]: value })) }

  async function chooseSelfPrint() {
    if (!vehicle) return
    setSubmitting(true)
    const { error } = await supabase.rpc('choose_self_print', { p_vehicle_id: vehicle.id })
    setSubmitting(false)
    if (error) setNotice({ type: 'error', text: error.message })
    else navigate(`/app/card/${vehicle.id}`, { replace: true })
  }

  async function placeOrder(event: FormEvent) {
    event.preventDefault()
    if (!vehicle) return
    setSubmitting(true); setNotice(null)
    try {
      const { data, error } = await supabase.rpc('create_print_order', {
        p_vehicle_id: vehicle.id,
        p_recipient_name: form.recipientName.trim(),
        p_contact_phone: form.phone.trim(),
        p_address_line_1: form.address1.trim(),
        p_address_line_2: form.address2.trim(),
        p_city: form.city.trim(),
        p_district: form.district.trim(),
        p_postal_code: form.postalCode.trim(),
        p_delivery_notes: form.notes.trim(),
        p_payment_method: form.paymentMethod,
      })
      if (error) throw error
      setOrder((Array.isArray(data) ? data[0] : data) as PrintOrder)
    } catch (error) { setNotice({ type: 'error', text: humanError(error) }) } finally { setSubmitting(false) }
  }

  if (loading) return <div className="app-page"><Skeleton className="alerts-skeleton" /></div>
  if (!vehicle) return <div className="app-page"><div className="empty-state"><h2>Vehicle not found</h2><Link className="button" to="/app/vehicles">Back to vehicles</Link></div></div>

  if (order) return <div className="app-page print-order-page"><Link className="back-link" to={`/app/card/${vehicle.id}`}><ArrowLeft size={16} /> Back to card</Link><section className="order-confirmed"><div className="order-confirmed-copy"><span className="eyebrow">ORDER RECEIVED</span><h2>HICH WILL TAKE IT FROM HERE.</h2><p>Your RoadPing card will be printed, quality checked and prepared for careful delivery.</p><span className="order-code">{order.order_code}</span></div><div className="order-confirmed-detail"><p className="eyebrow">DELIVERY WINDOW</p><h2>{new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'long' }).format(new Date(order.estimated_delivery_from))} — {new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'long' }).format(new Date(order.estimated_delivery_to))}</h2><p>Estimated within 2–3 working business days to {order.city}, {order.district}.</p><div className="order-timeline"><div><span /><section><strong>Order confirmed</strong><small>LKR 1,990 · {order.payment_method.replaceAll('_', ' ')}</small></section></div><div><span /><section><strong>Print & quality check</strong><small>Hich Web prepares the physical card.</small></section></div><div><span /><section><strong>Careful dispatch</strong><small>Status changes appear in your account.</small></section></div></div><Link className="button" to="/app">Return to dashboard <ArrowRight size={16} /></Link></div></section></div>

  return <div className="app-page print-order-page"><Link className="back-link" to={`/app/card/${vehicle.id}`}><ArrowLeft size={16} /> Back to card</Link><PageTitle eyebrow="CARD FULFILMENT" title="Choose how your card is made">Download and print it yourself, or let Hich Web print and deliver it within 2–3 working business days.</PageTitle>{notice && <Toast type={notice.type} onClose={() => setNotice(null)}>{notice.text}</Toast>}<div className="fulfillment-choice"><div className="fulfillment-choice-head"><h3>{vehicle.registration_number}</h3><span>ONE CARD · ONE VEHICLE</span></div><div className="fulfillment-options"><button className="fulfillment-option" onClick={() => void chooseSelfPrint()} disabled={submitting}><Printer /><strong>Print it myself</strong><p>Use the free high-resolution PNG and placement guide already prepared for this vehicle.</p><span>SELECT SELF PRINT <ArrowRight size={15} /></span></button><button className="fulfillment-option featured" onClick={() => document.getElementById('hich-order-form')?.scrollIntoView({ behavior: 'smooth' })}><PackageCheck /><strong>Printed by Hich · LKR 1,990</strong><p>One-time payment. Professionally printed, checked, protected and delivered carefully.</p><span>ORDER HICH PRINT <ArrowRight size={15} /></span></button></div></div><div id="hich-order-form" className="print-order-grid"><form className="print-order-form panel" onSubmit={placeOrder}><div className="settings-heading"><span><Truck /></span><div><h2>Delivery details</h2><p>Where Hich Web should hand over the finished card.</p></div></div><div className="form-grid two"><label><span>Recipient name *</span><input required value={form.recipientName} onChange={(e) => update('recipientName', e.target.value)} /></label><label><span>Contact number *</span><input required type="tel" value={form.phone} onChange={(e) => update('phone', e.target.value)} /></label><label className="wide"><span>Address line 1 *</span><input required value={form.address1} onChange={(e) => update('address1', e.target.value)} placeholder="House number and street" /></label><label className="wide"><span>Address line 2</span><input value={form.address2} onChange={(e) => update('address2', e.target.value)} placeholder="Apartment, landmark or area" /></label><label><span>City *</span><input required value={form.city} onChange={(e) => update('city', e.target.value)} /></label><label><span>District *</span><input required value={form.district} onChange={(e) => update('district', e.target.value)} /></label><label><span>Postal code</span><input value={form.postalCode} onChange={(e) => update('postalCode', e.target.value)} /></label><label><span>Payment method</span><select value={form.paymentMethod} onChange={(e) => update('paymentMethod', e.target.value)}><option value="cash_on_delivery">Cash on delivery</option><option value="bank_transfer">Bank transfer</option></select></label><label className="wide"><span>Delivery notes</span><textarea value={form.notes} onChange={(e) => update('notes', e.target.value)} placeholder="Useful directions or a preferred handover note" /></label></div></form><aside className="order-summary panel"><span className="eyebrow">ORDER SUMMARY</span><h2>RoadPing smart card</h2><p>{vehicle.registration_number} · {vehicle.color} {vehicle.make} {vehicle.model}</p><div className="order-summary-price"><span>ONE-TIME TOTAL</span><strong>LKR 1,990</strong></div><ul><li><Check /> Professional high-contrast print</li><li><ShieldCheck /> QR tested before dispatch</li><li><Clock3 /> Delivery in 2–3 working business days</li><li><Truck /> Careful islandwide handover</li><li><CreditCard /> No recurring card fee</li></ul><Button className="full" loading={submitting} onClick={() => document.querySelector<HTMLFormElement>('.print-order-form')?.requestSubmit()}>Confirm print order <ArrowRight size={16} /></Button><small>For bank transfer, Hich Web will confirm payment instructions before printing. Cash on delivery is paid once at handover.</small></aside></div></div>
}
