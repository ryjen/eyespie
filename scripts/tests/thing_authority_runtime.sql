\set ON_ERROR_STOP on

-- Runtime authorization contract for #122.
-- This file runs only against the disposable local Supabase database in CI.
-- It validates executable migration/RLS/RPC/storage semantics rather than SQL text.

begin;

-- Stable test identities. Only Player.user_id is required by the application
-- authorization predicates; these additional subjects do not need login rows.
insert into public."Player" (id, first_name, last_name, user_id)
values
  ('b0000000-0000-0000-0000-000000000001', 'Participant', 'Two', 'b0000000-0000-0000-0000-000000000011'),
  ('c0000000-0000-0000-0000-000000000001', 'Unrelated', 'Three', 'c0000000-0000-0000-0000-000000000011')
on conflict (id) do nothing;

insert into public."GamePlayer" (player_id, game_id)
values ('b0000000-0000-0000-0000-000000000001', '10444bc1-abd6-46d9-bac8-bb47dc38130a')
on conflict do nothing;

insert into public."Thing" (
  id,
  created_by,
  location,
  image_path,
  embedding,
  proof,
  guessed
)
values (
  'a0000000-0000-0000-0000-000000000001',
  'a3f8a59a-33a4-4e79-bccb-0a008df43cb4',
  point(49.2608724, -123.113952),
  'a3f8a59a-33a4-4e79-bccb-0a008df43cb4/authority.png',
  ('[' || array_to_string(array_fill(1.0::double precision, array[1024]), ',') || ']')::extensions.vector(1024),
  '{"hidden":"authority-only"}'::jsonb,
  false
)
on conflict (id) do nothing;

insert into public."GameThing" (thing_id, game_id)
values ('a0000000-0000-0000-0000-000000000001', '10444bc1-abd6-46d9-bac8-bb47dc38130a')
on conflict do nothing;

insert into storage.buckets (id, name, public)
values ('images', 'images', false)
on conflict (id) do update set public = false;

insert into storage.objects (bucket_id, name)
values ('images', 'a3f8a59a-33a4-4e79-bccb-0a008df43cb4/authority.png')
on conflict (bucket_id, name) do nothing;

-- Creator can read full authority state and owned Storage objects.
set local role authenticated;
select set_config('request.jwt.claim.sub', 'f77c3a35-1236-49bc-add5-dad3b806da83', true);

do $$
begin
  if (select count(*) from public."Thing" where id = 'a0000000-0000-0000-0000-000000000001') <> 1 then
    raise exception 'creator cannot read owned Thing authority';
  end if;

  if (select count(*) from storage.objects where bucket_id = 'images' and name = 'a3f8a59a-33a4-4e79-bccb-0a008df43cb4/authority.png') <> 1 then
    raise exception 'creator cannot read owned Storage object';
  end if;
end;
$$;

reset role;

-- A game participant can use safe game/nearby/match capabilities but cannot
-- select the creator's full Thing row or image object.
set local role authenticated;
select set_config('request.jwt.claim.sub', 'b0000000-0000-0000-0000-000000000011', true);

do $$
declare
  match_count integer;
  safe_count integer;
  nearby_count integer;
begin
  if (select count(*) from public."Thing" where id = 'a0000000-0000-0000-0000-000000000001') <> 0 then
    raise exception 'participant can read creator Thing authority';
  end if;

  if (select count(*) from storage.objects where bucket_id = 'images' and name = 'a3f8a59a-33a4-4e79-bccb-0a008df43cb4/authority.png') <> 0 then
    raise exception 'participant can read creator Storage object';
  end if;

  select count(*) into safe_count
  from public.game_things_safe('10444bc1-abd6-46d9-bac8-bb47dc38130a')
  where id = 'a0000000-0000-0000-0000-000000000001';
  if safe_count <> 1 then
    raise exception 'participant cannot read safe game Thing projection';
  end if;

  select count(*) into nearby_count
  from public.thingsnearby(5.0, 49.2608724, -123.113952)
  where id = 'a0000000-0000-0000-0000-000000000001';
  if nearby_count <> 1 then
    raise exception 'participant cannot read authorized nearby projection';
  end if;

  select count(*) into match_count
  from public.match_thing(
    'a0000000-0000-0000-0000-000000000001',
    ('[' || array_to_string(array_fill(1.0::double precision, array[1024]), ',') || ']')::extensions.vector(1024),
    0.5
  ) m
  where m.id = 'a0000000-0000-0000-0000-000000000001'
    and m.matched
    and m.similarity > 0.99;
  if match_count <> 1 then
    raise exception 'participant cannot execute authorized target-specific match';
  end if;

  begin
    insert into storage.objects (bucket_id, name)
    values ('images', 'a3f8a59a-33a4-4e79-bccb-0a008df43cb4/participant-forbidden.png');
    raise exception 'participant inserted into another player Storage namespace';
  exception
    when insufficient_privilege then null;
  end;
end;
$$;

reset role;

-- An unrelated authenticated subject receives neither authority nor safe game,
-- nearby, matching, or Storage access.
set local role authenticated;
select set_config('request.jwt.claim.sub', 'c0000000-0000-0000-0000-000000000011', true);

do $$
begin
  if (select count(*) from public."Thing" where id = 'a0000000-0000-0000-0000-000000000001') <> 0 then
    raise exception 'unrelated subject can read Thing authority';
  end if;

  if (select count(*) from storage.objects where bucket_id = 'images' and name = 'a3f8a59a-33a4-4e79-bccb-0a008df43cb4/authority.png') <> 0 then
    raise exception 'unrelated subject can read Storage object';
  end if;

  if exists (
    select 1
    from public.thingsnearby(5.0, 49.2608724, -123.113952)
    where id = 'a0000000-0000-0000-0000-000000000001'
  ) then
    raise exception 'unrelated subject can discover nearby Thing';
  end if;

  begin
    perform 1 from public.game_things_safe('10444bc1-abd6-46d9-bac8-bb47dc38130a');
    raise exception 'unrelated subject can read safe game projection';
  exception
    when insufficient_privilege then null;
  end;

  begin
    perform 1
    from public.match_thing(
      'a0000000-0000-0000-0000-000000000001',
      ('[' || array_to_string(array_fill(1.0::double precision, array[1024]), ',') || ']')::extensions.vector(1024),
      0.5
    );
    raise exception 'unrelated subject can invoke target-specific match';
  exception
    when insufficient_privilege then null;
  end;
end;
$$;

reset role;

-- Creator can write/delete only within the owned namespace.
set local role authenticated;
select set_config('request.jwt.claim.sub', 'f77c3a35-1236-49bc-add5-dad3b806da83', true);

do $$
declare
  affected integer;
begin
  insert into storage.objects (bucket_id, name)
  values ('images', 'a3f8a59a-33a4-4e79-bccb-0a008df43cb4/creator-write.png');

  delete from storage.objects
  where bucket_id = 'images'
    and name = 'a3f8a59a-33a4-4e79-bccb-0a008df43cb4/creator-write.png';
  get diagnostics affected = row_count;
  if affected <> 1 then
    raise exception 'creator cannot delete owned Storage object';
  end if;
end;
$$;

reset role;
rollback;
