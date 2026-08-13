-- Make embedding compatibility explicit.
--
-- Existing rows intentionally remain NULL and therefore do not participate in
-- model-aware matching. A same-dimension vector from an unknown model is not a
-- compatible embedding merely because pgvector can compute a distance for it.

alter table public."Thing"
  add column if not exists embedding_model_id text;

create index if not exists thing_embedding_model_id_idx
  on public."Thing" (embedding_model_id);

-- Remove the legacy three-argument RPC before introducing the model-aware contract.
drop function if exists match_things(extensions.vector, float, int);
drop function if exists match_things(extensions.vector, text, float, int);

create function match_things (
  query_embedding extensions.vector(1024),
  query_embedding_model_id text,
  match_threshold float,
  match_count int
)
returns table (
  id uuid,
  similarity float
)
language sql
stable
as $$
  select
    thing.id,
    (1 - (thing.embedding <=> query_embedding))::float as similarity
  from public."Thing" as thing
  where thing.embedding is not null
    and thing.embedding_model_id = query_embedding_model_id
    and 1 - (thing.embedding <=> query_embedding) > match_threshold
  order by thing.embedding <=> query_embedding
  limit match_count;
$$;
