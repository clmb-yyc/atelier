import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/_authenticated/(administration)/settings')({ component: Settings })

function Settings() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Settings</h1>
      <p className="mt-4 text-lg">
        Manage settings.
      </p>
    </div>
  )
}
