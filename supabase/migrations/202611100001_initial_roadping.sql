-- RoadPing initial schema
-- Apply with: supabase db push

create extension if not exists pgcrypto;

create type public.phone_visibility as enum ('hidden', 'masked', 'visible');
create type public.alert_status as enum ('new', 'seen', 'resolved');

create table public.profiles (
  id uuid primary key references auth.users(id) on delete cascade,
  full_name text not null default '',
  phone text not null default '',
  emergency_contact_name text not null default '',
  emergency_contact_phone text not null default '',
  phone_visibility public.phone_visibility not null default 'hidden',
  social_links jsonb not null default '{}'::jsonb,
  push_enabled boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint social_links_is_object check (jsonb_typeof(social_links) = 'object')
);

create table public.vehicles (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.profiles(id) on delete cascade,
  public_token uuid not null default gen_random_uuid() unique,
  registration_number text not null,
  nickname text not null default '',
  vehicle_type text not null default 'Car',
  make text not null default '',
  model text not null default '',
  color text not null default '',
  year integer,
  card_theme text not null default 'midnight',
  is_active boolean not null default true,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint registration_not_blank check (length(trim(registration_number)) >= 2),
  constraint valid_vehicle_year check (year is null or year between 1950 and 2100),
  constraint valid_card_theme check (card_theme in ('midnight', 'ocean', 'ember', 'forest'))
);

create unique index vehicles_registration_unique on public.vehicles (upper(registration_number));
create index vehicles_owner_id_idx on public.vehicles(owner_id);
create index vehicles_public_token_idx on public.vehicles(public_token) where is_active;

create table public.incident_alerts (
  id uuid primary key default gen_random_uuid(),
  vehicle_id uuid not null references public.vehicles(id) on delete cascade,
  owner_id uuid not null references public.profiles(id) on delete cascade,
  alert_type text not null,
  message text not null default '',
  reporter_contact text,
  latitude double precision,
  longitude double precision,
  status public.alert_status not null default 'new',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  constraint allowed_alert_type check (alert_type in ('blocking_access', 'parking_issue', 'lights_on', 'accident_damage', 'vehicle_attention', 'emergency', 'other')),
  constraint valid_message_length check (length(message) <= 500),
  constraint valid_latitude check (latitude is null or latitude between -90 and 90),
  constraint valid_longitude check (longitude is null or longitude between -180 and 180)
);

create index incident_alerts_owner_created_idx on public.incident_alerts(owner_id, created_at desc);
create index incident_alerts_vehicle_created_idx on public.incident_alerts(vehicle_id, created_at desc);
create index incident_alerts_new_idx on public.incident_alerts(owner_id, status) where status = 'new';

create table public.push_subscriptions (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.profiles(id) on delete cascade,
  endpoint text not null unique,
  subscription jsonb not null,
  user_agent text not null default '',
  last_used_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index push_subscriptions_owner_idx on public.push_subscriptions(owner_id);

create table public.android_device_tokens (
  id uuid primary key default gen_random_uuid(),
  owner_id uuid not null references public.profiles(id) on delete cascade,
  fcm_token text not null unique,
  device_name text not null default '',
  last_used_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);

create index android_device_tokens_owner_idx on public.android_device_tokens(owner_id);

create or replace function public.set_updated_at()
returns trigger
language plpgsql
security invoker
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger profiles_updated_at before update on public.profiles
for each row execute function public.set_updated_at();
create trigger vehicles_updated_at before update on public.vehicles
for each row execute function public.set_updated_at();
create trigger incident_alerts_updated_at before update on public.incident_alerts
for each row execute function public.set_updated_at();

create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.profiles (
    id,
    full_name,
    phone,
    emergency_contact_phone
  ) values (
    new.id,
    coalesce(new.raw_user_meta_data ->> 'full_name', ''),
    coalesce(new.raw_user_meta_data ->> 'phone', ''),
    coalesce(new.raw_user_meta_data ->> 'emergency_contact_phone', '')
  )
  on conflict (id) do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created
after insert on auth.users
for each row execute function public.handle_new_user();

alter table public.profiles enable row level security;
alter table public.vehicles enable row level security;
alter table public.incident_alerts enable row level security;
alter table public.push_subscriptions enable row level security;
alter table public.android_device_tokens enable row level security;

create policy "owners read their profile"
on public.profiles for select
to authenticated
using ((select auth.uid()) = id);

create policy "owners create their profile"
on public.profiles for insert
to authenticated
with check ((select auth.uid()) = id);

create policy "owners update their profile"
on public.profiles for update
to authenticated
using ((select auth.uid()) = id)
with check ((select auth.uid()) = id);

create policy "owners read their vehicles"
on public.vehicles for select
to authenticated
using ((select auth.uid()) = owner_id);

create policy "owners create their vehicles"
on public.vehicles for insert
to authenticated
with check ((select auth.uid()) = owner_id);

create policy "owners update their vehicles"
on public.vehicles for update
to authenticated
using ((select auth.uid()) = owner_id)
with check ((select auth.uid()) = owner_id);

create policy "owners delete their vehicles"
on public.vehicles for delete
to authenticated
using ((select auth.uid()) = owner_id);

create policy "owners read their alerts"
on public.incident_alerts for select
to authenticated
using ((select auth.uid()) = owner_id);

create policy "owners update alert status"
on public.incident_alerts for update
to authenticated
using ((select auth.uid()) = owner_id)
with check ((select auth.uid()) = owner_id);

create policy "owners manage their web push subscriptions"
on public.push_subscriptions for all
to authenticated
using ((select auth.uid()) = owner_id)
with check ((select auth.uid()) = owner_id);

create policy "owners manage their Android tokens"
on public.android_device_tokens for all
to authenticated
using ((select auth.uid()) = owner_id)
with check ((select auth.uid()) = owner_id);

-- The public scanner calls this function instead of selecting tables directly.
-- That keeps emergency contacts and private phone data outside the public API.
create or replace function public.get_public_vehicle_profile(p_public_token uuid)
returns table (
  vehicle_id uuid,
  public_token uuid,
  registration_number text,
  make text,
  model text,
  color text,
  vehicle_type text,
  owner_name text,
  phone_display text,
  phone_href text,
  social_links jsonb
)
language sql
stable
security definer
set search_path = ''
as $$
  select
    v.id,
    v.public_token,
    v.registration_number,
    v.make,
    v.model,
    v.color,
    v.vehicle_type,
    case
      when trim(p.full_name) = '' then 'Vehicle owner'
      when position(' ' in trim(p.full_name)) > 0 then split_part(trim(p.full_name), ' ', 1) || ' ' || left(split_part(trim(p.full_name), ' ', 2), 1) || '.'
      else trim(p.full_name)
    end,
    case
      when p.phone_visibility = 'visible' then p.phone
      when p.phone_visibility = 'masked' and length(regexp_replace(p.phone, '[^0-9+]', '', 'g')) >= 6
        then left(regexp_replace(p.phone, '[^0-9+]', '', 'g'), 3) || ' ••• •' || right(regexp_replace(p.phone, '[^0-9+]', '', 'g'), 2)
      else null
    end,
    case when p.phone_visibility = 'visible' and trim(p.phone) <> '' then 'tel:' || regexp_replace(p.phone, '[^0-9+]', '', 'g') else null end,
    p.social_links
  from public.vehicles v
  join public.profiles p on p.id = v.owner_id
  where v.public_token = p_public_token
    and v.is_active = true
  limit 1;
$$;

create or replace function public.submit_public_alert(
  p_public_token uuid,
  p_alert_type text,
  p_message text default '',
  p_reporter_contact text default null,
  p_latitude double precision default null,
  p_longitude double precision default null
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  target_vehicle public.vehicles%rowtype;
  new_alert_id uuid;
  recent_count integer;
begin
  select * into target_vehicle
  from public.vehicles
  where public_token = p_public_token and is_active = true;

  if not found then
    raise exception 'Vehicle card is unavailable';
  end if;

  if p_alert_type not in ('blocking_access', 'parking_issue', 'lights_on', 'accident_damage', 'vehicle_attention', 'emergency', 'other') then
    raise exception 'Invalid alert type';
  end if;

  if length(coalesce(p_message, '')) > 500 then
    raise exception 'Message is too long';
  end if;

  -- Vehicle-level flood protection. A reverse proxy / Edge Function can add IP-level limits.
  select count(*) into recent_count
  from public.incident_alerts
  where vehicle_id = target_vehicle.id
    and created_at > now() - interval '1 minute';

  if recent_count >= 20 then
    raise exception 'Too many reports. Please wait a moment and try again.';
  end if;

  insert into public.incident_alerts (
    vehicle_id,
    owner_id,
    alert_type,
    message,
    reporter_contact,
    latitude,
    longitude
  ) values (
    target_vehicle.id,
    target_vehicle.owner_id,
    p_alert_type,
    trim(coalesce(p_message, '')),
    nullif(trim(coalesce(p_reporter_contact, '')), ''),
    p_latitude,
    p_longitude
  ) returning id into new_alert_id;

  return new_alert_id;
end;
$$;

revoke all on function public.get_public_vehicle_profile(uuid) from public;
revoke all on function public.submit_public_alert(uuid, text, text, text, double precision, double precision) from public;
grant execute on function public.get_public_vehicle_profile(uuid) to anon, authenticated;
grant execute on function public.submit_public_alert(uuid, text, text, text, double precision, double precision) to anon, authenticated;

-- Supabase Realtime delivers new alerts while the web dashboard is open.
alter publication supabase_realtime add table public.incident_alerts;
