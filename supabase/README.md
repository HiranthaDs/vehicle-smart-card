# RoadPing Supabase setup

1. Rotate the exposed secret/service key before doing anything else. Never put a service key in the website or Android app.
2. Link the Supabase CLI to project `avixsqfvfonlioslhxif` and run `supabase db push` to apply both SQL migrations in order.
3. Apply `config.toml` to the hosted project with `supabase config diff --remote=avixsqfvfonlioslhxif`, review the Auth change, then run `supabase config push --remote=avixsqfvfonlioslhxif --yes`. This disables **Confirm email**, so signup creates a session immediately and sends no registration email or OTP. The equivalent Dashboard setting is Authentication → Sign In / Providers → Email → Confirm email.
4. In Authentication → Email Templates → Reset Password, send the six-digit token using `{{ .Token }}`. Password recovery is the only account flow that asks for an OTP. Configure production SMTP before launch.
5. In Authentication → URL Configuration, add the production RoadPing URL and localhost development URLs to the allow-list.
6. Generate a VAPID key pair, add the public key to `web/.env.local`, and set the incident-push function secrets from its `.env.example`.
7. Deploy push with `supabase functions deploy send-incident-push`, then add a Database Webhook for `public.incident_alerts` / `INSERT` with the `x-roadping-webhook-secret` header.
8. Set `TRACKING_MAINTENANCE_SECRET`, deploy `tracking-maintenance` with `--no-verify-jwt`, and schedule it once per minute using Supabase Cron/pg_net. Send the secret as `x-roadping-maintenance-secret`. This closes trips cleanly when a car player loses power instead of sending a final stop event.

The SQL exposes only two anonymous RPCs: one returns a privacy-filtered vehicle profile and one creates a rate-limited alert. Raw contacts, vehicles, print orders, tracker devices, trips and GPS points stay behind Row Level Security. The stale-session closer is executable only by the service role.
