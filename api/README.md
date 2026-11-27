## Database schema

Atelier stores gym operations and access records in PostgreSQL. IDs are UUIDs, timestamps use `timestamptz`, and user IDs refer to users in the Auth service. A null `archived_at` means the record has yet to be archived.

<img width="1246" height="881" alt="atelier-db" src="https://github.com/user-attachments/assets/712b862a-3ec8-4ffc-82a7-021112c606ca" />


| Table | Non-nullable fields | Nullable fields |
| --- | --- | --- |
| `organizations` | `id` (PK), `name`, `created_at` | `archived_at`, `description`, `slug`, `logo_url`, `website_url`, `contact_email` |
| `gyms` | `id` (PK), `organization_id`, `name`, `address`, `city`, `postal_code`, `country`, `latitude`, `longitude`, `timezone`, `created_at` | `grading_system_id`, `archived_at`, `description`, `phone_number`, `email`, `capacity` |
| `organization_admins` | `organization_id` and `user_id` (composite PK), `assigned_at`, `created_at` | `assigned_by` |
| `gym_staff` | `gym_id` and `user_id` (composite PK), `assigned_at`, `created_at` | `assigned_by`, `job_title`, `employment_type` |
| `walls` | `id` (PK), `gym_id`, `name`, `discipline` (`BOULDER` or `ROPE`), `created_at` | `archived_at`, `description`, `height_m`, `angle_degrees`, `location` |
| `routes` | `id` (PK), `wall_id`, `grade_id`, `colour`, `setter_id`, `hold_colour`, `set_at`, `created_at` | `name`, `removed_at`, `description`, `style`, `image_url`, `removal_reason` |
| `grading_systems` | `id` (PK), `name`, `visibility` (`PUBLIC` or `PRIVATE`), `created_at` | `gym_id`, `organization_id`, `description` |
| `grades` | `id` (PK), `grading_system_id`, `label`, `sort_order`, `created_at` | `description` |
| `passes` | `id` (PK), `organization_id`, `name`, `price`, `currency`, `is_renewable`, `created_at` | `gym_id`, `pass_type` (older passes), `valid_until`, `visit_limit` (integer), `archived_at`, `description`, `duration_days` |
| `user_passes` | `id` (PK), `pass_id`, `user_id`, `starts_at`, `expires_at`, `purchased_at`, `issued_by`, `created_at` | `revoked_at`, `revoked_by`, `revocation_reason` |
| `check_ins` | `id` (PK), `gym_id`, `user_pass_id`, `checked_in_at`, `checked_in_by`, `created_at` | `checked_out_at`, `voided_at` |

Atelier sets `created_at` automatically for new records.

- An organization can operate multiple gyms. Organization admins manage all gyms in their organization. Staff assignments are scoped to a gym.
- A route inherits its discipline from its wall and references a grade from a system available at that wall's gym. Archiving a wall preserves its routes.
- A gym can select one grading system in its settings. Public systems are available at every gym and have null `gym_id` and `organization_id`. Private systems have an `organization_id`. A null `gym_id` makes the system available throughout that organization. A set `gym_id` limits it to that gym. Grades inherit their system's scope and have a unique label within that system.
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
| `POST /api/gyms/{gymId}/walls/{wallId}/routes` | Admin of the gym's organization | `gradeId` (grade UUID), `colour`, `setterId` (user UUID), `holdColour`, optional `name` | `201 Created` with `id`, `wallId`, `name`, `gradeId`, `grade` (label), and `setAt`. A grade unavailable at the gym returns `404 Not Found`. |
| `GET /api/gyms/{gymId}/routes` | Staff assigned to the gym or an admin of its organization | None | `200 OK` with an array of active routes, newest first. Each item includes `id`, `wallId`, `wallName`, `discipline`, `name`, `gradeId`, `grade` (label), and `setAt`. |

### Grading settings

| Method and path | Who can call it | Request body | Success response |
| --- | --- | --- | --- |
| `GET /api/gyms/{gymId}/grading-systems` | Staff assigned to the gym | None | `200 OK` with public, organization-wide, and gym-specific systems available to the gym, ordered by name. Each system includes its grades, ordered by `sortOrder` and then `label`. |
| `GET /api/gyms/{gymId}/grading-system` | Staff assigned to the gym | None | `200 OK` with the gym's selected `gradingSystemId`, which is null until staff select a system. |
| `PUT /api/gyms/{gymId}/grading-system` | Staff assigned to the gym | `gradingSystemId` | `200 OK` with the selected `gradingSystemId`. A system unavailable at the gym returns `404 Not Found`. |
| `POST /api/gyms/{gymId}/grading-systems` | Staff assigned to the gym | `name`, optional `description` | `201 Created` with a private system for that gym and an empty `grades` array. |
| `POST /api/gyms/{gymId}/grading-systems/shared` | Staff assigned to the gym | `name`, optional `description` | `201 Created` with a private system shared across the gym's organization and an empty `grades` array. |
| `POST /api/gyms/{gymId}/grading-systems/{systemId}/grades` | Staff assigned to the gym | `label`, `sortOrder` (integer, zero or greater), optional `description` | `201 Created` with the grade. Staff can add grades to their gym's private systems and their organization's shared systems. A duplicate label in that system returns `409 Conflict`. |

- A grading system returns `id`, `gymId`, `organizationId`, `name`, `description`, `visibility`, `createdAt`, and `grades`.
- A grade returns `id`, `gradingSystemId`, `label`, `description`, `sortOrder`, and `createdAt`.

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
