import { createClient } from '@supabase/supabase-js'

const supabaseUrl = import.meta.env.VITE_SUPABASE_URL as string | undefined
const supabaseKey = import.meta.env.VITE_SUPABASE_PUBLISHABLE_KEY as string | undefined

export const isSupabaseConfigured = Boolean(supabaseUrl && supabaseKey)

export const supabase = createClient(
  supabaseUrl || 'https://placeholder.supabase.co',
  supabaseKey || 'placeholder-key',
  {
    auth: {
      persistSession: true,
      autoRefreshToken: true,
      detectSessionInUrl: true,
      storageKey: 'roadping-session',
    },
  },
)

export const publicAppUrl = (
  (import.meta.env.VITE_PUBLIC_APP_URL as string | undefined) || window.location.origin
).replace(/\/$/, '')

export function vehiclePublicUrl(token: string) {
  return `${publicAppUrl}/#/v/${token}`
}

export function humanError(error: unknown) {
  if (error instanceof Error) return error.message
  if (typeof error === 'object' && error && 'message' in error) return String(error.message)
  return 'Something went wrong. Please try again.'
}

export function formatRegistration(value: string) {
  return value.toUpperCase().replace(/[^A-Z0-9-]/g, '').replace(/-+/g, '-')
}

export function maskPhone(phone: string) {
  if (!phone) return 'Protected'
  const compact = phone.replace(/\s/g, '')
  if (compact.length < 6) return '••••••'
  return `${compact.slice(0, 3)} ••• •${compact.slice(-2)}`
}
