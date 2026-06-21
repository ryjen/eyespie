-- Harden RLS policies for EyesPie

-- Drop existing permissive policies
DROP POLICY IF EXISTS "user_policy" ON "public"."Thing";
DROP POLICY IF EXISTS "user_policy" ON "public"."Game";
DROP POLICY IF EXISTS "user_policy" ON "public"."GamePlayer";
DROP POLICY IF EXISTS "user_policy" ON "public"."GameThing";
DROP POLICY IF EXISTS "user_policy" ON "public"."Guess";
DROP POLICY IF EXISTS "user_policy" ON "public"."Player";

-- Player policies
CREATE POLICY "Players are visible to all authenticated users"
ON "public"."Player" FOR SELECT
TO authenticated
USING (true);

CREATE POLICY "Users can only update their own player profile"
ON "public"."Player" FOR UPDATE
TO authenticated
USING (auth.uid() = user_id);

-- Thing policies
CREATE POLICY "Things are visible to all authenticated users"
ON "public"."Thing" FOR SELECT
TO authenticated
USING (true);

CREATE POLICY "Users can only insert their own things"
ON "public"."Thing" FOR INSERT
TO authenticated
WITH CHECK (
  EXISTS (
    SELECT 1 FROM "public"."Player"
    WHERE id = created_by AND user_id = auth.uid()
  )
);

CREATE POLICY "Users can only update their own things"
ON "public"."Thing" FOR UPDATE
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM "public"."Player"
    WHERE id = created_by AND user_id = auth.uid()
  )
);

-- Guess policies
CREATE POLICY "Users can see their own guesses"
ON "public"."Guess" FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM "public"."Player"
    WHERE id = created_by AND user_id = auth.uid()
  )
);

CREATE POLICY "Users can see guesses on their own things"
ON "public"."Guess" FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM "public"."Thing" t
    JOIN "public"."Player" p ON t.created_by = p.id
    WHERE t.id = thing_id AND p.user_id = auth.uid()
  )
);

CREATE POLICY "Users can insert their own guesses"
ON "public"."Guess" FOR INSERT
TO authenticated
WITH CHECK (
  EXISTS (
    SELECT 1 FROM "public"."Player"
    WHERE id = created_by AND user_id = auth.uid()
  )
);

-- Game policies (Simplified for now, assuming games are public to join)
CREATE POLICY "Games are visible to all authenticated users"
ON "public"."Game" FOR SELECT
TO authenticated
USING (true);

CREATE POLICY "Anyone can join a game"
ON "public"."GamePlayer" FOR INSERT
TO authenticated
WITH CHECK (
  EXISTS (
    SELECT 1 FROM "public"."Player"
    WHERE id = player_id AND user_id = auth.uid()
  )
);

CREATE POLICY "Users can see their own game participation"
ON "public"."GamePlayer" FOR SELECT
TO authenticated
USING (
  EXISTS (
    SELECT 1 FROM "public"."Player"
    WHERE id = player_id AND user_id = auth.uid()
  )
);

-- Usage events
ALTER TABLE public.usage_events ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "usage_events_insert_own" ON public.usage_events;
CREATE POLICY "Users can insert their own usage events"
ON public.usage_events FOR INSERT
TO authenticated
WITH CHECK (auth.uid() = user_id);

CREATE POLICY "Users can see their own usage events"
ON public.usage_events FOR SELECT
TO authenticated
USING (auth.uid() = user_id);
