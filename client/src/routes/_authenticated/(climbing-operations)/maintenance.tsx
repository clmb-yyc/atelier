import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/(climbing-operations)/maintenance')({ component: Maintenance })

function Maintenance() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Maintenance</h1>
      <p className="mt-4 text-lg">
        Manage maintenance tasks.
      </p>
    </div>
  )
}
