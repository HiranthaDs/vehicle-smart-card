-- Public scanners use the two privacy-filtered SECURITY DEFINER functions.
-- They do not need direct access to any owner or tracking table.
revoke all privileges on table
  public.profiles,
  public.vehicles,
  public.incident_alerts,
  public.push_subscriptions,
  public.android_device_tokens,
  public.print_orders,
  public.tracker_devices,
  public.tracking_sessions,
  public.location_points
from anon;

notify pgrst, 'reload schema';
