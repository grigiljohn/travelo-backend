# Communities / circles — backend persistence & flow

## Bounded context

- **Service**: `social-service` (same JVM and **`travelo_posts`** PostgreSQL as posts and [`circle_plans`](EVENTS_AND_PLANS_PERSISTENCE.md)).
- **API base path**: `/api/v1/circles/communities` (`CircleCommunityController`).
- **Still in-memory**: `/api/v1/circles/discovery` (`CirclesDiscoveryService`) — replace later with geo + identity graph.

## Data model

### `circle_communities`

| Column | Purpose |
|--------|---------|
| `id` (VARCHAR PK) | Stable public id (`g1`… seeds, `c_…` for user-created) |
| `name`, `description`, `city` | List/detail UI |
| `visibility` | `public` \| `private` |
| `owner_user_id` | Creator / host (from `X-User-Id` or JWT-derived id in future) |
| `member_count` | Denormalized count for cards; incremented on **join** |
| `last_activity_label` | Human string (e.g. `2m ago`); set to `Just now` on join/create |
| `created_at`, `updated_at` | Audit + future sorting |

**Logical links**: `circle_plans.organizer_community_id` should reference `circle_communities.id` when the event is hosted by a community (no hard FK yet so `c_*` / legacy ids stay flexible).

### `circle_community_members`

| Column | Purpose |
|--------|---------|
| `community_id`, `user_id` | Composite PK |
| `role` | `OWNER` reserved for future moderation; seeds use **`MEMBER`** only (legacy parity: owner may not appear as a row) |
| `joined_at` | Ordering |

**Membership semantics** (aligned with previous in-memory behaviour):

- **`member` flag** in API: `exists(circle_community_members)` for `(community_id, user_id)` — owner is **not** automatically a row unless they joined or were added at create.
- **`owner` flag**: `owner_user_id == current user`.
- **Private visibility**: community is visible only if user is **owner** or has a **member** row.

### `circle_community_tags`

Normalized `(community_id, tag)` for search, filters, and onboarding suggestions.

## API surface

| Method | Path | Auth (current `SecurityConfig`) | Behaviour |
|--------|------|----------------------------------|-----------|
| `GET` | `/api/v1/circles/communities?city=` | `permitAll` (optional `X-User-Id`) | Lists communities **visible** to the viewer; optional city loose-match. |
| `GET` | `/api/v1/circles/communities/{id}` | `permitAll` | Detail if visible. |
| `POST` | `/api/v1/circles/communities` | `permitAll` | Creates community + member rows + tags; **201 Created**. |
| `POST` | `/api/v1/circles/communities/{id}/join` | `permitAll` | Idempotent join: insert member if missing, bump `member_count`. |

**DTOs**: `CreateCommunityRequest` / `CommunityResponse` unchanged at the JSON shape level.

## End-to-end flows

### List / discover communities

1. Client sends `GET …/communities?city=Kochi%2C%20Kerala` and optional `X-User-Id`.
2. `CircleCommunityService.list` loads `findAccessibleForUser(user)`:
   - `visibility = public` **OR** user appears in `circle_community_members`.
3. Tags loaded in batch for returned ids; sort: non-members first, then name (same UX as before).
4. City filter: substring match on `city`; empty `city` matches any requested locality (worldwide hubs).

### Create community

1. `POST …/communities` with body (`name`, `description`, `tags`, `visibility`, `city`, `inviteUserIds`).
2. Service generates id `c_` + 12 hex chars.
3. **Transaction**: insert `circle_communities` with `member_count = 1 + invitees`; insert **member** row for owner and each invitee; insert tag rows (deduped, max 48 chars).
4. Returns `GET`-equivalent payload via internal `get`.

### Join

1. `POST …/communities/{id}/join` with `X-User-Id`.
2. Validate visibility; if already a member → return current payload.
3. Insert `circle_community_members`, native `UPDATE` to increment `member_count` and refresh `last_activity_label`.

## Operational notes

- Flyway: `V24__create_circle_communities_tables.sql`, `V25__seed_circle_communities_demo.sql` under `db/migration/post`.
- JPA: `PostJpaConfiguration` scans `com.travelo.circlesservice.persistence` and `com.travelo.circlesservice.repository`.

## Phase 2 (recommended)

1. **Auth**: require JWT for `POST` create/join; derive user id from token instead of trusting `X-User-Id` alone.
2. **Moderation**: `OWNER` / `MODERATOR` roles, `PENDING` join requests when product adds approvals.
3. **Discovery**: persist “nearby travelers” or query `identity-service` + last-known location.
4. **Events**: enforce FK or sync job from `circle_plans.organizer_community_id` → `circle_communities.id`.
5. **Events**: `community.updated` Kafka message for feed/search invalidation.
