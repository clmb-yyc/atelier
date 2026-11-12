import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/(climbing-operations)/routes')({ component: Routes })

function Routes() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Routes</h1>
      <p className="mt-4 text-lg">
        Manage climbing routes.
      </p>
    </div>
  )
}
