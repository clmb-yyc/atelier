import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/walls-zones')({ component: WallsZones })

function WallsZones() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Walls &amp; Zones</h1>
      <p className="mt-4 text-lg">
        Manage climbing walls and zones.
      </p>
    </div>
  )
}
