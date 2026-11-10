export type PhoneVisibility = 'hidden' | 'masked' | 'visible'

export interface Profile {
  id: string
  full_name: string
  phone: string
  emergency_contact_name: string
  emergency_contact_phone: string
  phone_visibility: PhoneVisibility
  social_links: Record<string, string>
  push_enabled: boolean
  created_at?: string
  updated_at?: string
}

export interface Vehicle {
  id: string
  owner_id: string
  public_token: string
  registration_number: string
  make: string
  model: string
  color: string
  year: number | null
  vehicle_type: string
  nickname: string
  card_theme: string
  card_layout: 'balanced' | 'qr_focus' | 'minimal'
  card_fulfillment: 'undecided' | 'self_print' | 'hich_print'
  tracking_enabled: boolean
  is_active: boolean
  created_at: string
}

export interface PrintOrder {
  id: string
  owner_id: string
  vehicle_id: string
  order_code: string
  unit_price_lkr: number
  total_lkr: number
  payment_status: 'pending' | 'paid' | 'failed' | 'refunded' | 'cash_on_delivery'
  payment_method: 'cash_on_delivery' | 'bank_transfer' | 'online_gateway'
  status: 'pending_payment' | 'confirmed' | 'printing' | 'quality_check' | 'dispatched' | 'delivered' | 'cancelled'
  recipient_name: string
  contact_phone: string
  city: string
  district: string
  estimated_delivery_from: string
  estimated_delivery_to: string
  created_at: string
  vehicles?: Pick<Vehicle, 'registration_number' | 'nickname'>
}

export interface TrackerDevice {
  id: string
  owner_id: string
  vehicle_id: string
  installation_id: string
  name: string
  mode: 'mobile' | 'vehicle_head_unit'
  platform: string
  app_version: string
  is_tracking: boolean
  location_permission: string
  background_permission: boolean
  notification_permission: boolean
  battery_optimization_ignored: boolean
  location_services_enabled: boolean
  network_connected: boolean
  last_latitude: number | null
  last_longitude: number | null
  last_speed_mps: number
  last_bearing: number | null
  last_accuracy_m: number | null
  last_battery_percent: number | null
  last_seen_at: string | null
  display_mode: 'journey' | 'speed' | 'clock' | 'map'
  vehicles?: Pick<Vehicle, 'registration_number' | 'nickname' | 'make' | 'model'>
}

export interface TrackingSession {
  id: string
  owner_id: string
  vehicle_id: string
  tracker_device_id: string
  status: 'active' | 'completed' | 'stale' | 'cancelled'
  started_at: string
  ended_at: string | null
  start_reason: string
  end_reason: string | null
  duration_seconds: number
  distance_m: number
  max_speed_mps: number
  average_speed_mps: number
  point_count: number
  start_latitude: number | null
  start_longitude: number | null
  end_latitude: number | null
  end_longitude: number | null
  vehicles?: Pick<Vehicle, 'registration_number' | 'nickname'>
}

export interface LocationPoint {
  id: number
  session_id: string
  sequence_number: number
  recorded_at: string
  latitude: number
  longitude: number
  speed_mps: number
  bearing: number | null
  accuracy_m: number | null
}

export interface IncidentAlert {
  id: string
  vehicle_id: string
  owner_id: string
  alert_type: string
  message: string
  reporter_contact: string | null
  latitude: number | null
  longitude: number | null
  status: 'new' | 'seen' | 'resolved'
  created_at: string
  vehicles?: Pick<Vehicle, 'registration_number' | 'nickname'>
}

export interface PublicVehicleProfile {
  vehicle_id: string
  public_token: string
  registration_number: string
  make: string
  model: string
  color: string
  vehicle_type: string
  owner_name: string
  phone_display: string | null
  phone_href: string | null
  social_links: Record<string, string>
}
