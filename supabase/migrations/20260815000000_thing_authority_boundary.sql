-- #122: narrow Thing/image authority and remove durable bearer capabilities.
--
-- Full Thing rows remain creator authority. Game/nearby/match flows use purpose-specific
-- projections/RPCs that do not return image object paths, precise location, proof, or embeddings.

-- Durable image identity is an opaque Storage object path, never a signed URL.
alter table public."Thing"
  add column if not exists image_path text;

-- Preserve legacy values only when they already look like opaque object paths.
update public."Thing"
set image_path = "imageUrl"
where image_path is null
  and "imageUrl" is not null
  and "imageUrl" !~* '^(https?://|/storage/v1/object/sign/)';

-- Fail closed for previously persisted bearer capabilities. Old captures without an opaque path
-- are intentionally no longer renderable and may be recreated during closed alpha.
update public."Thing"
set "imageUrl" = null
where "imageUrl" is not null;

comment on column public."Thing".image_path is
  'Creator-authority Supabase Storage object path. Never stores a signed/public URL.';
comment on column public."Thing"."imageUrl" is
  'Deprecated legacy field. Do not persist signed URLs; retained nullable for migration compatibility.';

-- Small SECURITY DEFINER predicates keep RLS independent from later public/private Player projections.
create or replace function public.eyespie_owns_player(target_player_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select auth.uid() is not null and exists (
    select 1
    from public."Player" p
    where p.id = target_player_id
      and p.user_id = auth.uid()
  );
$$;

create or replace function public.eyespie_owns_thing(target_thing_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select auth.uid() is not null and exists (
    select 1
    from public."Thing" t
    join public."Player" p on p.id = t.created_by
    where t.id = target_thing_id
      and p.user_id = auth.uid()
  );
$$;

create or replace function public.eyespie_participates_in_game(target_game_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select auth.uid() is not null and exists (
    select 1
    from public."GamePlayer" gp
    join public."Player" p on p.id = gp.player_id
    where gp.game_id = target_game_id
      and p.user_id = auth.uid()
  );
$$;

revoke all on function public.eyespie_owns_player(uuid) from public, anon;
revoke all on function public.eyespie_owns_thing(uuid) from public, anon;
revoke all on function public.eyespie_participates_in_game(uuid) from public, anon;
grant execute on function public.eyespie_owns_player(uuid) to authenticated;
grant execute on function public.eyespie_owns_thing(uuid) to authenticated;
grant execute on function public.eyespie_participates_in_game(uuid) to authenticated;

-- Full Thing authority is creator-only.
drop policy if exists "Things are visible to all authenticated users" on public."Thing";
drop policy if exists "Users can only insert their own things" on public."Thing";
drop policy if exists "Users can only update their own things" on public."Thing";
drop policy if exists "Users can only delete their own things" on public."Thing";
drop policy if exists "Thing creator authority select" on public."Thing";
drop policy if exists "Thing creator authority insert" on public."Thing";
drop policy if exists "Thing creator authority update" on public."Thing";
drop policy if exists "Thing creator authority delete" on public."Thing";

create policy "Thing creator authority select"
on public."Thing" for select
to authenticated
using (public.eyespie_owns_player(created_by));

create policy "Thing creator authority insert"
on public."Thing" for insert
to authenticated
with check (public.eyespie_owns_player(created_by));

create policy "Thing creator authority update"
on public."Thing" for update
to authenticated
using (public.eyespie_owns_player(created_by))
with check (public.eyespie_owns_player(created_by));

create policy "Thing creator authority delete"
on public."Thing" for delete
to authenticated
using (public.eyespie_owns_player(created_by));

-- GameThing rows are visible only to participants (or the Thing owner) and can be mutated only by
-- a participant attaching/removing a Thing they own.
drop policy if exists "user_policy" on public."GameThing";
drop policy if exists "Game participants can see game things" on public."GameThing";
drop policy if exists "Owners can attach things to joined games" on public."GameThing";
drop policy if exists "Owners can remove things from joined games" on public."GameThing";

create policy "Game participants can see game things"
on public."GameThing" for select
to authenticated
using (
  public.eyespie_participates_in_game(game_id)
  or public.eyespie_owns_thing(thing_id)
);

create policy "Owners can attach things to joined games"
on public."GameThing" for insert
to authenticated
with check (
  public.eyespie_participates_in_game(game_id)
  and public.eyespie_owns_thing(thing_id)
);

create policy "Owners can remove things from joined games"
on public."GameThing" for delete
to authenticated
using (
  public.eyespie_participates_in_game(game_id)
  and public.eyespie_owns_thing(thing_id)
);

-- Replace the full-row nearby RPC with a safe projection. It is limited to Things the caller owns
-- or Things in a game the caller participates in; precise location remains server-side.
drop function if exists public.thingsnearby(double precision, double precision, double precision);
create function public.thingsnearby(
  distance double precision,
  latitude double precision,
  longitude double precision
)
returns table (
  id uuid,
  created_at timestamp with time zone,
  guessed boolean
)
language sql
stable
security definer
set search_path = public
as $$
  select t.id, t.created_at, coalesce(t.guessed, false)
  from public."Thing" t
  where t.location is not null
    and (t.location <@> point(latitude, longitude)) <= distance
    and (
      public.eyespie_owns_thing(t.id)
      or exists (
        select 1
        from public."GameThing" gt
        where gt.thing_id = t.id
          and public.eyespie_participates_in_game(gt.game_id)
      )
    );
$$;

revoke all on function public.thingsnearby(double precision, double precision, double precision)
  from public, anon;
grant execute on function public.thingsnearby(double precision, double precision, double precision)
  to authenticated;

-- Purpose-specific safe game projection. #153 may add clue text later, but never expected answers,
-- image paths, precise location, embeddings, or generated-provider provenance.
create or replace function public.game_things_safe(target_game_id uuid)
returns table (
  id uuid,
  created_at timestamp with time zone,
  guessed boolean
)
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  if not public.eyespie_participates_in_game(target_game_id) then
    raise exception 'not authorized for game'
      using errcode = '42501';
  end if;

  return query
  select t.id, t.created_at, coalesce(t.guessed, false)
  from public."GameThing" gt
  join public."Thing" t on t.id = gt.thing_id
  where gt.game_id = target_game_id;
end;
$$;

revoke all on function public.game_things_safe(uuid) from public, anon;
grant execute on function public.game_things_safe(uuid) to authenticated;

-- Remove the corpus-wide/full-row embedding oracle and replace it with one authorized target.
drop function if exists public.match_things(extensions.vector, float, int);
drop function if exists public.match_thing(uuid, extensions.vector, float);
create function public.match_thing(
  target_thing_id uuid,
  query_embedding extensions.vector(1024),
  match_threshold float
)
returns table (
  id uuid,
  similarity float,
  matched boolean
)
language plpgsql
stable
security definer
set search_path = public, extensions
as $$
declare
  target_similarity float;
begin
  if auth.uid() is null then
    raise exception 'authentication required'
      using errcode = '42501';
  end if;

  if not (
    public.eyespie_owns_thing(target_thing_id)
    or exists (
      select 1
      from public."GameThing" gt
      where gt.thing_id = target_thing_id
        and public.eyespie_participates_in_game(gt.game_id)
    )
  ) then
    raise exception 'not authorized for Thing'
      using errcode = '42501';
  end if;

  select 1 - (t.embedding <=> query_embedding)
    into target_similarity
  from public."Thing" t
  where t.id = target_thing_id
    and t.embedding is not null;

  if target_similarity is null then
    raise exception 'Thing embedding unavailable'
      using errcode = '22023';
  end if;

  return query
  select target_thing_id, target_similarity, target_similarity >= match_threshold;
end;
$$;

revoke all on function public.match_thing(uuid, extensions.vector, float) from public, anon;
grant execute on function public.match_thing(uuid, extensions.vector, float) to authenticated;

-- Explicit Storage authorization. Object names are created as <player-id>/<uuid>.<ext>.
drop policy if exists "EyesPie image owners can read" on storage.objects;
drop policy if exists "EyesPie image owners can insert" on storage.objects;
drop policy if exists "EyesPie image owners can update" on storage.objects;
drop policy if exists "EyesPie image owners can delete" on storage.objects;

create policy "EyesPie image owners can read"
on storage.objects for select
to authenticated
using (
  bucket_id = 'images'
  and exists (
    select 1
    from public."Player" p
    where p.user_id = auth.uid()
      and p.id::text = split_part(name, '/', 1)
  )
);

create policy "EyesPie image owners can insert"
on storage.objects for insert
to authenticated
with check (
  bucket_id = 'images'
  and exists (
    select 1
    from public."Player" p
    where p.user_id = auth.uid()
      and p.id::text = split_part(name, '/', 1)
  )
);

create policy "EyesPie image owners can update"
on storage.objects for update
to authenticated
using (
  bucket_id = 'images'
  and exists (
    select 1
    from public."Player" p
    where p.user_id = auth.uid()
      and p.id::text = split_part(name, '/', 1)
  )
)
with check (
  bucket_id = 'images'
  and exists (
    select 1
    from public."Player" p
    where p.user_id = auth.uid()
      and p.id::text = split_part(name, '/', 1)
  )
);

create policy "EyesPie image owners can delete"
on storage.objects for delete
to authenticated
using (
  bucket_id = 'images'
  and exists (
    select 1
    from public."Player" p
    where p.user_id = auth.uid()
      and p.id::text = split_part(name, '/', 1)
  )
);
