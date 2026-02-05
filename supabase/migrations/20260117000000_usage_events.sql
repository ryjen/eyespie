-- Usage / observability events captured from clients (offline-first).
--
-- This intentionally stores `properties_json` as jsonb so we can filter and aggregate later.
--
-- If you want strict auth, add RLS policies; for simple ingestion from authenticated users,
-- enable RLS and allow inserts where auth.uid() = user_id.

create table if not exists public.usage_events (
  id uuid primary key default gen_random_uuid(),
  created_at timestamptz not null default now(),

  event_id text not null unique,
  schema_name text not null,
  schema_version int not null,
  event_type text not null,

  user_id uuid null,
  session_id text null,

  timestamp_ms bigint not null,
  properties_json jsonb not null default '{}'::jsonb
);

create index if not exists usage_events_user_id_idx on public.usage_events(user_id);
create index if not exists usage_events_timestamp_idx on public.usage_events(timestamp_ms);
create index if not exists usage_events_schema_idx on public.usage_events(schema_name, schema_version);

-- Optional (recommended) if you're using RLS:
-- alter table public.usage_events enable row level security;
-- create policy "usage_events_insert_own" on public.usage_events
--   for insert
--   to authenticated
--   with check (auth.uid() = user_id);
