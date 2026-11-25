## Database schema

Atelier stores gym operations and access records in PostgreSQL. IDs are UUIDs, timestamps use `timestamptz`, and fields marked **optional** can be null. Account IDs refer to accounts in the Auth service.

<img width="966" height="745" alt="atelier-db" src="https://github.com/user-attachments/assets/1e552b88-3958-47ad-9381-6d66af58f50e" />

| Table | Fields |
| --- | --- |
| `organizations` | `id` (PK), `name`, `archived_at` (optional) |
| `gyms` | `id` (PK), `organization_id`, `name`, `archived_at` (optional) |
| `organization_admins` | `organization_id` and `account_id` (composite PK) |
| `gym_admins` | `gym_id` and `account_id` (composite PK) |
| `gym_staff` | `gym_id` and `account_id` (composite PK) |
| `walls` | `id` (PK), `gym_id`, `name`, `discipline` (`BOULDER` or `ROPE`), `archived_at` (optional) |
| `routes` | `id` (PK), `wall_id`, `name` (optional), `grade` (text), `set_at`, `removed_at` (optional) |
| `access_plans` | `id` (PK), `organization_id`, `gym_id` (optional), `name`, `plan_type` (optional on older plans), `valid_until` (optional), `visit_limit` (optional integer), `archived_at` (optional) |
| `member_passes` | `id` (PK), `access_plan_id`, `account_id`, `starts_at`, `expires_at`, `revoked_at` (optional) |
| `check_ins` | `id` (PK), `gym_id`, `member_pass_id`, `checked_in_at` |

- An organization can operate multiple gyms. Admin assignments are scoped to an organization, and staff assignments are scoped to a gym.
- A route inherits its discipline from its wall and stores its own grade. Archiving a wall preserves its routes.
- An access plan with no `gym_id` covers every gym in its organization. Each issued pass has its own required expiry.
- Archiving a plan closes it to new issuance. Existing passes still reference the plan for scope and any visit limit.
- Each check-in records one successful entry. The pass identifies the member. Count the pass's check-ins to calculate visits used.
- The auth database has information about users for the entire platform.

## API endpoints

Send an access token from Auth as `Authorization: Bearer <token>` on every request. Request bodies are JSON. Send resource IDs as UUIDs and timestamps as ISO 8601 strings, for example `2027-01-01T00:00:00Z`.

### Organizations and gyms

| Method and path | Who can call it | Request body | Success response |
| --- | --- | --- | --- |
| `POST /api/organizations` | Auth `STAFF` | `name` | `201 Created` with `id` and `name`. The caller becomes the first organization admin. |
| `POST /api/organizations/{organizationId}/gyms` | Organization admin | `name` | `201 Created` with `id`, `organizationId`, and `name`. |
| `PUT /api/organizations/{organizationId}/admins/{accountId}` | Organization admin | None | `201 Created` for a new assignment or `204 No Content` if it already exists. Both have an empty body. |
| `PUT /api/gyms/{gymId}/admins/{accountId}` | Admin of the gym's organization | None | `201 Created` for a new assignment or `204 No Content` if it already exists. Both have an empty body. |
| `PUT /api/gyms/{gymId}/staff/{accountId}` | Gym admin or admin of its organization | None | `201 Created` for a new assignment or `204 No Content` if it already exists. Both have an empty body. |
| `POST /api/gyms/{gymId}/walls` | Gym admin or admin of its organization | `name`, `discipline` (`BOULDER` or `ROPE`) | `201 Created` with `id`, `gymId`, `name`, and `discipline`. |
| `POST /api/gyms/{gymId}/walls/{wallId}/routes` | Gym admin or admin of its organization | `grade`, optional `name` | `201 Created` with `id`, `wallId`, `name`, `grade`, and `setAt`. |
| `GET /api/gyms/{gymId}/routes` | Staff assigned to the gym, a gym admin, or an admin of its organization | None | `200 OK` with an array of active routes, newest first. Each item includes `id`, `wallId`, `wallName`, `discipline`, `name`, `grade`, and `setAt`. |

### Access plans and member passes

| Method and path | Who can call it | Request body | Success response |
| --- | --- | --- | --- |
| `POST /api/organizations/{organizationId}/access-plans` | Organization admin | `name`, `planType` (`MEMBERSHIP`, `DAY_PASS`, or `VISIT_PASS`), optional `gymId`, `validUntil`, and `visitLimit` | `201 Created` with an access plan. Omit `gymId` for an organization-wide plan or `visitLimit` for unlimited visits. |
| `GET /api/organizations/{organizationId}/access-plans` | Organization admin | None | `200 OK` with an array of access plans, including archived plans, ordered by name. |
| `DELETE /api/organizations/{organizationId}/access-plans/{planId}` | Organization admin | None | `204 No Content`. Archives the plan, closing it to new issuance. |
| `POST /api/organizations/{organizationId}/access-plans/{planId}/passes` | Organization admin | `accountId`, `expiresAt`, optional `startsAt` (defaults to now) | `201 Created` with a member pass. |
| `POST /api/gyms/{gymId}/access-plans/{planId}/passes` | Gym staff, gym admin, or admin of its organization | `accountId`, `expiresAt`, optional `startsAt` (defaults to now) | `201 Created` with a member pass. The plan must cover that gym. |
| `DELETE /api/organizations/{organizationId}/member-passes/{passId}` | Organization admin | None | `204 No Content`. Revokes the pass. |
| `GET /api/me/member-passes` | Signed-in account | None | `200 OK` with an array of the caller's member passes, latest expiry first. |

- An access plan returns `id`, `organizationId`, `gymId`, `name`, `planType`, `validUntil`, `visitLimit`, and `archivedAt`. Older plans have no type and cannot issue new passes.
- A member pass returns `id`, `accessPlanId`, `accountId`, `startsAt`, `expiresAt`, and `revokedAt`.
- `validUntil` is the cutoff for issuing passes under a plan. An issued pass uses its own `expiresAt`.
- Issuance checks overlapping passes for the same account and organization. A conflicting membership returns `409 EXISTING_MEMBERSHIP`. Unused day or visit passes return `409 EXISTING_PASSES_REVIEW` when starting a membership. The response includes `conflictingPassIds` for the front desk. Review leaves the existing passes intact.

### Check-ins

| Method and path | Who can call it | Request body | Success response |
| --- | --- | --- | --- |
| `POST /api/gyms/{gymId}/check-ins` | Staff assigned to the gym, a gym admin, or an admin of its organization | `memberPassId` | `201 Created` with `id`, `gymId`, `memberPassId`, and `checkedInAt`. Each success records one visit for the member who holds the pass. |

### Error responses

| Status | When it applies |
| --- | --- |
| `400 Bad Request` | A request field is missing or invalid, such as a missing `expiresAt` when issuing a pass. |
| `401 Unauthorized` | The access token is missing or invalid. |
| `403 Forbidden` | The caller lacks the required gym or organization permission, or the pass is restricted to another gym in the same organization. |
| `404 Not Found` | A resource is missing or unavailable in the requested scope, such as a wall in another gym or a pass issued by another organization. |
| `409 Conflict` | Pass issuance is blocked or needs review, or a check-in uses a revoked, expired, not-yet-active, or exhausted pass. |
