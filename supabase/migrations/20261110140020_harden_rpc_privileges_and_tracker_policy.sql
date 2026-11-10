-- Functions receive EXECUTE from PUBLIC by default. Start from a closed API
-- surface, then expose only the RPC entry points each role actually needs.
revoke execute on all functions in schema public from public, anon, authenticated, service_role;

grant execute on function public.get_public_vehicle_profile(uuid) to anon, authenticated;
grant execute on function public.submit_public_alert(uuid, text, text, text, double precision, double precision) to anon, authenticated;

grant execute on function public.create_print_order(uuid,text,text,text,text,text,text,text,text,text) to authenticated;
grant execute on function public.choose_self_print(uuid) to authenticated;
grant execute on function public.register_tracker_device(uuid,uuid,text,public.tracker_mode,text,text,boolean,boolean,boolean,boolean,boolean) to authenticated;
grant execute on function public.start_tracking_session(uuid,text) to authenticated;
grant execute on function public.ingest_location_batch(uuid,jsonb) to authenticated;
grant execute on function public.finish_tracking_session(uuid,text) to authenticated;

grant execute on function public.close_stale_tracking_sessions(interval) to service_role;

-- The ALL policy already covers SELECT with the same ownership predicate.
drop policy "owners read their tracker devices" on public.tracker_devices;

notify pgrst, 'reload schema';
