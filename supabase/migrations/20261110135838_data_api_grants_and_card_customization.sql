-- Keep the browser and Android clients reachable after Supabase's 2026
-- opt-in Data API grants change. RLS remains the row-level security boundary.
grant select, insert, update on table public.profiles to authenticated;
grant select, insert, update, delete on table public.vehicles to authenticated;
grant select, update on table public.incident_alerts to authenticated;
grant select, insert, update, delete on table public.push_subscriptions to authenticated;
grant select, insert, update, delete on table public.android_device_tokens to authenticated;
grant select on table public.print_orders to authenticated;
grant select, insert, update, delete on table public.tracker_devices to authenticated;
grant select on table public.tracking_sessions to authenticated;
grant select on table public.location_points to authenticated;

grant select, insert, update, delete on table
  public.profiles,
  public.vehicles,
  public.incident_alerts,
  public.push_subscriptions,
  public.android_device_tokens,
  public.print_orders,
  public.tracker_devices,
  public.tracking_sessions,
  public.location_points
to service_role;

alter table public.vehicles
  drop constraint valid_card_theme,
  add column card_layout text not null default 'balanced',
  add constraint valid_card_theme
    check (card_theme in ('midnight', 'ocean', 'ember', 'forest', 'sand', 'violet')),
  add constraint valid_card_layout
    check (card_layout in ('balanced', 'qr_focus', 'minimal'));

alter table public.tracker_devices
  add column display_mode text not null default 'journey',
  add constraint valid_tracker_display_mode
    check (display_mode in ('journey', 'speed', 'clock', 'map'));

-- Make new columns visible immediately to PostgREST clients after deployment.
notify pgrst, 'reload schema';
