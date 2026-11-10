import { Link } from 'react-router-dom'

export function Brand({ compact = false }: { compact?: boolean }) {
  return (
    <Link className="brand" to="/" aria-label="RoadPing home">
      <span className="brand-word" aria-hidden="true">ROAD<span>PING</span><i>✳</i></span>
      {!compact && (
        <span className="brand-copy">
          <strong>VEHICLE<br />INTELLIGENCE</strong>
          <small>BY HICH</small>
        </span>
      )}
    </Link>
  )
}
