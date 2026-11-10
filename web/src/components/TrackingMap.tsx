import { useEffect, useRef } from 'react'
import L from 'leaflet'
import type { LocationPoint } from '../types'

interface TrackingMapProps {
  latitude?: number | null
  longitude?: number | null
  bearing?: number | null
  points?: LocationPoint[]
}

export function TrackingMap({ latitude, longitude, bearing = 0, points = [] }: TrackingMapProps) {
  const host = useRef<HTMLDivElement>(null)
  const mapRef = useRef<L.Map | null>(null)
  const markerRef = useRef<L.Marker | null>(null)
  const lineRef = useRef<L.Polyline | null>(null)

  useEffect(() => {
    if (!host.current || mapRef.current) return
    const map = L.map(host.current, { zoomControl: true, attributionControl: true }).setView([7.8731, 80.7718], 7)
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '© OpenStreetMap',
    }).addTo(map)
    mapRef.current = map
    return () => { map.remove(); mapRef.current = null }
  }, [])

  useEffect(() => {
    const map = mapRef.current
    if (!map) return
    const route = points.map((point) => [point.latitude, point.longitude] as L.LatLngTuple)
    if (lineRef.current) lineRef.current.remove()
    if (route.length > 1) {
      lineRef.current = L.polyline(route, { color: '#1043f9', weight: 5, opacity: .95 }).addTo(map)
      map.fitBounds(lineRef.current.getBounds(), { padding: [28, 28] })
    }
    const lat = latitude ?? points.at(-1)?.latitude
    const lon = longitude ?? points.at(-1)?.longitude
    if (lat == null || lon == null) return
    const icon = L.divIcon({ className: '', html: `<div class="vehicle-map-marker" style="transform:rotate(${bearing || 0}deg)"></div>`, iconSize: [24, 24], iconAnchor: [12, 12] })
    if (markerRef.current) markerRef.current.setLatLng([lat, lon]).setIcon(icon)
    else markerRef.current = L.marker([lat, lon], { icon }).addTo(map)
    if (route.length <= 1) map.setView([lat, lon], 16, { animate: true })
  }, [latitude, longitude, bearing, points])

  return <div ref={host} className="tracking-map" aria-label="Vehicle location map" />
}
