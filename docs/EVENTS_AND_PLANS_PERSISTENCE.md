# Events / circle plans — persistence architecture

## Scope (this iteration)

- **Persisted**: “Simple” plans from `POST /api/v1/plans` and `GET /api/v1/plans` (mobile Create Event + community event lists).
- **Still in-memory**: `GET /api/v1/plans/feed`, `GET|POST /api/v1/plans/{uuid}` rich model (`RichPlanService`). Unify these onto `circle_plans` (or a `plan_rich_payload` JSONB column) in a later phase to avoid two sources of truth.

## Bounded context

- **Service**: `social-service` (same process as posts; shared **`travelo_posts`** PostgreSQL via `postDataSource`).
- **Rationale**: Events are social graph / feed adjacent; reuse existing HA DB, Flyway discipline, and JWT + `SecurityUtils` already used by `PlansController`.

## Tables

### `circle_plans`

Canonical row for a published event/plan.

| Column | Purpose |
|--------|---------|
| `id` (UUID) | Public identifier returned as `id` in API |
| `host_user_id` | JWT subject / owner |
| `organizer_community_id` | Optional circle hosting the event |
| `title`, `description`, `location_label`, `time_label` | Display + search |
| `external_place_id`, `latitude`, `longitude`, `starts_at` | Geo + calendar (optional until clients send them) |
| `max_people`, `joined_count` | Capacity; `joined_count` denormalized for list cards |
| `badge` | `TRENDING` \| `HAPPENING_NOW` \| `NONE` |
| `hero_image_url`, `host_name`, `host_avatar_url` | Card + detail header |
| `privacy`, `require_approval`, `allow_waitlist` | Policy (Create Event step 2) |
| `status` | `PUBLISHED` (future: `DRAFT`, `CANCELLED`) |
| `created_at`, `updated_at` | Sorting + auditing |

### `circle_plan_participants`

RSVP / membership rows.

| Column | Purpose |
|--------|---------|
| `plan_id` | FK → `circle_plans` |
| `user_id` | Participant identity |
| `display_name`, `avatar_url` | Preview strip on cards |
| `role` | `HOST` \| `MEMBER` \| (future `PENDING`) |
| `joined_at` | Ordering |

**Rules**: On publish, insert **HOST** row. Future `POST .../join` should insert `MEMBER` (or `PENDING` if `require_approval`) and increment `joined_count` in one transaction.

## API (unchanged routes, extended contract)

| Method | Path | Behaviour |
|--------|------|-------------|
| `GET` | `/api/v1/plans?city=` | Lists from **`circle_plans`** + participant avatars (ordered, deduped). |
| `POST` | `/api/v1/plans` | Validates `PlanCreateRequest`, inserts `circle_plans` + host participant, returns `PlanResponse`. |
| `POST` | `/api/v1/plans/create` | Rich create — **still memory**; migrate in phase 2. |

**Request extensions** (`PlanCreateRequest`, backward compatible JSON):

- `organizerCommunityId`, `externalPlaceId`, `latitude`, `longitude`, `startsAtIso`
- `requireApprovalToJoin`, `allowWaitlist`, `privacy`
- `maxPeople` max raised to **500** (aligned with mobile stepper).

**Response extension** (`PlanResponse`):

- `organizerCommunityId` (empty string when unset).

## End-to-end flow

1. **Client** (Flutter): `PlansApiService.createPlan` → `POST /api/v1/plans` with Bearer JWT + optional `X-User-*` headers.
2. **Gateway** (if used): route to `social-service` (same as today’s post-service path prefix your env uses).
3. **`PlansController`**: resolves `hostUserId` from JWT; calls `PlanService.createPlan`.
4. **`PlanService`**: single `@Transactional` — insert `circle_plans`, insert `circle_plan_participants` (HOST), return `PlanResponse`.
5. **Feed / discovery** (future): emit `plan.created` (Kafka) or call feed ranking service after commit; **not** implemented here to keep the change focused.

## Operational notes

- Flyway: `V22__create_circle_plans_tables.sql`, `V23__seed_circle_plans_demo.sql` under `db/migration/post`.
- JPA packages: `PostJpaConfiguration` scans `com.travelo.planservice.persistence` and repositories under `com.travelo.planservice.repository`.
- If `travelo_posts` is empty, run app once so Flyway applies migrations before relying on list/create.

## Phase 2 (recommended)

1. Merge **`RichPlanService`** create/detail/join onto the same tables (or add `payload JSONB` for rich-only fields).
2. **`POST /api/v1/plans/{id}/join`**: transactional participant insert + `joined_count` + optional approval workflow.
3. **Outbox / events**: `plan.created` for search indexing and push notifications.
4. **Read models**: materialized view or CQRS slice for “plans near me” if geo volume grows.
