## Database schema

Atelier stores gym operations and access records in PostgreSQL. IDs are UUIDs, timestamps use `timestamptz`, and fields marked **optional** can be null. Account IDs refer to accounts in the Auth service.

| Table | Fields |
| --- | --- |
| `organizations` | `id` (PK), `name`, `archived_at` (optional) |
| `gyms` | `id` (PK), `organization_id`, `name`, `archived_at` (optional) |
| `organization_admins` | `organization_id` and `account_id` (composite PK) |
| `gym_staff` | `gym_id` and `account_id` (composite PK) |
| `walls` | `id` (PK), `gym_id`, `name`, `discipline` (`BOULDER` or `ROPE`), `archived_at` (optional) |
| `routes` | `id` (PK), `wall_id`, `name` (optional), `grade` (text), `set_at`, `removed_at` (optional) |
| `access_plans` | `id` (PK), `organization_id`, `gym_id` (optional), `name`, `valid_until` (optional), `visit_limit` (optional integer), `archived_at` (optional) |
| `member_passes` | `id` (PK), `access_plan_id`, `account_id`, `starts_at`, `expires_at`, `revoked_at` (optional) |
| `check_ins` | `id` (PK), `gym_id`, `member_pass_id`, `checked_in_at` |

- An organization can operate multiple gyms. Admin assignments are scoped to an organization, and staff assignments are scoped to a gym.
- A route inherits its discipline from its wall and stores its own grade. Archiving a wall preserves its routes.
- An access plan with no `gym_id` covers every gym in its organization. Each issued pass has its own required expiry.
- Archiving a plan closes it to new issuance. Existing passes still reference the plan for scope and any visit limit.
- Each check-in records one successful entry. The pass identifies the member. Count the pass's check-ins to calculate visits used.
- The auth database has information about users for the entire platform.
