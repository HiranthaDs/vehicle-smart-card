import { useEffect, useRef, useState, type MouseEvent } from 'react'
import {
  ArrowUpRight,
  Check,
  Download,
  EyeOff,
  LocateFixed,
  Menu,
  PackageCheck,
  Printer,
  QrCode,
  Radio,
  Route,
  ShieldCheck,
  Smartphone,
  X,
} from 'lucide-react'
import { Link } from 'react-router-dom'
import { Brand } from '../components/Brand'
import { VehicleCard } from '../components/VehicleCard'

const demoVehicle = {
  public_token: 'demo-roadping',
  registration_number: 'WP CAB-2048',
  make: 'Toyota',
  model: 'Corolla',
  color: 'Pearl white',
  card_theme: 'midnight',
  nickname: 'Daily ride',
}

export function HomePage() {
  const [menuOpen, setMenuOpen] = useState(false)
  const [installEvent, setInstallEvent] = useState<Event | null>(null)
  const menuRef = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    if (!menuOpen) return
    const dialog = menuRef.current
    const previousOverflow = document.body.style.overflow
    dialog?.showModal()
    document.body.style.overflow = 'hidden'
    return () => { dialog?.close(); document.body.style.overflow = previousOverflow }
  }, [menuOpen])

  function goToSection(event: MouseEvent<HTMLAnchorElement>, id: string) {
    event.preventDefault()
    setMenuOpen(false)
    document.getElementById(id)?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth' })
  }

  useEffect(() => {
    const onInstall = (event: Event) => {
      event.preventDefault()
      setInstallEvent(event)
    }
    window.addEventListener('beforeinstallprompt', onInstall)
    return () => window.removeEventListener('beforeinstallprompt', onInstall)
  }, [])

  async function installPwa() {
    if (!installEvent) return
    await (installEvent as Event & { prompt: () => Promise<void> }).prompt()
    setInstallEvent(null)
  }

  return (
    <div className="marketing-site hich-roadping">
      <header className="hich-header">
        <Brand />
        <div className="hich-header-center"><i /> SRI LANKA · EVERY ROAD</div>
        <div className="hich-header-actions">
          <Link className="hich-pill" to="/login">Owner login <ArrowUpRight /></Link>
          <button className="hich-menu-trigger" aria-expanded={menuOpen} aria-controls="site-menu" onClick={() => setMenuOpen(!menuOpen)}>MENU <Menu /></button>
        </div>
      </header>

      <dialog ref={menuRef} id="site-menu" className="hich-menu-sheet" aria-label="Main menu" onCancel={() => setMenuOpen(false)}>
        <div className="menu-sheet-top"><Brand /><button className="hich-menu-trigger" onClick={() => setMenuOpen(false)}>CLOSE <X /></button></div>
        <nav>
          <a href="#how" onClick={(e) => goToSection(e, 'how')}><span>01</span>How it works <ArrowUpRight /></a>
          <a href="#tracking" onClick={(e) => goToSection(e, 'tracking')}><span>02</span>Live tracking <ArrowUpRight /></a>
          <a href="#print" onClick={(e) => goToSection(e, 'print')}><span>03</span>Card printing <ArrowUpRight /></a>
          <a href="#app" onClick={(e) => goToSection(e, 'app')}><span>04</span>Android app <ArrowUpRight /></a>
          <Link to="/login"><span>05</span>Owner login <ArrowUpRight /></Link>
        </nav>
        <aside><p>ONE VEHICLE ID.<br />A SAFER WAY TO STAY CONNECTED.</p><Link to="/signup">Create your account <ArrowUpRight /></Link><small>A HICH WEB PRODUCT · BUILT IN SRI LANKA</small></aside>
      </dialog>

      <main>
        <section className="hich-hero">
          <div className="hich-hero-topline"><p>A little card. A closer connection.<br />Meet a smarter way to care for your vehicle.</p><span>DESIGN + TECHNOLOGY<br />ROADS MADE A LITTLE SMARTER.</span></div>
          <div className="hich-hero-copy">
            <h1>YOUR VEHICLE.<br />A LITTLE<br /><span>SMARTER.<i className="hero-spark" aria-hidden="true">✳</i></span></h1>
            <div className="hich-hero-bottom"><p>Less wondering. More peace of mind.<br /><strong>One connection, wherever the road takes you.</strong></p><Link to="/signup" className="hich-pill hero-primary">Create your smart card <ArrowUpRight /></Link></div>
          </div>
        </section>
        <section className="hich-product-showcase" aria-label="Your RoadPing smart card">
          <div className="hich-hero-intro"><span className="hich-label">SMALL CARD. BIG POSSIBILITIES.</span><h2>A BETTER WAY<br />TO BE <em>REACHED.</em></h2><p>A private QR identity, live location and your journey history. Everything that matters about your vehicle, together in one place.</p><Link to="/v/demo-roadping" className="hich-demo-link">Try a scan. See the difference. <ArrowUpRight /></Link><div className="showcase-trust"><ShieldCheck size={16} /> Your phone number stays private.</div></div>
          <div className="hich-hero-stage">
            <div className="hich-orbit" aria-hidden="true"><i /><i /><i /></div>
            <span className="stage-edition">ROADPING / SMART ID — 01</span>
            <div className="hich-card-float"><VehicleCard vehicle={demoVehicle} ownerName="Nimal P." demo /></div>
            <div className="hich-location-chip"><ShieldCheck /><span><strong>LOOKING OUT FOR YOU.</strong><small>ONE SCAN. A PRIVATE CONNECTION.</small></span></div>
            <div className="hich-stage-foot"><span>QR CONTACT</span><i /><span>LIVE LOCATION</span><i /><span>TRIP HISTORY</span></div>
          </div>
        </section>

        <section className="hich-marquee"><div>SCAN <i>✳</i> ALERT <i>✳</i> LOCATE <i>✳</i> REMEMBER <i>✳</i> SCAN <i>✳</i> ALERT <i>✳</i> LOCATE</div></section>

        <section id="how" className="hich-section hich-intro-section">
          <div className="hich-section-head"><p><span>01</span> A DIFFERENT KIND OF VEHICLE CARD</p><h2>USEFUL ON<br /><em>EVERY ROAD.</em></h2></div>
          <div className="hich-intro-grid">
            <div className="hich-copy"><h3>Beautiful on the surface.<br />Thoughtful underneath.</h3><p>RoadPing connects the moments that normally get lost: a stranger trying to warn you, a vehicle player reporting its position, or a journey you need to remember later.</p><Link to="/v/demo-roadping">Try the public scan <ArrowUpRight /></Link></div>
            <div className="hich-feature-stack">
              <article><span>01</span><QrCode /><div><h3>Private QR contact</h3><p>A scanner can reach the right owner without seeing a protected phone number.</p></div><ArrowUpRight /></article>
              <article><span>02</span><Radio /><div><h3>Realtime vehicle presence</h3><p>See the car player online, current speed, battery, network and last reliable position.</p></div><ArrowUpRight /></article>
              <article><span>03</span><Route /><div><h3>Every trip, remembered</h3><p>Start, stop, route, distance, duration and speed become a useful owner history.</p></div><ArrowUpRight /></article>
            </div>
          </div>
        </section>

        <section id="tracking" className="hich-section hich-tracking-section">
          <div className="hich-section-head light"><p><span>02</span> ONE APP. TWO PURPOSES.</p><h2>THE CAR REPORTS.<br /><em>YOU STAY INFORMED.</em></h2></div>
          <div className="hich-tracking-grid">
            <div className="hich-device-card car-mode"><div className="device-card-top"><span>VEHICLE PLAYER</span><Radio /></div><div className="device-screen"><div className="device-logo">ROADPING<i>✳</i></div><p>SETUP COMPLETE</p><h3>TRACKING<br />THE JOURNEY.</h3><div className="permission-list"><span><Check /> Precise location</span><span><Check /> Background service</span><span><Check /> Network sync</span><span><Check /> Offline queue</span></div><button>TRACKING ACTIVE <i /></button></div></div>
            <div className="hich-route-board">
              <div className="route-board-top"><span>WP CAB-2048</span><span><i /> LIVE · 4s</span></div>
              <div className="route-map-art"><svg viewBox="0 0 600 430" fill="none"><path d="M-30 345C68 291 110 359 181 282C240 218 260 279 329 192C395 110 458 165 632 21" /><path d="M-20 378C83 327 142 391 211 317C278 245 283 307 361 217C431 137 494 199 639 57" /><circle cx="329" cy="192" r="12" /><circle cx="329" cy="192" r="28" /></svg><span className="map-start">START<br /><b>08:42</b></span><span className="map-now">NOW<br /><b>09:16</b></span></div>
              <div className="route-metrics"><div><small>SPEED</small><strong>42</strong><span>KM/H</span></div><div><small>DISTANCE</small><strong>18.6</strong><span>KM</span></div><div><small>DURATION</small><strong>34</strong><span>MIN</span></div></div>
            </div>
            <div className="hich-tracking-copy"><p className="hich-label">BUILT FOR ANDROID HEAD UNITS</p><h3>Setup once.<br />Start with the vehicle.</h3><p>Install the same Android app on an aftermarket Android car player. Choose <b>Vehicle Player Mode</b>, connect it to a vehicle and approve the guided permissions. The foreground tracking service keeps a visible system notification, batches points safely, and continues through screen-off conditions.</p><ul><li><Check /> Adaptive GPS accuracy and speed capture</li><li><Check /> Offline queue when mobile data disappears</li><li><Check /> Automatic stale-trip closure after power loss</li><li><Check /> Owner-only realtime data with RLS</li></ul><Link to="/login">Open tracking dashboard <ArrowUpRight /></Link></div>
          </div>
        </section>

        <section id="print" className="hich-section hich-print-section">
          <div className="hich-section-head"><p><span>03</span> YOUR CARD. YOUR CHOICE.</p><h2>PRINT IT YOURSELF.<br /><em>OR LEAVE IT TO HICH.</em></h2></div>
          <div className="hich-print-grid">
            <article className="self-print-card"><div className="print-number">01</div><Printer /><p>SELF PRINT</p><h3>Download it.<br />Make it yours.</h3><span>Get the high-resolution RoadPing card instantly. Print it at home or with any local print partner.</span><ul><li><Check /> Instant PNG download</li><li><Check /> Print placement guide</li><li><Check /> No fulfillment charge</li></ul><Link to="/signup">Create and download <ArrowUpRight /></Link></article>
            <article className="hich-print-card"><div className="print-number">02</div><PackageCheck /><div className="print-price"><small>ONE-TIME</small><strong>LKR 1,990</strong></div><p>PRINTED BY HICH</p><h3>Made carefully.<br />Delivered ready.</h3><span>Hich Web prints, checks, protects and delivers your finished card within 2–3 working business days.</span><ul><li><Check /> Professional high-contrast print</li><li><Check /> Quality checked before dispatch</li><li><Check /> Careful islandwide handover</li></ul><Link to="/signup">Order after signup <ArrowUpRight /></Link></article>
          </div>
        </section>

        <section className="hich-section hich-privacy-section">
          <div className="hich-privacy-word">PRIVATE<span>BY DESIGN.</span><i>✳</i></div>
          <div className="hich-privacy-copy"><p className="hich-label">THE DETAILS MAKE THE DIFFERENCE</p><h3>Your number is not the product.</h3><p>Phone visibility starts protected. Public scans use privacy-filtered functions. Tracking data belongs to the signed-in owner, and every head unit is tied to a specific vehicle and installation identity.</p><div><span><EyeOff /> PHONE HIDDEN</span><span><ShieldCheck /> OWNER-ONLY ROUTES</span><span><LocateFixed /> PRECISE CONSENT</span></div></div>
        </section>

        <section id="app" className="hich-section hich-app-section">
          <div className="hich-section-head"><p><span>04</span> BUILT FOR THE SCREEN YOU HAVE</p><h2>MOBILE. WEB.<br /><em>VEHICLE PLAYER.</em></h2></div>
          <div className="hich-app-grid">
            <div className="hich-app-phone"><div className="app-phone-header"><b>ROADPING✳</b><span>OWNER</span></div><p>GOOD MORNING</p><h3>YOUR GARAGE.</h3><div className="app-live-vehicle"><i /><div><small>LIVE VEHICLE</small><strong>WP CAB-2048</strong><span>42 km/h · Colombo 07</span></div><LocateFixed /></div><div className="app-mini-stats"><span><b>2</b>VEHICLES</span><span><b>18</b>TRIPS</span><span><b>0</b>ALERTS</span></div></div>
            <div className="hich-app-copy"><p className="hich-label">ROADPING FOR ANDROID</p><h3>One installation.<br />Choose the role.</h3><p>Owner Mode is designed for your phone. Vehicle Player Mode turns a compatible aftermarket Android head unit into a consent-led tracker for one selected vehicle.</p><div className="hich-app-actions"><a href="/downloads/roadping.apk" download><Download /> DOWNLOAD APK <ArrowUpRight /></a>{installEvent && <button onClick={installPwa}><Smartphone /> INSTALL WEB APP <ArrowUpRight /></button>}</div><small>ANDROID 8.0+ · DIRECT APK · LOCATION FOREGROUND SERVICE</small></div>
          </div>
        </section>

        <section className="hich-final-cta"><p>HAVE A VEHICLE IN MIND?</p><Link to="/signup"><span>MAKE IT<br /><em>CONNECTED.</em></span><ArrowUpRight /></Link><div><span>QR SAFETY</span><span>LIVE TRACKING</span><span>HICH CARD DELIVERY</span></div></section>
      </main>

      <footer className="hich-footer"><div className="hich-footer-brand"><Brand /><p>Vehicle intelligence.<br />Made with purpose.</p></div><div><p>WHERE TO NEXT?</p><a href="#tracking" onClick={(e) => goToSection(e, 'tracking')}>Live tracking</a><a href="#print" onClick={(e) => goToSection(e, 'print')}>Card printing</a><Link to="/login">Owner login</Link></div><div><p>BUILT BY HICH</p><a href="https://hichweb.com" target="_blank" rel="noreferrer">Hich Web <ArrowUpRight /></a><a href="tel:+94714112113">+94 714 112 113</a><span>SRI LANKA</span></div><div className="hich-footer-bottom"><span>© {new Date().getFullYear()} ROADPING / HICH WEB</span><span>THOUGHTFULLY BUILT. ENDLESSLY USEFUL.</span></div></footer>
      <nav className="marketing-dock" aria-label="Quick navigation"><a href="#how" onClick={(e) => goToSection(e, 'how')}><QrCode /><span>Discover</span></a><a href="#tracking" onClick={(e) => goToSection(e, 'tracking')}><LocateFixed /><span>Tracking</span></a><a href="#app" onClick={(e) => goToSection(e, 'app')}><Smartphone /><span>The app</span></a><Link to="/signup">Get your card <ArrowUpRight /></Link></nav>
    </div>
  )
}
