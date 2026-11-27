## Database schema

Atelier stores gym operations and access records in PostgreSQL. IDs are UUIDs, timestamps use `timestamptz`, and fields marked **optional** can be null. User IDs refer to users in the Auth service.

<img width="966" height="745" alt="atelier-db" src="https://github.com/user-attachments/assets/1e552b88-3958-47ad-9381-6d66af58f50e" />

| Table | Fields | Additional nullable fields |
| --- | --- | --- |
| `organizations` | `id` (PK), `name`, `archived_at` (optional) | `description`, `slug`, `logo_url`, `website_url`, `contact_email`, `created_at` |
| `gyms` | `id` (PK), `organization_id`, `name`, `address`, `city`, `postal_code`, `country`, `latitude`, `longitude`, `timezone`, `archived_at` (optional) | `description`, `phone_number`, `email`, `capacity`, `created_at` |
| `organization_admins` | `organization_id` and `user_id` (composite PK), `assigned_at` | `assigned_by` |
| `gym_staff` | `gym_id` and `user_id` (composite PK), `assigned_at` | `assigned_by`, `job_title`, `employment_type` |
| `walls` | `id` (PK), `gym_id`, `name`, `discipline` (`BOULDER` or `ROPE`), `created_at`, `archived_at` (optional) | `description`, `height_m`, `angle_degrees`, `location` |
| `routes` | `id` (PK), `wall_id`, `name` (optional), `grade` (text), `colour`, `setter_id`, `hold_colour`, `difficulty_rating` (integer), `set_at`, `removed_at` (optional) | `description`, `style`, `image_url`, `removal_reason` |
| `passes` | `id` (PK), `organization_id`, `gym_id` (optional), `name`, `pass_type` (optional on older passes), `price`, `currency`, `is_renewable`, `created_at`, `valid_until` (optional), `visit_limit` (optional integer), `archived_at` (optional) | `description`, `duration_days` |
| `user_passes` | `id` (PK), `pass_id`, `user_id`, `starts_at`, `expires_at`, `purchased_at`, `issued_by`, `revoked_at` (optional) | `revoked_by`, `revocation_reason` |
| `check_ins` | `id` (PK), `gym_id`, `user_pass_id`, `checked_in_at`, `checked_in_by` | `checked_out_at`, `voided_at` |

Atelier validates the PostgreSQL schema on startup. Create these columns before starting the API. For an existing database, backfill newly required fields before applying their `NOT NULL` constraints. The endpoint tables below list which fields the API currently accepts and returns.

- An organization can operate multiple gyms. Organization admins manage all gyms in their organization. Staff assignments are scoped to a gym.
- A route inherits its discipline from its wall and stores its own grade. Archiving a wall preserves its routes.
- A pass with no `gym_id` covers every gym in its organization. Each user pass has its own required expiry.
- Archiving a pass closes it to new issuance. Existing user passes still reference it for scope and any visit limit.
- Each check-in records one successful entry. The pass identifies the member. Count the pass's check-ins to calculate visits used.
- The auth database has information about users for the entire platform.

## API endpoints

Send an access token from Auth as `Authorization: Bearer <token>` on every request. Request bodies are JSON. Send resource IDs as UUIDs and timestamps as ISO 8601 strings, for example `2027-01-01T00:00:00Z`.

### Organizations and gyms

| Method and path | Who can call it | Request body | Success response |
| --- | --- | --- | --- |
| `POST /api/organizations` | Auth `STAFF` | `name` | `201 Created` with `id` and `name`. The caller becomes the first organization admin. |
| `POST /api/organizations/{organizationId}/gyms` | Organization admin | Required `name`, `address`, `city`, `postalCode`, `country`, `latitude`, `longitude`, and `timezone` | `201 Created` with `id`, `organizationId`, and `name`. Missing required fields return `400 Bad Request`. |
| `PUT /api/organizations/{organizationId}/admins/{userId}` | Organization admin | None | `201 Created` for a new assignment, with `assigned_at` recorded by the server, or `204 No Content` if it already exists. Both have an empty body. |
| `PUT /api/gyms/{gymId}/staff/{userId}` | Admin of the gym's organization | None | `201 Created` for a new assignment, with `assigned_at` recorded by the server, or `204 No Content` if it already exists. Both have an empty body. |
| `POST /api/gyms/{gymId}/walls` | Admin of the gym's organization | `name`, `discipline` (`BOULDER` or `ROPE`) | `201 Created` with `id`, `gymId`, `name`, and `discipline`. The server records `created_at`. |
| `POST /api/gyms/{gymId}/walls/{wallId}/routes` | Admin of the gym's organization | `grade`, `colour`, `setterId` (user UUID), `holdColour`, `difficultyRating` (integer), optional `name` | `201 Created` with `id`, `wallId`, `name`, `grade`, and `setAt`. |
| `GET /api/gyms/{gymId}/routes` | Staff assigned to the gym or an admin of its organization | None | `200 OK` with an array of active routes, newest first. Each item includes `id`, `wallId`, `wallName`, `discipline`, `name`, `grade`, and `setAt`. |

### Passes and user passes

| Method and path | Who can call it | Request body | Success response |
| --- | --- | --- | --- |
| `POST /api/organizations/{organizationId}/passes` | Organization admin | `name`, `passType` (`MEMBERSHIP`, `DAY_PASS`, or `VISIT_PASS`), `price` (zero or greater), `currency` (three-letter code), `isRenewable` (boolean), optional `gymId`, `validUntil`, and `visitLimit` | `201 Created` with a pass. The server records `created_at`. Omit `gymId` for organization-wide access or `visitLimit` for unlimited visits. |
| `GET /api/organizations/{organizationId}/passes` | Organization admin | None | `200 OK` with an array of passes, including archived passes, ordered by name. |
| `DELETE /api/organizations/{organizationId}/passes/{passId}` | Organization admin | None | `204 No Content`. Archives the pass, closing it to new issuance. |
| `POST /api/organizations/{organizationId}/passes/{passId}/user-passes` | Organization admin | `userId`, `expiresAt`, optional `startsAt` (defaults to now) | `201 Created` with a user pass. |
| `POST /api/gyms/{gymId}/passes/{passId}/user-passes` | Staff assigned to the gym or an admin of its organization | `userId`, `expiresAt`, optional `startsAt` (defaults to now) | `201 Created` with a user pass. The pass must cover that gym. |
| `DELETE /api/organizations/{organizationId}/user-passes/{userPassId}` | Organization admin | None | `204 No Content`. Revokes the user pass. |
| `GET /api/me/user-passes` | Signed-in account | None | `200 OK` with an array of the caller's user passes, latest expiry first. |

- A pass returns `id`, `organizationId`, `gymId`, `name`, `passType`, `validUntil`, `visitLimit`, and `archivedAt`. Older passes without a type cannot be issued to users.
- A user pass returns `id`, `passId`, `userId`, `startsAt`, `expiresAt`, and `revokedAt`.
- When issuing a user pass, the server sets `purchased_at` to the issuance time and `issued_by` to the authenticated caller's user ID.
- `validUntil` is the cutoff for issuing a pass to users. A user pass uses its own `expiresAt`.
- Issuance checks overlapping user passes for the same account and organization. A conflicting membership returns `409 EXISTING_MEMBERSHIP`. Unused day or visit passes return `409 EXISTING_PASSES_REVIEW` when starting a membership. The response includes `conflictingUserPassIds` for the front desk. Review leaves the existing user passes intact.

### Check-ins

| Method and path | Who can call it | Request body | Success response |
| --- | --- | --- | --- |
| `POST /api/gyms/{gymId}/check-ins` | Staff assigned to the gym or an admin of its organization | `userPassId` | `201 Created` with `id`, `gymId`, `userPassId`, and `checkedInAt`. Each success records one visit for the pass holder and stores the caller's user ID in `checked_in_by`. |

### Error responses

| Status | When it applies |
| --- | --- |
| `400 Bad Request` | A request field is missing or invalid, such as a missing `expiresAt` when issuing a pass. |
| `401 Unauthorized` | The access token is missing or invalid. |
| `403 Forbidden` | The caller lacks the required gym or organization permission, or the pass is restricted to another gym in the same organization. |
| `404 Not Found` | A resource is missing or unavailable in the requested scope, such as a wall in another gym or a pass issued by another organization. |
| `409 Conflict` | Pass issuance is blocked or needs review, or a check-in uses a revoked, expired, not-yet-active, or exhausted pass. |
