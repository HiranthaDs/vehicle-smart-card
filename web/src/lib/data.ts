import { useCallback, useEffect, useState } from 'react'
import { supabase } from './supabase'
import type { IncidentAlert, PrintOrder, TrackerDevice, TrackingSession, Vehicle } from '../types'

export function useVehicles(ownerId?: string) {
  const [vehicles, setVehicles] = useState<Vehicle[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')

  const refresh = useCallback(async () => {
    if (!ownerId) return
    setLoading(true)
    const { data, error: queryError } = await supabase.from('vehicles').select('*').eq('owner_id', ownerId).order('created_at', { ascending: false })
    setVehicles((data as Vehicle[]) || [])
    setError(queryError?.message || '')
    setLoading(false)
  }, [ownerId])

  useEffect(() => { void refresh() }, [refresh])
  return { vehicles, loading, error, refresh }
}

export function useAlerts(ownerId?: string) {
  const [alerts, setAlerts] = useState<IncidentAlert[]>([])
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    if (!ownerId) return
    setLoading(true)
    const { data } = await supabase.from('incident_alerts').select('*, vehicles(registration_number,nickname)').eq('owner_id', ownerId).order('created_at', { ascending: false })
    setAlerts((data as IncidentAlert[]) || [])
    setLoading(false)
  }, [ownerId])

  useEffect(() => {
    void refresh()
    if (!ownerId) return
    const channel = supabase
      .channel(`owner-alerts-${ownerId}`)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'incident_alerts', filter: `owner_id=eq.${ownerId}` }, () => void refresh())
      .subscribe()
    return () => { void supabase.removeChannel(channel) }
  }, [ownerId, refresh])
  return { alerts, loading, refresh }
}

export function useTracking(ownerId?: string) {
  const [devices, setDevices] = useState<TrackerDevice[]>([])
  const [sessions, setSessions] = useState<TrackingSession[]>([])
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    if (!ownerId) return
    const [deviceResult, sessionResult] = await Promise.all([
      supabase.from('tracker_devices').select('*, vehicles(registration_number,nickname,make,model)').eq('owner_id', ownerId).order('last_seen_at', { ascending: false }),
      supabase.from('tracking_sessions').select('*, vehicles(registration_number,nickname)').eq('owner_id', ownerId).order('started_at', { ascending: false }).limit(100),
    ])
    setDevices((deviceResult.data as TrackerDevice[]) || [])
    setSessions((sessionResult.data as TrackingSession[]) || [])
    setLoading(false)
  }, [ownerId])

  useEffect(() => {
    void refresh()
    if (!ownerId) return
    const channel = supabase.channel(`owner-tracking-${ownerId}`)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'tracker_devices', filter: `owner_id=eq.${ownerId}` }, () => void refresh())
      .on('postgres_changes', { event: '*', schema: 'public', table: 'tracking_sessions', filter: `owner_id=eq.${ownerId}` }, () => void refresh())
      .subscribe()
    return () => { void supabase.removeChannel(channel) }
  }, [ownerId, refresh])

  return { devices, sessions, loading, refresh }
}

export function usePrintOrders(ownerId?: string) {
  const [orders, setOrders] = useState<PrintOrder[]>([])
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async () => {
    if (!ownerId) return
    const { data } = await supabase.from('print_orders').select('*, vehicles(registration_number,nickname)').eq('owner_id', ownerId).order('created_at', { ascending: false })
    setOrders((data as PrintOrder[]) || [])
    setLoading(false)
  }, [ownerId])

  useEffect(() => {
    void refresh()
    if (!ownerId) return
    const channel = supabase.channel(`owner-orders-${ownerId}`)
      .on('postgres_changes', { event: '*', schema: 'public', table: 'print_orders', filter: `owner_id=eq.${ownerId}` }, () => void refresh())
      .subscribe()
    return () => { void supabase.removeChannel(channel) }
  }, [ownerId, refresh])

  return { orders, loading, refresh }
}
