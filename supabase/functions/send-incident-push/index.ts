import { createClient } from 'npm:@supabase/supabase-js@2'
import webpush from 'npm:web-push@3.6.7'

const supabaseUrl = Deno.env.get('SUPABASE_URL')!
const serviceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!
const webhookSecret = Deno.env.get('ALERT_WEBHOOK_SECRET')!
const vapidPublicKey = Deno.env.get('VAPID_PUBLIC_KEY')!
const vapidPrivateKey = Deno.env.get('VAPID_PRIVATE_KEY')!
const publicAppUrl = Deno.env.get('PUBLIC_APP_URL') || 'https://roadping-vehicle-card.hiirantha.chatgpt.site'

webpush.setVapidDetails('mailto:alerts@roadping.app', vapidPublicKey, vapidPrivateKey)
const admin = createClient(supabaseUrl, serviceRoleKey, { auth: { persistSession: false } })

Deno.serve(async (request) => {
  if (request.method !== 'POST') return new Response('Method not allowed', { status: 405 })
  if (request.headers.get('x-roadping-webhook-secret') !== webhookSecret) return new Response('Unauthorized', { status: 401 })

  const payload = await request.json()
  const alert = payload.record
  if (!alert?.owner_id || !alert?.vehicle_id) return new Response('Invalid webhook payload', { status: 400 })

  const [{ data: vehicle }, { data: subscriptions }] = await Promise.all([
    admin.from('vehicles').select('registration_number').eq('id', alert.vehicle_id).single(),
    admin.from('push_subscriptions').select('id, endpoint, subscription').eq('owner_id', alert.owner_id),
  ])

  const notification = JSON.stringify({
    title: `RoadPing · ${vehicle?.registration_number || 'Vehicle alert'}`,
    body: `${String(alert.alert_type).replaceAll('_', ' ')}${alert.message ? ` — ${alert.message}` : ''}`,
    tag: `roadping-${alert.id}`,
    url: `${publicAppUrl}/#/app/alerts`,
  })

  const expired: string[] = []
  const results = await Promise.allSettled((subscriptions || []).map(async (row) => {
    try {
      await webpush.sendNotification(row.subscription, notification)
    } catch (error) {
      const status = (error as { statusCode?: number }).statusCode
      if (status === 404 || status === 410) expired.push(row.id)
      else throw error
    }
  }))

  if (expired.length) await admin.from('push_subscriptions').delete().in('id', expired)
  const delivered = results.filter((result) => result.status === 'fulfilled').length
  return Response.json({ delivered, failed: results.length - delivered, expired: expired.length })
})
