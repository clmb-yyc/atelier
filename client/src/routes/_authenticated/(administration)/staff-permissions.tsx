import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/(administration)/staff-permissions')({ component: StaffPermissions })

function StaffPermissions() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Staff &amp; Permissions</h1>
      <p className="mt-4 text-lg">
        Manage staff and permissions.
      </p>
    </div>
  )
}
