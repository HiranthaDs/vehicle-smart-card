-- RoadPing print fulfillment and vehicle tracking
-- Depends on 202611100001_initial_roadping.sql

alter table public.vehicles
  add column card_fulfillment text not null default 'undecided',
  add column tracking_enabled boolean not null default false,
  add constraint vehicles_card_fulfillment_check
    check (card_fulfillment in ('undecided', 'self_print', 'hich_print'));

create type public.print_order_status as enum (
  'pending_payment', 'confirmed', 'printing', 'quality_check',
  'dispatched', 'delivered', 'cancelled'
);
create type public.payment_status as enum ('pending', 'paid', 'failed', 'refunded', 'cash_on_delivery');
create type public.tracker_mode as enum ('mobile', 'vehicle_head_unit');
create type public.tracking_session_status as enum ('active', 'completed', 'stale', 'cancelled');

create table public.print_orders (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.profiles(id) on delete cascade,
  vehicle_id uuid not null references public.vehicles(id) on delete restrict,
  order_code text not null unique default ('RP-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 10))),
  quantity integer not null default 1 check (quantity = 1),
  unit_price_lkr integer not null default 1990 check (unit_price_lkr = 1990),
  total_lkr integer generated always as (quantity * unit_price_lkr) stored,
  payment_status public.payment_status not null default 'pending',
  payment_method text not null default 'cash_on_delivery'
    check (payment_method in ('cash_on_delivery', 'bank_transfer', 'online_gateway')),
  payment_reference text,
  status public.print_order_status not null default 'pending_payment',
  recipient_name text not null,
  contact_phone text not null,
  address_line_1 text not null,
  address_line_2 text not null default '',
  city text not null,
  district text not null,
  postal_code text not null default '',
  delivery_notes text not null default '',
  estimated_delivery_from date not null,
  estimated_delivery_to date not null,
  paid_at timestamptz,
  dispatched_at timestamptz,
  delivered_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create index print_orders_owner_created_idx on public.print_orders(owner_id, created_at desc);
create index print_orders_vehicle_idx on public.print_orders(vehicle_id);
create index print_orders_status_idx on public.print_orders(status, created_at);

create table public.tracker_devices (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.profiles(id) on delete cascade,
  vehicle_id uuid not null references public.vehicles(id) on delete cascade,
  installation_id uuid not null,
  name text not null default 'Vehicle player',
  mode public.tracker_mode not null default 'vehicle_head_unit',
  platform text not null default 'android',
  app_version text not null default '',
  is_primary boolean not null default true,
  is_tracking boolean not null default false,
  location_permission text not null default 'unknown',
  background_permission boolean not null default false,
  notification_permission boolean not null default false,
  battery_optimization_ignored boolean not null default false,
  location_services_enabled boolean not null default false,
  network_connected boolean not null default false,
  last_latitude double precision,
  last_longitude double precision,
  last_speed_mps double precision not null default 0,
  last_bearing double precision,
  last_accuracy_m double precision,
  last_battery_percent integer,
  last_seen_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  unique(owner_id, installation_id),
  constraint tracker_latitude_check check (last_latitude is null or last_latitude between -90 and 90),
  constraint tracker_longitude_check check (last_longitude is null or last_longitude between -180 and 180),
  constraint tracker_battery_check check (last_battery_percent is null or last_battery_percent between 0 and 100)
);

create index tracker_devices_owner_idx on public.tracker_devices(owner_id);
create index tracker_devices_vehicle_idx on public.tracker_devices(vehicle_id);
create index tracker_devices_live_idx on public.tracker_devices(owner_id, last_seen_at desc);

create table public.tracking_sessions (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.profiles(id) on delete cascade,
  vehicle_id uuid not null references public.vehicles(id) on delete cascade,
  tracker_device_id uuid not null references public.tracker_devices(id) on delete cascade,
  status public.tracking_session_status not null default 'active',
  started_at timestamptz not null default now(),
  ended_at timestamptz,
  start_reason text not null default 'manual',
  end_reason text,
  start_latitude double precision,
  start_longitude double precision,
  end_latitude double precision,
  end_longitude double precision,
  duration_seconds integer not null default 0,
  distance_m double precision not null default 0,
  max_speed_mps double precision not null default 0,
  average_speed_mps double precision not null default 0,
  point_count integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create unique index one_active_session_per_tracker
on public.tracking_sessions(tracker_device_id)
where status = 'active';
create index tracking_sessions_owner_started_idx on public.tracking_sessions(owner_id, started_at desc);
create index tracking_sessions_vehicle_started_idx on public.tracking_sessions(vehicle_id, started_at desc);

create table public.location_points (
  id bigint generated always as identity primary key,
  session_id uuid not null references public.tracking_sessions(id) on delete cascade,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  vehicle_id uuid not null references public.vehicles(id) on delete cascade,
  tracker_device_id uuid not null references public.tracker_devices(id) on delete cascade,
  sequence_number bigint not null,
  recorded_at timestamptz not null,
  received_at timestamptz not null default now(),
  latitude double precision not null check (latitude between -90 and 90),
  longitude double precision not null check (longitude between -180 and 180),
  speed_mps double precision not null default 0 check (speed_mps between 0 and 120),
  bearing double precision check (bearing is null or bearing between 0 and 360),
  altitude_m double precision,
  accuracy_m double precision check (accuracy_m is null or accuracy_m between 0 and 5000),
  battery_percent integer check (battery_percent is null or battery_percent between 0 and 100),
  network_type text not null default '',
  is_mock boolean not null default false,
  unique(session_id, sequence_number)
);

create index location_points_session_time_idx on public.location_points(session_id, recorded_at);
create index location_points_vehicle_time_idx on public.location_points(vehicle_id, recorded_at desc);
create index location_points_owner_received_idx on public.location_points(owner_id, received_at desc);

create trigger print_orders_updated_at before update on public.print_orders
for each row execute function public.set_updated_at();
create trigger tracker_devices_updated_at before update on public.tracker_devices
for each row execute function public.set_updated_at();
create trigger tracking_sessions_updated_at before update on public.tracking_sessions
for each row execute function public.set_updated_at();

alter table public.print_orders enable row level security;
alter table public.tracker_devices enable row level security;
alter table public.tracking_sessions enable row level security;
alter table public.location_points enable row level security;

create policy "owners read their print orders"
on public.print_orders for select to authenticated
using ((select auth.uid()) = owner_id);

create policy "owners read their tracker devices"
on public.tracker_devices for select to authenticated
using ((select auth.uid()) = owner_id);

create policy "owners manage their tracker devices"
on public.tracker_devices for all to authenticated
using ((select auth.uid()) = owner_id)
with check (
  (select auth.uid()) = owner_id
  and exists (
    select 1 from public.vehicles v
    where v.id = vehicle_id and v.owner_id = (select auth.uid())
  )
);

create policy "owners read their tracking sessions"
on public.tracking_sessions for select to authenticated
using ((select auth.uid()) = owner_id);

create policy "owners read their location points"
on public.location_points for select to authenticated
using ((select auth.uid()) = owner_id);

create or replace function public.add_business_days(p_start date, p_days integer)
returns date
language plpgsql
immutable
set search_path = ''
as $$
declare
  result_date date := p_start;
  added integer := 0;
begin
  while added < p_days loop
    result_date := result_date + 1;
    if extract(isodow from result_date) < 6 then
      added := added + 1;
    end if;
  end loop;
  return result_date;
end;
$$;

create or replace function public.create_print_order(
  p_vehicle_id uuid,
  p_recipient_name text,
  p_contact_phone text,
  p_address_line_1 text,
  p_address_line_2 text,
  p_city text,
  p_district text,
  p_postal_code text,
  p_delivery_notes text,
  p_payment_method text default 'cash_on_delivery'
)
returns public.print_orders
language plpgsql
security definer
set search_path = ''
as $$
declare
  result public.print_orders;
  current_owner uuid := auth.uid();
begin
  if current_owner is null then raise exception 'Authentication required'; end if;
  if not exists (select 1 from public.vehicles where id = p_vehicle_id and owner_id = current_owner) then
    raise exception 'Vehicle not found';
  end if;
  if p_payment_method not in ('cash_on_delivery', 'bank_transfer', 'online_gateway') then
    raise exception 'Unsupported payment method';
  end if;
  if length(trim(p_recipient_name)) < 2 or length(trim(p_contact_phone)) < 7
    or length(trim(p_address_line_1)) < 5 or length(trim(p_city)) < 2 or length(trim(p_district)) < 2 then
    raise exception 'Complete recipient and delivery details are required';
  end if;

  insert into public.print_orders (
    owner_id, vehicle_id, payment_method, payment_status, status,
    recipient_name, contact_phone, address_line_1, address_line_2,
    city, district, postal_code, delivery_notes,
    estimated_delivery_from, estimated_delivery_to
  ) values (
    current_owner, p_vehicle_id, p_payment_method,
    case when p_payment_method = 'cash_on_delivery' then 'cash_on_delivery'::public.payment_status else 'pending'::public.payment_status end,
    case when p_payment_method = 'cash_on_delivery' then 'confirmed'::public.print_order_status else 'pending_payment'::public.print_order_status end,
    trim(p_recipient_name), trim(p_contact_phone), trim(p_address_line_1), trim(coalesce(p_address_line_2, '')),
    trim(p_city), trim(p_district), trim(coalesce(p_postal_code, '')), trim(coalesce(p_delivery_notes, '')),
    public.add_business_days(current_date, 2), public.add_business_days(current_date, 3)
  ) returning * into result;

  update public.vehicles set card_fulfillment = 'hich_print' where id = p_vehicle_id;
  return result;
end;
$$;

create or replace function public.choose_self_print(p_vehicle_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  update public.vehicles
  set card_fulfillment = 'self_print'
  where id = p_vehicle_id and owner_id = auth.uid();
  if not found then raise exception 'Vehicle not found'; end if;
end;
$$;

create or replace function public.haversine_metres(lat1 double precision, lon1 double precision, lat2 double precision, lon2 double precision)
returns double precision
language sql
immutable
set search_path = ''
as $$
  select 6371000 * 2 * asin(sqrt(
    power(sin(radians(lat2 - lat1) / 2), 2) +
    cos(radians(lat1)) * cos(radians(lat2)) * power(sin(radians(lon2 - lon1) / 2), 2)
  ));
$$;

create or replace function public.register_tracker_device(
  p_vehicle_id uuid,
  p_installation_id uuid,
  p_name text,
  p_mode public.tracker_mode,
  p_app_version text,
  p_location_permission text,
  p_background_permission boolean,
  p_notification_permission boolean,
  p_battery_optimization_ignored boolean,
  p_location_services_enabled boolean,
  p_network_connected boolean
)
returns public.tracker_devices
language plpgsql
security definer
set search_path = ''
as $$
declare
  result public.tracker_devices;
  current_owner uuid := auth.uid();
begin
  if not exists (select 1 from public.vehicles where id = p_vehicle_id and owner_id = current_owner) then
    raise exception 'Vehicle not found';
  end if;

  insert into public.tracker_devices (
    owner_id, vehicle_id, installation_id, name, mode, app_version,
    location_permission, background_permission, notification_permission,
    battery_optimization_ignored, location_services_enabled, network_connected, last_seen_at
  ) values (
    current_owner, p_vehicle_id, p_installation_id, left(trim(p_name), 80), p_mode, left(p_app_version, 30),
    p_location_permission, p_background_permission, p_notification_permission,
    p_battery_optimization_ignored, p_location_services_enabled, p_network_connected, now()
  )
  on conflict (owner_id, installation_id) do update set
    vehicle_id = excluded.vehicle_id,
    name = excluded.name,
    mode = excluded.mode,
    app_version = excluded.app_version,
    location_permission = excluded.location_permission,
    background_permission = excluded.background_permission,
    notification_permission = excluded.notification_permission,
    battery_optimization_ignored = excluded.battery_optimization_ignored,
    location_services_enabled = excluded.location_services_enabled,
    network_connected = excluded.network_connected,
    last_seen_at = now()
  returning * into result;

  update public.vehicles set tracking_enabled = true where id = p_vehicle_id;
  return result;
end;
$$;

create or replace function public.start_tracking_session(
  p_tracker_device_id uuid,
  p_start_reason text default 'vehicle_power_on'
)
returns public.tracking_sessions
language plpgsql
security definer
set search_path = ''
as $$
declare
  device public.tracker_devices;
  result public.tracking_sessions;
begin
  select * into device from public.tracker_devices
  where id = p_tracker_device_id and owner_id = auth.uid();
  if not found then raise exception 'Tracker device not found'; end if;

  update public.tracking_sessions
  set status = 'stale', ended_at = coalesce((select max(recorded_at) from public.location_points where session_id = tracking_sessions.id), now()),
      end_reason = 'replaced_by_new_session'
  where tracker_device_id = device.id and status = 'active';

  insert into public.tracking_sessions (owner_id, vehicle_id, tracker_device_id, start_reason)
  values (device.owner_id, device.vehicle_id, device.id, left(p_start_reason, 60))
  returning * into result;

  update public.tracker_devices set is_tracking = true, last_seen_at = now() where id = device.id;
  return result;
end;
$$;

create or replace function public.ingest_location_batch(
  p_session_id uuid,
  p_points jsonb
)
returns jsonb
language plpgsql
security definer
set search_path = ''
as $$
declare
  trip public.tracking_sessions;
  device public.tracker_devices;
  point jsonb;
  previous_point public.location_points;
  inserted_count integer := 0;
  added_distance double precision := 0;
  segment_distance double precision;
  point_speed double precision;
  point_accuracy double precision;
  latest_lat double precision;
  latest_lon double precision;
  latest_speed double precision := 0;
  latest_bearing double precision;
  latest_accuracy double precision;
  latest_battery integer;
  latest_recorded_at timestamptz;
  batch_max_speed double precision := 0;
begin
  select * into trip from public.tracking_sessions
  where id = p_session_id and owner_id = auth.uid() and status = 'active';
  if not found then raise exception 'Active trip not found'; end if;
  if jsonb_typeof(p_points) <> 'array' or jsonb_array_length(p_points) = 0 or jsonb_array_length(p_points) > 100 then
    raise exception 'Location batch must contain 1 to 100 points';
  end if;
  select * into device from public.tracker_devices where id = trip.tracker_device_id;
  select * into previous_point from public.location_points where session_id = trip.id order by sequence_number desc limit 1;

  for point in select * from jsonb_array_elements(p_points) loop
    point_speed := greatest(0, least(120, coalesce((point->>'speed_mps')::double precision, 0)));
    point_accuracy := coalesce((point->>'accuracy_m')::double precision, 999);
    if coalesce((point->>'latitude')::double precision, 999) not between -90 and 90
      or coalesce((point->>'longitude')::double precision, 999) not between -180 and 180 then
      continue;
    end if;

    insert into public.location_points (
      session_id, owner_id, vehicle_id, tracker_device_id, sequence_number,
      recorded_at, latitude, longitude, speed_mps, bearing, altitude_m,
      accuracy_m, battery_percent, network_type, is_mock
    ) values (
      trip.id, trip.owner_id, trip.vehicle_id, trip.tracker_device_id, (point->>'sequence_number')::bigint,
      (point->>'recorded_at')::timestamptz, (point->>'latitude')::double precision,
      (point->>'longitude')::double precision, point_speed,
      nullif(point->>'bearing', '')::double precision, nullif(point->>'altitude_m', '')::double precision,
      point_accuracy, nullif(point->>'battery_percent', '')::integer,
      left(coalesce(point->>'network_type', ''), 30), coalesce((point->>'is_mock')::boolean, false)
    ) on conflict (session_id, sequence_number) do nothing
    returning * into previous_point;

    if found then
      inserted_count := inserted_count + 1;
      batch_max_speed := greatest(batch_max_speed, previous_point.speed_mps);
      if trip.point_count + inserted_count = 1 then
        update public.tracking_sessions set start_latitude = previous_point.latitude, start_longitude = previous_point.longitude where id = trip.id;
      else
        select public.haversine_metres(p.latitude, p.longitude, previous_point.latitude, previous_point.longitude)
          into segment_distance
        from public.location_points p
        where p.session_id = trip.id and p.sequence_number < previous_point.sequence_number
        order by p.sequence_number desc limit 1;
        if previous_point.accuracy_m <= 100 and coalesce(segment_distance, 0) between 0 and 2000 then
          added_distance := added_distance + coalesce(segment_distance, 0);
        end if;
      end if;
      latest_lat := previous_point.latitude;
      latest_lon := previous_point.longitude;
      latest_speed := previous_point.speed_mps;
      latest_bearing := previous_point.bearing;
      latest_accuracy := previous_point.accuracy_m;
      latest_battery := previous_point.battery_percent;
      latest_recorded_at := previous_point.recorded_at;
    end if;
  end loop;

  if inserted_count > 0 then
    update public.tracking_sessions set
      point_count = point_count + inserted_count,
      distance_m = distance_m + added_distance,
      max_speed_mps = greatest(max_speed_mps, batch_max_speed),
      duration_seconds = greatest(0, extract(epoch from (latest_recorded_at - started_at))::integer),
      end_latitude = latest_lat,
      end_longitude = latest_lon
    where id = trip.id;

    update public.tracker_devices set
      is_tracking = true, last_latitude = latest_lat, last_longitude = latest_lon,
      last_speed_mps = latest_speed, last_bearing = latest_bearing,
      last_accuracy_m = latest_accuracy, last_battery_percent = latest_battery,
      last_seen_at = now(), network_connected = true
    where id = trip.tracker_device_id;
  end if;
  return jsonb_build_object('accepted', inserted_count, 'distance_added_m', round(added_distance::numeric, 1));
end;
$$;

create or replace function public.finish_tracking_session(
  p_session_id uuid,
  p_end_reason text default 'vehicle_power_off'
)
returns public.tracking_sessions
language plpgsql
security definer
set search_path = ''
as $$
declare result public.tracking_sessions;
begin
  update public.tracking_sessions set
    status = 'completed',
    ended_at = coalesce((select max(recorded_at) from public.location_points where session_id = p_session_id), now()),
    end_reason = left(p_end_reason, 60),
    duration_seconds = greatest(0, extract(epoch from (coalesce((select max(recorded_at) from public.location_points where session_id = p_session_id), now()) - started_at))::integer),
    average_speed_mps = case when extract(epoch from (coalesce((select max(recorded_at) from public.location_points where session_id = p_session_id), now()) - started_at)) > 0
      then distance_m / extract(epoch from (coalesce((select max(recorded_at) from public.location_points where session_id = p_session_id), now()) - started_at)) else 0 end
  where id = p_session_id and owner_id = auth.uid() and status = 'active'
  returning * into result;
  if not found then raise exception 'Active trip not found'; end if;
  update public.tracker_devices set is_tracking = false, last_speed_mps = 0, last_seen_at = now() where id = result.tracker_device_id;
  return result;
end;
$$;

create or replace function public.close_stale_tracking_sessions(p_stale_after interval default interval '5 minutes')
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare closed_count integer;
begin
  with closed as (
    update public.tracking_sessions s set
      status = 'stale',
      ended_at = coalesce((select max(p.recorded_at) from public.location_points p where p.session_id = s.id), s.started_at),
      end_reason = 'tracker_offline',
      duration_seconds = greatest(0, extract(epoch from (coalesce((select max(p.recorded_at) from public.location_points p where p.session_id = s.id), s.started_at) - s.started_at))::integer),
      average_speed_mps = case when extract(epoch from (coalesce((select max(p.recorded_at) from public.location_points p where p.session_id = s.id), s.started_at) - s.started_at)) > 0
        then s.distance_m / extract(epoch from (coalesce((select max(p.recorded_at) from public.location_points p where p.session_id = s.id), s.started_at) - s.started_at)) else 0 end
    from public.tracker_devices d
    where s.tracker_device_id = d.id and s.status = 'active'
      and coalesce(d.last_seen_at, s.started_at) < now() - p_stale_after
    returning s.tracker_device_id
  )
  select count(*) into closed_count from closed;
  update public.tracker_devices d set is_tracking = false, last_speed_mps = 0
  where d.is_tracking and not exists (select 1 from public.tracking_sessions s where s.tracker_device_id = d.id and s.status = 'active');
  return closed_count;
end;
$$;

revoke all on function public.create_print_order(uuid,text,text,text,text,text,text,text,text,text) from public;
revoke all on function public.choose_self_print(uuid) from public;
revoke all on function public.register_tracker_device(uuid,uuid,text,public.tracker_mode,text,text,boolean,boolean,boolean,boolean,boolean) from public;
revoke all on function public.start_tracking_session(uuid,text) from public;
revoke all on function public.ingest_location_batch(uuid,jsonb) from public;
revoke all on function public.finish_tracking_session(uuid,text) from public;
revoke all on function public.close_stale_tracking_sessions(interval) from public;

grant execute on function public.create_print_order(uuid,text,text,text,text,text,text,text,text,text) to authenticated;
grant execute on function public.choose_self_print(uuid) to authenticated;
grant execute on function public.register_tracker_device(uuid,uuid,text,public.tracker_mode,text,text,boolean,boolean,boolean,boolean,boolean) to authenticated;
grant execute on function public.start_tracking_session(uuid,text) to authenticated;
grant execute on function public.ingest_location_batch(uuid,jsonb) to authenticated;
grant execute on function public.finish_tracking_session(uuid,text) to authenticated;
grant execute on function public.close_stale_tracking_sessions(interval) to service_role;

alter publication supabase_realtime add table public.tracker_devices;
alter publication supabase_realtime add table public.tracking_sessions;
alter publication supabase_realtime add table public.print_orders;
