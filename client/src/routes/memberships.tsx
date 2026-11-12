import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/memberships')({ component: Memberships })

function Memberships() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Memberships</h1>
      <p className="mt-4 text-lg">
        Manage memberships.
      </p>
    </div>
  )
}
