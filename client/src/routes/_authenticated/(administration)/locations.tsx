import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/(administration)/locations')({ component: Locations })

function Locations() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Locations</h1>
      <p className="mt-4 text-lg">
        Manage locations.
      </p>
    </div>
  )
}
