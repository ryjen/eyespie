from __future__ import annotations

import re
import unittest
from pathlib import Path


REPO_ROOT = Path(__file__).resolve().parents[2]
MIGRATION = REPO_ROOT / "supabase" / "migrations" / "20260815000000_thing_authority_boundary.sql"


def normalized_sql() -> str:
    sql = MIGRATION.read_text(encoding="utf-8")
    # Comments are documentation, not executable evidence. Strip them so a contract
    # cannot pass because an invariant appears only in prose.
    sql = re.sub(r"--.*?$", "", sql, flags=re.MULTILINE)
    return re.sub(r"\s+", " ", sql).strip().lower()


def function_body(sql: str, name: str) -> str:
    match = re.search(
        rf"create(?: or replace)? function public\.{re.escape(name)}\b(.*?)\$\$\s*;",
        sql,
        flags=re.IGNORECASE | re.DOTALL,
    )
    if not match:
        raise AssertionError(f"function public.{name} was not found")
    return re.sub(r"\s+", " ", match.group(1)).strip().lower()


class ThingAuthorityMigrationContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls) -> None:
        cls.sql = normalized_sql()
        cls.raw = MIGRATION.read_text(encoding="utf-8")

    def test_persists_opaque_image_identity_and_clears_legacy_bearers(self) -> None:
        self.assertIn('add column if not exists image_path text', self.sql)
        self.assertIn('set "imageurl" = null', self.sql)
        self.assertRegex(
            self.sql,
            r'set image_path = "imageurl" .*?"imageurl" !~\*? .*?https\?://',
        )
        self.assertNotRegex(
            self.sql,
            r'create_signed_url|signedurl|signed_url',
            "the authority migration must not create or persist signed bearer URLs",
        )

    def test_full_thing_authority_is_creator_scoped(self) -> None:
        for policy in (
            "thing creator authority select",
            "thing creator authority insert",
            "thing creator authority update",
            "thing creator authority delete",
        ):
            self.assertIn(f'create policy "{policy}" on public."thing"', self.sql)

        self.assertIn(
            'create policy "thing creator authority select" on public."thing" for select to authenticated using (public.eyespie_owns_player(created_by))',
            self.sql,
        )
        self.assertIn(
            'create policy "thing creator authority insert" on public."thing" for insert to authenticated with check (public.eyespie_owns_player(created_by))',
            self.sql,
        )
        self.assertIn(
            'drop policy if exists "things are visible to all authenticated users" on public."thing"',
            self.sql,
        )

    def test_legacy_full_row_match_oracle_is_removed(self) -> None:
        self.assertIn(
            'drop function if exists public.match_things(extensions.vector, float, int)',
            self.sql,
        )
        match = function_body(self.raw, "match_thing")
        self.assertIn('returns table ( id uuid, similarity float, matched boolean )', match)
        self.assertNotIn('to_jsonb', match)
        self.assertNotRegex(match, r'\bt\.\*\b')
        self.assertNotRegex(match, r'\bembedding\s+(?:extensions\.)?vector\b')
        self.assertIn("raise exception 'not authorized for thing'", match)

    def test_nearby_and_game_rpc_return_shapes_are_minimal(self) -> None:
        expected_projection = 'returns table ( id uuid, created_at timestamp with time zone, guessed boolean )'
        for function_name in ("thingsnearby", "game_things_safe"):
            body = function_body(self.raw, function_name)
            self.assertIn(expected_projection, body)
            self.assertNotIn('to_jsonb', body)
            self.assertNotRegex(body, r'\bt\.\*\b')

    def test_security_definer_functions_fix_search_path_and_restrict_execute(self) -> None:
        functions = {
            "eyespie_owns_player": "uuid",
            "eyespie_owns_thing": "uuid",
            "eyespie_participates_in_game": "uuid",
            "thingsnearby": "double precision, double precision, double precision",
            "game_things_safe": "uuid",
            "match_thing": "uuid, extensions.vector, float",
        }
        for name, signature in functions.items():
            body = function_body(self.raw, name)
            self.assertIn("security definer", body, f"{name} must be SECURITY DEFINER")
            self.assertIn("set search_path =", body, f"{name} must pin search_path")
            self.assertIn(
                f"revoke all on function public.{name}({signature}) from public, anon",
                self.sql,
                f"{name} must not be executable by public/anon",
            )
            self.assertIn(
                f"grant execute on function public.{name}({signature}) to authenticated",
                self.sql,
                f"{name} must explicitly grant only the authenticated app role",
            )

    def test_storage_policies_are_explicit_and_owner_namespaced(self) -> None:
        for operation in ("read", "insert", "update", "delete"):
            policy = f'eyespie image owners can {operation}'
            self.assertIn(f'create policy "{policy}" on storage.objects', self.sql)

        # Read, insert, delete each check the namespace once; update checks both the
        # existing row and the replacement row.
        self.assertEqual(5, self.sql.count("bucket_id = 'images'"))
        self.assertEqual(5, self.sql.count("p.user_id = auth.uid()"))
        self.assertEqual(5, self.sql.count("p.id::text = split_part(name, '/', 1)"))

    def test_game_thing_mutation_requires_membership_and_ownership(self) -> None:
        self.assertIn('drop policy if exists "user_policy" on public."gamething"', self.sql)
        self.assertIn(
            'with check ( public.eyespie_participates_in_game(game_id) and public.eyespie_owns_thing(thing_id) )',
            self.sql,
        )
        self.assertIn(
            'using ( public.eyespie_participates_in_game(game_id) and public.eyespie_owns_thing(thing_id) )',
            self.sql,
        )


if __name__ == "__main__":
    unittest.main()
