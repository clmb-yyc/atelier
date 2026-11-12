import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/route-setting')({ component: RouteSetting })

function RouteSetting() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Route Setting</h1>
      <p className="mt-4 text-lg">
        Manage route setting.
      </p>
    </div>
  )
}
