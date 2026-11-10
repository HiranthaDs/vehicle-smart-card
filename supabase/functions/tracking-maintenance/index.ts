const headers = { 'content-type': 'application/json' }

Deno.serve(async (request) => {
  const expected = Deno.env.get('TRACKING_MAINTENANCE_SECRET') || ''
  if (!expected || request.headers.get('x-roadping-maintenance-secret') !== expected) {
    return new Response(JSON.stringify({ error: 'Unauthorized' }), { status: 401, headers })
  }

  const baseUrl = Deno.env.get('SUPABASE_URL') || ''
  const serviceKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || ''
  if (!baseUrl || !serviceKey) return new Response(JSON.stringify({ error: 'Server configuration is incomplete' }), { status: 500, headers })

  const response = await fetch(`${baseUrl}/rest/v1/rpc/close_stale_tracking_sessions`, {
    method: 'POST',
    headers: { apikey: serviceKey, authorization: `Bearer ${serviceKey}`, 'content-type': 'application/json' },
    body: JSON.stringify({ p_stale_after: '5 minutes' }),
  })
  const body = await response.text()
  return new Response(body || JSON.stringify({ closed: 0 }), { status: response.status, headers })
})
