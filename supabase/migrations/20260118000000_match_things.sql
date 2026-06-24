-- Add semantic matching support for Things.
--
-- This migration updates the embedding column to match the current app's
-- 32-dimensional deterministic embeddings and adds the RPC function
-- used by ThingsRemoteSource.

alter table public."Thing" alter column embedding type extensions.vector(32);

create or replace function match_things (
  query_embedding extensions.vector(32),
  match_threshold float,
  match_count int
)
returns table (
  id uuid,
  similarity float,
  content jsonb
)
language plpgsql
as $$
begin
  return query
  select
    "Thing".id,
    1 - ("Thing".embedding <=> query_embedding) as similarity,
    to_jsonb("Thing".*) as content
  from "Thing"
  where 1 - ("Thing".embedding <=> query_embedding) > match_threshold
  order by "Thing".embedding <=> query_embedding
  limit match_count;
end;
$$;
