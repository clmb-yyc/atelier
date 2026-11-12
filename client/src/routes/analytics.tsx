import { createFileRoute } from '@tanstack/react-router'

export const Route = createFileRoute('/analytics')({ component: Analytics })

function Analytics() {
  return (
    <div className="p-8">
      <h1 className="text-4xl font-bold">Analytics</h1>
      <p className="mt-4 text-lg">
        This is the analytics page.
      </p>
    </div>
  )
}
